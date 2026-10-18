
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Client {

    private static final String HOST = "localhost";
    private static final int PORT = 5000;

    public static void main(String[] args) throws Exception {

        // ไม่มี arguments ให้เข้าโหมดรับคำสั่ง
        if (args.length == 0) {
            runCommandMode();
            return;
        }

        // คงรูปแบบการเรียกดาวน์โหลดแบบเดิม
        if (args.length < 1 || args.length > 4) {
            System.out.println(
                    "Usage: java Client <filename> "
                    + "[traditional|nio] [workers] [destination]");
            return;
        }

        downloadFile(args);
    }

    // ==========================================
    // Download file
    // ==========================================
    private static void downloadFile(String[] args) throws Exception {

        if (args.length < 1 || args.length > 4) {
            throw new IllegalArgumentException(
                    "Usage: get <filename> "
                    + "[traditional|nio] [workers] [destination]");
        }

        String filename = args[0];

        TransferMode mode = args.length >= 2
                ? TransferMode.fromString(args[1])
                : TransferMode.TRADITIONAL;

        int workers = args.length >= 3
                ? Integer.parseInt(args[2])
                : 10;

        if (workers < 1) {
            throw new IllegalArgumentException(
                    "Workers อย่างน้อยต้องเป็น 1");
        }

        // หากระบุ destination ให้ถือว่าเป็นตำแหน่งไฟล์เต็ม
        // หากไม่ระบุ ให้ใช้ downloads/<filename>
        Path destination = args.length == 4
                ? Paths.get(args[3])
                : Paths.get("downloads").resolve(filename);

        // แสดงรายชื่อไฟล์ที่ Server ให้ดาวน์โหลดได้
        listFiles();

        // ขอขนาดไฟล์จาก Server
        long size = getFileSize(filename);

        // สร้างโฟลเดอร์ปลายทาง ถ้ายังไม่มี
        Path parent = destination.toAbsolutePath().getParent();

        if (parent != null) {
            Files.createDirectories(parent);
        }

        System.out.println(
                "Save location: " + destination.toAbsolutePath());

        // สร้างไฟล์ปลายทางและกำหนดขนาดก่อนให้ Workers เขียน
        try (java.io.RandomAccessFile file = new java.io.RandomAccessFile(destination.toFile(), "rw")) {
            file.setLength(size);
        }

        ExecutorService pool
                = Executors.newFixedThreadPool(workers);

        List<Future<Void>> futures = new ArrayList<>();

        long startTime = System.nanoTime();

        try {

            // แบ่งไฟล์เป็นช่วงให้แต่ละ Worker
            for (int i = 0; i < workers; i++) {

                long start = size * i / workers;
                long end = size * (i + 1) / workers;
                long length = end - start;

                LoadWorker worker = new LoadWorker(
                        HOST,
                        PORT,
                        filename,
                        start,
                        length,
                        destination,
                        mode
                );

                futures.add(pool.submit(worker));
            }

            // รอให้ Workers ทุกตัวทำงานเสร็จ
            for (Future<Void> future : futures) {
                future.get();
            }

        } catch (ExecutionException e) {

            for (Future<Void> future : futures) {
                future.cancel(true);
            }

            throw new RuntimeException(
                    "Download failed", e.getCause());

        } finally {
            pool.shutdown();
        }

        // คำนวณเวลาที่ใช้ดาวน์โหลด
        long elapsedNanos = System.nanoTime() - startTime;

        double elapsedSeconds
                = elapsedNanos / 1_000_000_000.0;

        long downloadedSize = Files.size(destination);

        boolean sizePassed = downloadedSize == size;

        System.out.println(
                "\n======================RESULT========================");

        System.out.println("Download finished");
        System.out.println("File: " + destination.toAbsolutePath());
        System.out.println("Expected size: " + size + " bytes");
        System.out.println("Actual size:   " + downloadedSize + " bytes");

        System.out.printf(
                "Time: %.3f seconds%n", elapsedSeconds);

        System.out.println(
                "Size verification: "
                + (sizePassed ? "PASS" : "FAIL"));

        // คำนวณ Throughput หน่วย MB/s
        double mb = downloadedSize / 1_000_000.0;

        double throughput = elapsedSeconds > 0
                ? mb / elapsedSeconds
                : 0;

        // ตรวจสอบ SHA-256 ของไฟล์ต้นฉบับและไฟล์ดาวน์โหลด
        Path original = Paths.get("server_files", filename);

        String originalHash = sha256(original);
        String downloadedHash = sha256(destination);

        System.out.println();
        System.out.println("Original SHA-256:   " + originalHash);
        System.out.println("Downloaded SHA-256: " + downloadedHash);

        boolean sha256Passed
                = originalHash.equals(downloadedHash);

        System.out.println(
                "SHA-256 verification: "
                + (sha256Passed ? "PASS" : "FAIL"));

        if (!sizePassed) {
            throw new IOException(
                    "File size verification failed");
        }

        if (!sha256Passed) {
            throw new IOException(
                    "SHA-256 verification failed");
        }

        System.out.printf(
                "Throughput: %.2f MB/s%n", throughput);

        // บันทึกผลการทดลองลง CSV
        saveResult(
                filename,
                mode,
                workers,
                downloadedSize,
                elapsedSeconds,
                throughput,
                sizePassed,
                sha256Passed
        );

        System.out.println(
                "Results saved to result/experiment_results.csv");

        System.out.println(
                "=====================================================");
    }

    // ==========================================
    // List files
    // ==========================================
    private static void listFiles() throws IOException {

        try (Socket socket = new Socket(HOST, PORT)) {

            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();

            Protocol.sendLine(out, "LIST");

            System.out.println(
                    "=========Available files==================");

            String line;

            while ((line = Protocol.readLine(in)) != null) {

                if (line.equals("END")) {
                    break;
                }

                System.out.println("  " + line);
            }
        }
    }

    // ==========================================
    // Get file size
    // ==========================================
    private static long getFileSize(String filename)
            throws IOException {

        try (Socket socket = new Socket(HOST, PORT)) {

            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();

            Protocol.sendLine(out, "INFO " + filename);

            String response = Protocol.readLine(in);

            if (response == null
                    || !response.startsWith("SIZE ")) {

                throw new IOException(
                        "Server response: " + response);
            }

            return Long.parseLong(
                    response.substring(5).trim());
        }
    }

    // ==========================================
    // Save experiment result
    // ==========================================
    private static void saveResult(
            String filename,
            TransferMode mode,
            int workers,
            long sizeBytes,
            double elapsedSeconds,
            double throughput,
            boolean sizePassed,
            boolean sha256Passed) throws IOException {

        Path dir = Paths.get("result");
        Files.createDirectories(dir);
        Path csv = dir.resolve("experiment_results.csv");
        boolean newFile = !Files.exists(csv)
                || Files.size(csv) == 0;

        try (BufferedWriter writer = Files.newBufferedWriter(
                csv,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND)) {

            if (newFile) {

                writer.write(
                        "Filename,Mode,Workers,SizeBytes,"
                        + "TimeSeconds,ThroughputMBs,"
                        + "SizeCheck,SHA256Check");
                writer.newLine();
            }

            writer.write(String.format(
                    Locale.US,
                    "%s,%s,%d,%d,%.3f,%.3f,%s,%s",
                    filename,
                    mode,
                    workers,
                    sizeBytes,
                    elapsedSeconds,
                    throughput,
                    sizePassed ? "PASS" : "FAIL",
                    sha256Passed ? "PASS" : "FAIL"
            ));

            writer.newLine();
        }
    }

    // ==========================================
    // SHA-256 verification
    // ==========================================
    private static String sha256(Path path)
            throws IOException, NoSuchAlgorithmException {

        MessageDigest digest
                = MessageDigest.getInstance("SHA-256");

        try (InputStream in = Files.newInputStream(path)) {

            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = in.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
        }

        StringBuilder hex = new StringBuilder();

        for (byte b : digest.digest()) {

            hex.append(
                    String.format("%02x", b & 0xff));
        }

        return hex.toString();
    }

    // ==========================================
    // Command mode
    // ==========================================
    private static void runCommandMode() {

        Scanner scanner = new Scanner(System.in);

        System.out.println("===== File Transfer Client =====");

        System.out.println(
                "Commands: list, info <filename>, "
                + "get <filename> [traditional|nio] "
                + "[workers] [destination], exit");

        while (true) {

            System.out.print("> ");

            if (!scanner.hasNextLine()) {
                break;
            }

            String input = scanner.nextLine().trim();

            if (input.isEmpty()) {
                continue;
            }

            try {

                List<String> tokens = parseCommand(input);

                String command
                        = tokens.get(0).toLowerCase(Locale.ROOT);

                switch (command) {

                    case "list":

                        if (tokens.size() != 1) {
                            System.out.println("Usage: list");
                            break;
                        }

                        listFiles();
                        break;

                    case "info":

                        if (tokens.size() != 2) {
                            System.out.println(
                                    "Usage: info <filename>");
                            break;
                        }

                        long size = getFileSize(tokens.get(1));

                        System.out.println(
                                "Filename: " + tokens.get(1));

                        System.out.println(
                                "Size: " + size + " bytes");

                        System.out.printf(
                                "Size: %.2f MB%n",
                                size / 1_000_000.0);

                        break;

                    case "get": {

                        if (tokens.size() < 2
                                || tokens.size() > 5) {
                            System.out.println(
                                    "Usage: get <filename> "
                                    + "[traditional|nio] "
                                    + "[workers] [destination]");
                            break;
                        }

                        String[] downloadArgs
                                = tokens.subList(1, tokens.size())
                                        .toArray(new String[0]);

                        downloadFile(downloadArgs);
                        break;
                    }

                    case "exit":

                        if (tokens.size() != 1) {
                            System.out.println("Usage: exit");
                            break;
                        }

                        System.out.println("Exiting Client...");
                        return;

                    default:

                        System.out.println(
                                "Unknown command. "
                                + "Use list, info, get, or exit.");
                }

            } catch (Exception e) {

                System.out.println(
                        "Error: " + e.getMessage());
            }
        }
    }

    // ==========================================
    // Parse command with quoted paths
    // ==========================================
    private static List<String> parseCommand(String input) {

        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        boolean inQuotes = false;

        for (int i = 0; i < input.length(); i++) {

            char c = input.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (Character.isWhitespace(c) && !inQuotes) {

                if (current.length() > 0) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }

            } else {
                current.append(c);
            }
        }

        if (inQuotes) {
            throw new IllegalArgumentException(
                    "Missing closing quote (\").");
        }

        if (current.length() > 0) {
            tokens.add(current.toString());
        }

        return tokens;
    }
}
