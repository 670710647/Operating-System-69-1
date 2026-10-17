import java.util.concurrent.Semaphore;
/**
 * ควบคุมสิทธิ์การใช้ทรัพยากรร่วมของทั้งระบบ
 *
 * ===== ไฟล์นี้เป็นโครงเปล่า นักศึกษาต้องเขียนเอง =====
 *
 * ข้อกำหนดจากโจทย์ที่เกี่ยวกับคลาสนี้:
 *   - หัวข้อ 5: ใช้ Semaphore ควบคุม PRINTER และ DATABASE
 *     จำนวน permit มาจาก command line (Config)
 *     ในส่วนบังคับให้สร้าง Semaphore แบบ fair = true
 *   - Worker ทุกตัวต้องใช้ ResourceManager object เดียวกัน
 *   - หัวข้อ 7: permit ต้องไม่สูญหายหรือค้าง แม้เกิด exception
 *     หรือถูก interrupt ระหว่างถือ resource
 *
 * คำถามที่จะถูกถามใน Demo:
 *   - ทำไมต้อง fair = true และถ้าเปลี่ยนเป็น false จะเกิดอะไรขึ้น
 *   - ถ้า Thread ถูก interrupt หลัง acquire สำเร็จแต่ก่อน release
 *     โค้ดของกลุ่มยังคืน permit ได้หรือไม่
 */
public class ResourceManager {

    // ✅ เก็บ Semaphore ของ PRINTER และ DATABASE

    private final Semaphore printer; // ใช้ควบคุมจำนวน Job ที่ใช้ Printer พร้อมกันได้
    private final Semaphore database; // ช้ควบคุมจำนวน Job ที่ใช้ Database พร้อมกันได้

    private final int printerTotal;
    private final int databaseTotal;

    public ResourceManager(int printerPermits, int databasePermits) { 

        // permits = จำนวนที่ยอมอนุญาตให้ใช้พร้อมกันได้
        this.printer = new Semaphore(printerPermits, true); // fair เพื่อให้การรอรับสิทธิ์เป็นธรรมมากขึ้น
        this.database = new Semaphore(databasePermits, true);
        
        this.printerTotal = printerPermits;
        this.databaseTotal = databasePermits;
    }

    // ✅ ขอสิทธิ์ใช้ทรัพยากร จะรอจนกว่าจะได้ 
    public void acquire(ResourceType type) throws InterruptedException {
        // ขอ permit จาก printer, database
        switch (type) {

            case PRINTER:
                printer.acquire(); 
                break;

            case DATABASE:
                database.acquire();  
                break;

            case NONE:
                break;
        }
    }

    // ✅ คืนสิทธิ์ใช้ทรัพยากร 
    public void release(ResourceType type) {
        // คืน permit ให้ printer, database 
        switch (type) {

            case PRINTER:
                printer.release();
                break;
            
            case DATABASE:
                database.release();
                break;
                
            case NONE:
                break;
        }
    }

    /*
     * ข้อความสั้น ๆ บอกสถานะการใช้ทรัพยากร สำหรับส่งให้ ProjectLogger.monitor()
     * เช่น "printer=1/1 database=0/2"
     */

    // เมธอดนี้ใช้รายงานสถานะของ Resource ว่ามี permit ถูกใช้อยู่กี่ตัว จากทั้งหมดกี่ตัว 
    // เพื่อให้ Monitor แสดงสถานะได้

    public String status() {
        // ✅ TODO
        int printerUsed = printerTotal - printer.availablePermits();
        int databaseUsed = databaseTotal - database.availablePermits();

        return "printer=" + printerUsed + "/" + printerTotal
            + ", database=" + databaseUsed + "/" + databaseTotal;
        
    }
}
