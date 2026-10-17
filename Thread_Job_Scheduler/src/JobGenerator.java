import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.Comparator;
import java.util.ArrayList;


/**
 * ปล่อยงานเข้าสู่ระบบตามเวลา arrivalMs ของแต่ละ Job
 *
 * ===== ไฟล์นี้เป็นโครงเปล่า นักศึกษาต้องเขียนเอง =====
 *
 * หน้าที่ (หัวข้อ 3 ของโจทย์):
 *   - รอจนถึงเวลา arrivalMs ของแต่ละงาน แล้วส่งงานต่อไปยัง Scheduler
 *   - บันทึกเวลาที่งานเข้าสู่ระบบ "จริง" ลงใน Job
 *     (อาจไม่ตรงกับ arrivalMs เป๊ะ เพราะ Thread ถูกปลุกช้าได้)
 *   - เรียก logger.jobArrived(job) ทุกครั้งที่ปล่อยงาน
 *
 * ข้อควรคิด:
 *   - รายการงานที่ได้จาก WorkloadLoader เรียงตามลำดับในไฟล์ ไม่ได้เรียงตามเวลา
 *   - เมื่อปล่อยงานครบทุกชิ้นแล้ว ต้องมีวิธีบอกระบบว่า "จะไม่มีงานเข้ามาอีก"
 *     ดู TODO เรื่องการปิดระบบใน Main
 */
public class JobGenerator extends Thread {

    // TODO: เก็บรายการงาน, ช่องทางส่งงานไปยัง Scheduler และ logger
    
    private final List<Job> jobs;
    private final BlockingQueue<Job> arrivalQueue;
    private final ProjectLogger logger;

    public JobGenerator(List<Job> jobs, BlockingQueue<Job> arrivalQueue, ProjectLogger logger) {
        super("generator");
        
        this.jobs = jobs;
        this.logger = logger;
        this.arrivalQueue = arrivalQueue; // เก็บ queue ที่ใช้ส่งงานให้ scheduler

    }

    @Override
    public void run() {

        List<Job> sortedJobs = new ArrayList<>(jobs); // สร้างสำเนารายการ jobs (ไม่เปลี่ยนต้นฉบับ)
        sortedJobs.sort(Comparator.comparingLong(job -> job.arrivalMs)); // เรียงลำดับตามเวลาที่เข้าระบบจากน้อยไปมาก

        for (Job job : sortedJobs) {

            long currentTime = logger.now();  // ตอนนี้ simulation ผ่านมาแล้ว ms?
            long waitMs = job.arrivalMs - currentTime; // ต้องรออีกกี่ ms ถึงเวลาของ Job?


            // ✅ 1) ยังไม่ถึงเวลา arrival -> พัก Thread (sleep)
            //     try-catch เพราะ sleep() , put() สามารถถูก interrupt จาก Thread อื่นได้ 
            try {
                if( waitMs > 0 ) { 
                        Thread.sleep(waitMs);
                    }

                // ✅ 2. ถึงเวลาแล้ว บันทึกเวลาที่ Job เข้าระบบจริง
                job.setActualArrivalTime(logger.now());

                // ✅ 3. Log ว่า Job มาถึงแล้ว
                logger.jobArrived(job);
                arrivalQueue.put(job); 

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();   // ถ้า Generator ถูก interrupt: interrupt() → บอกสถานะว่า Thread นี้ถูก interrupt 
                return;                               //    return → ออกจาก run() และหยุด Generator 
            }
            
            

            
        }
    }
}
