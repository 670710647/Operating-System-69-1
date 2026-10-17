import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.CountDownLatch;

/**
 * จุดเริ่มต้นของโปรแกรม
 *
 * ===== ไฟล์นี้เป็นโครงเปล่า นักศึกษาต้องเขียนเอง =====
 *
 * ส่วนที่เขียนไว้ให้แล้วคือการรับค่า การโหลด workload และการแสดง error
 * ซึ่งไม่ใช่สิ่งที่โครงงานนี้วัด ส่วนที่เหลือเป็น TODO ทั้งหมด
 *
 * วิธีรัน:
 *   java Main jobs_standard.csv priority 3 1 2
 */
public class Main {

    public static void main(String[] args) {
        // ✅ ---------- 1. รับค่าจาก command line ----------
        Config config;
        try {
            config = Config.parse(args);
        } catch (IllegalArgumentException e) {
            System.err.println("ผิดพลาด: " + e.getMessage());
            System.err.println();
            System.err.println(Config.USAGE);
            System.exit(1);
            return;
        }


        // ✅ ---------- 2. เริ่มจับเวลาและโหลด workload ----------
        ProjectLogger logger = new ProjectLogger();
        List<Job> jobs;
        try {
            jobs = WorkloadLoader.load(config.workloadPath);
        } catch (WorkloadFormatException e) {
            System.err.println("ไฟล์ workload ผิดรูปแบบ — " + e.getMessage());
            System.exit(1);
            return;
        } catch (java.io.IOException e) {
            System.err.println("เปิดไฟล์ \"" + config.workloadPath + "\" ไม่ได้");
            System.err.println("ตรวจว่าไฟล์มีอยู่จริงและ path ถูกต้อง (สั่ง java จากโฟลเดอร์ใด)");
            System.exit(1);
            return;
        }
        logger.systemStart(config);
        logger.systemEvent("โหลดงานได้ " + jobs.size() + " ชิ้น");


        // ✅ ---------- 3. สร้างส่วนประกอบของระบบ ----------

        ResourceManager resources = new ResourceManager(
            config.printerPermits,
            config.databasePermits
        );

        ReadyQueue readyQueue = new ReadyQueue(config.policy);

        Statistics statistics = new Statistics();

        CountDownLatch jobsDone = new CountDownLatch(jobs.size());

        BlockingQueue<Job> arrivalQueue = new LinkedBlockingQueue<>();


        // ✅  ---------- 4. สร้างและเริ่ม Thread ----------

        // สร้าง Worker จำนวน config.workers ตัว แล้ว start
        Worker[] workers = new Worker[config.workers];

        for( int i=0 ; i<workers.length ; i++ ) {

            workers[i] = new Worker( "worker-" + (i+1), readyQueue, resources, statistics, logger, jobsDone);

        }

        // TODO: สร้างและ start Scheduler

        Scheduler scheduler = new Scheduler(readyQueue, logger, arrivalQueue);

        // TODO: สร้างและ start Monitor
        Monitor monitor = new Monitor(readyQueue, resources, statistics, logger);

        // TODO: สร้างและ start JobGenerator
        JobGenerator generator = new JobGenerator(jobs, arrivalQueue, logger);

        // เริ่ม Thread ทุกตัว
        for( Worker worker : workers ) {
            worker.start();
        }

        scheduler.start();
        monitor.start();
        generator.start();


        // ลำดับการ start มีผลหรือไม่ ให้คิดและอธิบายได้ใน Demo

        // ✅ ---------- 5. รอจนงานเสร็จครบ ----------
        
        try {

            // รอให้ Generator ส่งงานจนครบ
            generator.join();

            // ส่งสัญญาณให้ Scheduler หยุด
            arrivalQueue.put(new Job("__STOP__", 0L, Integer.MAX_VALUE, 0L, ResourceType.NONE, 0L, Integer.MAX_VALUE));

            // รอให้ Scheduler ส่งงานที่รับมาทั้งหมดเข้า ReadyQueue
            scheduler.join();

            // รอให้ Worker ทำ Job ครบทุกชิ้น
            jobsDone.await();


        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();
            return;

        }

        
        // ✅ ---------- 6. สั่งหยุดทุก Thread ----------
        // TODO: หยุด Worker ทุกตัว, Scheduler, Monitor และ JobGenerator
        // TODO: join ทุก Thread เพื่อยืนยันว่าหยุดจริงก่อนไปขั้นถัดไป

        try {

            // สั่งให้ monitor หยุดก่อน
            monitor.stopMonitoring();

            // รอจนกว่า monitor หยุดจริง
            monitor.join();

                
            // หยุด Worker หลังงานจริงเสร็จครบแล้ว
            for( Worker worker : workers ) {
                worker.interrupt();
            }

            // ให้ Main รอจน Worker แต่ละตัวจบจริง 
            for( Worker worker : workers ) {
                worker.join();
            }

        } catch ( InterruptedException e ) {

            Thread.currentThread().interrupt();
            return;

        }

        // ✅ ---------- 7. สรุปผล ----------
        // สรุปผลเพียงครั้งเดียว หลัง Worker หยุดครบแล้ว

        long makespanMs = 0;

        for( Job job : jobs ) {
            makespanMs = Math.max(makespanMs, job.getCompletionTime());
        }

        statistics.printSummary(jobs, makespanMs);

        logger.systemStop(statistics.completedCount(), jobs.size());


    }
}
