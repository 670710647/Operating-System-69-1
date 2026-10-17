import java.util.concurrent.BlockingQueue;

/**
 * รับงานจาก JobGenerator แล้วจัดเข้า Ready Queue
 *
 * ===== ไฟล์นี้เป็นโครงเปล่า นักศึกษาต้องเขียนเอง =====
 *
 * ข้อกำหนดจากโจทย์ (หัวข้อ 2 และ 4):
 *   - Scheduler เป็น Thread บังคับ ห้ามให้ JobGenerator ใส่งานลง Ready Queue โดยตรง
 *   - รับผิดชอบการจัดลำดับตามนโยบาย FCFS หรือ Priority
 *
 * ข้อควรคิด:
 *   - Scheduler รับงานจาก JobGenerator ผ่านอะไร และรอโดยไม่กิน CPU อย่างไร
 *   - เมื่อ JobGenerator ปล่อยงานครบแล้ว Scheduler รู้ได้อย่างไรว่าควรหยุด
 */
public class Scheduler extends Thread {

    // TODO: เก็บช่องทางรับงานจาก JobGenerator, ReadyQueue ปลายทาง และ logger
    //
    // หมายเหตุ: constructor ด้านล่างยังไม่มี parameter สำหรับ "ช่องทางรับงาน"
    // ให้เพิ่มเข้าไปให้ตรงกับที่ออกแบบไว้ใน JobGenerator
    // เพิ่ม parameter ได้ แต่อย่าเปลี่ยนชื่อคลาส

    private final ReadyQueue readyQueue;
    private final BlockingQueue<Job> arrivalQueue;
    private final ProjectLogger logger;

    public Scheduler(ReadyQueue readyQueue, ProjectLogger logger, BlockingQueue<Job> arrivalQueue) {
        super("scheduler");
        
        this.readyQueue = readyQueue;
        this.arrivalQueue = arrivalQueue;
        this.logger = logger;

    }

    @Override
    public void run() {
        // TODO: วนรับงานเข้ามาแล้วใส่ ReadyQueue จนกว่าจะได้รับสัญญาณให้หยุด
        try {

            // Scheduler ทำงานไปเรื่อย ๆ เพราะเราไม่รู้ว่า Job จะเข้ามากี่ตัว
            while (true) {
                Job job = arrivalQueue.take();

                // ตรวจว่าเป็นสัญญาณให้หยุดหรือไม่
                if("__STOP__".equals(job.id)) {
                    break;
                }

                readyQueue.add(job);
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
