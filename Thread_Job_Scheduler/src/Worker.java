import java.util.concurrent.CountDownLatch;
/**
 * Thread ที่ดึงงานจาก Ready Queue ไปทำจนเสร็จ
 *
 * ===== ไฟล์นี้เป็นโครงเปล่า นักศึกษาต้องเขียนเอง =====
 *
 * ลำดับการทำงานของ Job หนึ่งชิ้น บังคับตามหัวข้อ 6 ของโจทย์:
 *   1. รับงานจาก Ready Queue แล้วบันทึกเวลาเริ่ม
 *   2. จำลองงานหลักด้วย Thread.sleep(job.workMs)
 *   3. ถ้า job.resource != NONE ให้บันทึกเวลาเริ่มรอ แล้ว acquire
 *   4. จำลองการถือครองด้วย Thread.sleep(job.resourceMs)
 *   5. release แล้วบันทึกเวลาจบ
 *
 * ห้ามสลับขั้นที่ 2 กับ 3 เพราะจะทำให้ผลของทุกกลุ่มเทียบกันไม่ได้
 *
 * จุดที่มักพลาด:
 *   - ถ้า exception หรือ interrupt เกิดขึ้นหลัง acquire แต่ก่อน release
 *     permit จะค้างถาวรและระบบจะแขวน ต้องออกแบบให้คืนได้เสมอ
 *   - Worker ต้องหยุดเองได้เมื่อไม่มีงานเหลือแล้ว ไม่ใช่วนรอตลอดไป
 */
public class Worker extends Thread {

    // ✅ เพิ่ม field
    private final ReadyQueue readyQueue;       // รับ job ที่ scheduler จัดคิวไว้
    private final ResourceManager resources;   // จัดการการใชเ Printer หรือ Database
    private final Statistics statistics;       // บันทึกผลเมื่อ Job ทำงานเสร็จ
    private final ProjectLogger logger;        // บันทึกเหตุการณ์การทำงาน

    private final CountDownLatch jobsDone;     // เป็นตัวนับงานทั้งหมดที่ยังไม่เสร็จ

    // ✅ แก้ constructor
    public Worker(String name, ReadyQueue readyQueue, ResourceManager resources,
                  Statistics statistics, ProjectLogger logger, CountDownLatch jobsDone) {
        super(name);
        
        this.readyQueue = readyQueue;
        this.resources = resources;
        this.statistics = statistics;
        this.logger = logger;
        this.jobsDone = jobsDone;
    }

    // ✅ วนรับงานและเรียก processJob จนกว่าจะได้รับสัญญาณให้หยุด
    @Override
    public void run() {

         while (!Thread.currentThread().isInterrupted()) {

            try {

                Job job = readyQueue.take();
                processJob(job);

            } catch (InterruptedException e) {

                // ถูก interrupt ขณะรอรับงาน จึงหยุดได้อย่างปลอดภัย
                Thread.currentThread().interrupt();
                break;

            }

        }

    }
 
    // ✅ ทำงานหนึ่งชิ้นให้จบตามลำดับ 5 ขั้นด้านบน
    private void processJob(Job job) {
        boolean acquired  = false;
        boolean completedNormally = false;

        // 1. บันทึกเวลาเริ่มทำงาน
        job.setStartTime(logger.now());
        logger.jobStarted(job);
        statistics.recordStart();

        try {

            // 2. ทำงานหลักให้ครบก่อนขอ Resource
            Thread.sleep(job.workMs);
            logger.workFinished(job);


            // 3. ขอ Resource ถ้าจำเป็น
            if (job.resource != ResourceType.NONE) {
                job.setResourceWaitStartTime(logger.now());
                logger.resourceWaitStarted(job);

                // ถ้าถูก interrupt ระหว่างรอ จะออกจากเมธอดนี้
                acquireInterruptibly(job.resource);
                acquired = true;


                job.setResourceWaitTime(logger.now() - job.getResourceWaitStartTime());
                logger.resourceAcquired( job, job.getResourceWaitTime() );

                
                try {

                    Thread.sleep(job.resourceMs);

                } finally {

                    // คืน permit เฉพาะเมื่อ acquire เสร็จ
                    if( acquired ) {
                        resources.release(job.resource);
                        acquired = false;
                        logger.resourceReleased(job);
                    }
                }
            }

            completedNormally = true;

        } catch ( InterruptedException e ) {

            // หยุดงานปัจจุบันเมื่อถูก interrupt
            Thread.currentThread().interrupt();

        } finally {

            if( completedNormally ) {
                job.setCompletionTime(logger.now());
                statistics.recordCompletion(job);
                logger.jobCompleted(job);
                jobsDone.countDown();
            }

        }
    }
    
    private void acquireInterruptibly(ResourceType resource)
            throws InterruptedException {
        resources.acquire(resource);
    }

}
