
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    private static final int PORT = 5000;
    private static final File ROOT = new File("server_files"); //ไฟล์ที่ให้โหลดจากในนี้

    public static void main(String[] args) throws IOException {

        if (!ROOT.exists() && !ROOT.mkdirs()) {
            throw new IOException("Cannot create server_files directory");
        }

        // สร้าง Virtual Thread แยกสำหรับแต่ละงานของ Client
        ExecutorService pool
                = Executors.newVirtualThreadPerTaskExecutor();

        try (ServerSocketChannel server = ServerSocketChannel.open()) {

            server.bind(new InetSocketAddress(PORT));

            System.out.println("Server listening on port " + PORT);
            System.out.println("File directory: "
                    + ROOT.getAbsolutePath());
            showFiles();

            while (true) {
                SocketChannel client = server.accept();

                // ส่งงาน Client แต่ละตัวไปทำใน Virtual Thread
                pool.submit(() -> handleClient(client));
            }

        } finally {
            pool.shutdown();
        }
    }

    private static void showFiles() {
        File[] files = ROOT.listFiles();

        System.out.println("\n===== Available Files =====");

        if (files == null) {
            System.out.println("Cannot read server_files directory.");
            return;
        }

        boolean found = false;

        for (File file : files) {
            if (file.isFile()) {
                System.out.println(
                        file.getName() + " (" + file.length() + " bytes)"
                );
                found = true;
            }
        }

        if (!found) {
            System.out.println("No files available.");
        }

        System.out.println("===========================\n");
    }

    private static void handleClient(SocketChannel socket) {

        try (SocketChannel client = socket) {

            InputStream in = Channels.newInputStream(client);
            OutputStream out = Channels.newOutputStream(client);

            String command = Protocol.readLine(in);

            if (command == null || command.trim().isEmpty()) {
                return;
            }

            String[] parts = command.trim().split("\\s+");

            switch (parts[0].toUpperCase()) {

                case "LIST":
                    sendList(out);
                    break;

                case "INFO":
                    if (parts.length != 2) {
                        Protocol.sendLine(out,
                                "ERROR BAD_REQUEST Usage: INFO filename");
                    } else {
                        sendInfo(out, parts[1]);
                    }
                    break;

                case "GET":
                    if (parts.length != 4 && parts.length != 5) {
                        Protocol.sendLine(out,
                                "ERROR BAD_REQUEST Usage: GET filename offset length [mode]");
                        break;
                    }

                    try {
                        String name = parts[1];
                        long offset = Long.parseLong(parts[2]);
                        long length = Long.parseLong(parts[3]);

                        String mode = parts.length == 5
                                ? parts[4]
                                : "traditional";

                        sendRange(
                                client, out, name, offset, length, mode);

                    } catch (NumberFormatException e) {
                        Protocol.sendLine(out,
                                "ERROR BAD_REQUEST Invalid offset or length");
                    }
                    break;

                default:
                    Protocol.sendLine(out,
                            "ERROR BAD_REQUEST Unknown command");
            }

        } catch (Exception e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }

    private static void sendList(OutputStream out)
            throws IOException {

        File[] files = ROOT.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    Protocol.sendLine(out,
                            "FILE " + file.getName()
                            + " " + file.length());
                }
            }
        }

        Protocol.sendLine(out, "END");
    }

    private static void sendInfo(
            OutputStream out, String name) throws IOException {

        File file = safeFile(name);

        if (file == null || !file.isFile()) {
            Protocol.sendLine(out,
                    "ERROR NOT_FOUND File not found");
            return;
        }

        Protocol.sendLine(out, "SIZE " + file.length());
    }

    private static void sendRange(
            SocketChannel client,
            OutputStream out,
            String name,
            long offset,
            long length,
            String mode) throws IOException {

        File file = safeFile(name);

        if (file == null || !file.isFile()) {
            Protocol.sendLine(out,
                    "ERROR NOT_FOUND File not found");
            return;
        }

        long size = file.length();

        if (offset < 0 || length < 0
                || offset > size || length > size - offset) {
            Protocol.sendLine(out,
                    "ERROR BAD_RANGE Invalid byte range");
            return;
        }

        if (!mode.equalsIgnoreCase("traditional")
                && !mode.equalsIgnoreCase("nio")) {
            Protocol.sendLine(out,
                    "ERROR BAD_MODE Unknown transfer mode");
            return;
        }

        Protocol.sendLine(out, "OK " + length);

        if (mode.equalsIgnoreCase("nio")) {
            sendNativeTransfer(client, file, offset, length);
        } else {
            sendTraditional(out, file, offset, length);
        }
    }

    // Traditional I/O: InputStream.read() and OutputStream.write()
    private static void sendTraditional(
            OutputStream out,
            File file,
            long offset,
            long length) throws IOException {

        try (InputStream fileIn = new FileInputStream(file)) {

            long skipped = 0;

            while (skipped < offset) {
                long count = fileIn.skip(offset - skipped);

                if (count <= 0) {
                    if (fileIn.read() == -1) {
                        throw new IOException(
                                "Cannot reach file offset");
                    }
                    count = 1;
                }

                skipped += count;
            }

            byte[] buffer = new byte[8192];
            long remaining = length;

            while (remaining > 0) {
                int wanted = (int) Math.min(
                        buffer.length, remaining);

                int count = fileIn.read(buffer, 0, wanted);

                if (count == -1) {
                    throw new IOException(
                            "Unexpected end of file");
                }

                out.write(buffer, 0, count);
                remaining -= count;
            }

            out.flush();
        }
    }

    // NIO Native Transfer: FileChannel.transferTo()
    private static void sendNativeTransfer(
            SocketChannel client,
            File file,
            long offset,
            long length) throws IOException {

        try (FileChannel source = FileChannel.open(
                file.toPath(), StandardOpenOption.READ)) {

            long position = offset;
            long remaining = length;

            while (remaining > 0) {

                long sent = source.transferTo(
                        position, remaining, client);

                if (sent > 0) {
                    position += sent;
                    remaining -= sent;
                } else {
                    // Fallback if transferTo makes no progress
                    ByteBuffer buffer = ByteBuffer.allocate(
                            (int) Math.min(8192, remaining));

                    int count = source.read(buffer, position);

                    if (count <= 0) {
                        throw new IOException(
                                "Transfer made no progress");
                    }

                    buffer.flip();

                    while (buffer.hasRemaining()) {
                        int written = client.write(buffer);

                        if (written < 0) {
                            throw new IOException(
                                    "Connection closed during transfer");
                        }
                    }

                    position += count;
                    remaining -= count;
                }
            }
        }
    }

    private static File safeFile(String name)
            throws IOException {

        if (name == null || name.isEmpty()) {
            return null;
        }

        File root = ROOT.getCanonicalFile();
        File file = new File(root, name).getCanonicalFile();

        String rootPath = root.getPath()
                + File.separator;

        if (!file.getPath().startsWith(rootPath)) {
            return null;
        }

        return file;
    }
}
