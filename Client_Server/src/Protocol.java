import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public final class Protocol {
    private Protocol() {
    }

    public static void sendLine(OutputStream out, String line)
            throws IOException {
        out.write((line + "\n").getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    // อ่านเฉพาะบรรทัดคำสั่ง ไม่อ่านล้ำไปถึงข้อมูลไฟล์
    public static String readLine(InputStream in) throws IOException {
        StringBuilder result = new StringBuilder();

        while (true) {
            int value = in.read();

            if (value == -1) {
                if (result.length() == 0) {
                    return null;
                }
                break;
            }

            if (value == '\n') {
                break;
            }

            if (value != '\r') {
                result.append((char) value);
            }
        }

        return result.toString();
    }
}
