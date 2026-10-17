import java.util.Comparator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
/**
 * คิวงานที่พร้อมถูกหยิบไปทำ
 *
 * ===== ไฟล์นี้เป็นโครงเปล่า นักศึกษาต้องเขียนเอง =====
 *
 * สิ่งที่คลาสนี้ต้องทำได้:
 *   - เก็บงานที่รอ Worker อยู่
 *   - หยิบงานถัดไปตามนโยบายที่เลือก (FCFS หรือ Priority)
 *   - ถูกเรียกจากหลาย Thread พร้อมกันได้อย่างปลอดภัย
 *
 * ข้อกำหนดจากโจทย์ที่เกี่ยวกับคลาสนี้:
 *   - หัวข้อ 4: priority = 1 สูงสุด เมื่อเท่ากันต้องมีกติกาตัดสินลำดับ (tie-break)
 *     ที่ตัดสินจากข้อมูลของ Job ไม่ขึ้นกับว่า Thread ใดเข้าถึงคิวก่อน
 *   - หัวข้อ 7: ห้ามวนลูปเช็กแบบกิน CPU (busy waiting) — Worker ที่ไม่มีงานทำ
 *     ต้องถูกพักไว้ ไม่ใช่วนถามซ้ำ ๆ
 *
 * จะออกแบบเป็นคลาสเดียวที่รับนโยบายเข้ามา หรือแยกเป็นสองคลาส
 * หรือใช้โครงสร้างข้อมูลสำเร็จรูปของ Java ก็ได้ ขอให้อธิบายเหตุผลได้ใน Demo
 */
public class ReadyQueue {

    // ✅ เก็บนโยบาย (Config.Policy) และโครงสร้างข้อมูลที่ใช้เก็บงาน 
    private final Config.Policy policy;
    private final BlockingQueue<Job> queue;

    public ReadyQueue(Config.Policy policy) {
        // ✅
        this.policy = policy;

        if( policy == Config.Policy.FCFS )
        {
            this.queue = new LinkedBlockingQueue<>(); // มันจะเก็บตามลำดับที่ Scheduler เอางานเข้ามา
        }
        else
        {
            Comparator<Job> comparator = Comparator.comparingInt((Job job) -> job.priority) // comparator บอกว่า priority น้อยกว่า → มาก่อน
                                                   .thenComparingLong(job -> job.sequence); // ถ้า priority เท่ากัน เราใช้ job.sequence

            this.queue = new PriorityBlockingQueue<>(11, comparator);
        }
 
    }

    public void add(Job job) {
        // ✅ Scheduler เรียก , เอางานเข้า ReadyQueue

        queue.add(job); // Scheduler เอา Job มาใส่ ReadyQueue
    }

    public Job take() throws InterruptedException {
        // ✅ Worker เรียก , เอางานออกไปทำ

        return queue.take(); 
    }

    public int size() {
        // ✅ Monitor เรียก , ดูว่ามีงานรอกี่งาน

        return queue.size();
    }
}
