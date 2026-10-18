
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.Callable;

public class LoadWorker implements Callable<Void> {
    private final String host;
    private final int port;
    private final String filename;
    private final long offset;
    private final long length;
    private final Path destination;
    private final TransferMode mode;

    public LoadWorker(
            String host, int port, String filename,
            long offset, long length,
            Path destination, TransferMode mode) {
        this.host = host;
        this.port = port;
        this.filename = filename;
        this.offset = offset;
        this.length = length;
        this.destination = destination;
        this.mode = mode;
    }

    @Override
    public Void call() throws Exception {
        try (Socket socket = new Socket(host, port)) {
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();

            Protocol.sendLine(out,
                    "GET " + filename + " " + offset + " " + length
                    + " " + mode.name().toLowerCase());

            String response = Protocol.readLine(in);

            if (response == null || !response.startsWith("OK ")) {
                throw new IOException("Server response: " + response);
            }

            long announcedLength =
                    Long.parseLong(response.substring(3).trim());

            if (announcedLength != length) {
                throw new IOException("Unexpected range length");
            }

            if (mode == TransferMode.TRADITIONAL) {
                writeTraditional(in);
            } else {
                writeNio(in);
            }
        }

        System.out.println("Downloaded offset=" + offset
                + ", length=" + length + ", mode=" + mode);

        return null;
    }

    private void writeTraditional(InputStream in) throws IOException {
        try (RandomAccessFile file =
                    new RandomAccessFile(destination.toFile(), "rw")) {
            file.seek(offset);

            byte[] buffer = new byte[8192];
            long remaining = length;

            while (remaining > 0) {
                int wanted = (int) Math.min(buffer.length, remaining);
                int count = in.read(buffer, 0, wanted);

                if (count == -1) {
                    throw new IOException("Connection ended early");
                }

                file.write(buffer, 0, count);
                remaining -= count;
            }
        }
    }

    private void writeNio(InputStream in) throws IOException {
        try (FileChannel channel = FileChannel.open(
                destination,
                StandardOpenOption.WRITE)) {

            byte[] bytes = new byte[8192];
            long remaining = length;
            long position = offset;

            while (remaining > 0) {
                int wanted = (int) Math.min(bytes.length, remaining);
                int count = in.read(bytes, 0, wanted);

                if (count == -1) {
                    throw new IOException("Connection ended early");
                }

                ByteBuffer buffer = ByteBuffer.wrap(bytes, 0, count);

                while (buffer.hasRemaining()) {
                    int written = channel.write(buffer, position);
                    position += written;
                }

                remaining -= count;
            }
        }
    }
}
