import java.util.List;

/**
 * รวบรวมและคำนวณค่าที่ใช้วัดผลของการรันหนึ่งครั้ง
 *
 * ===== ไฟล์นี้เป็นโครงเปล่า นักศึกษาต้องเขียนเอง =====
 *
 * ข้อกำหนดจากโจทย์ที่เกี่ยวกับคลาสนี้ (หัวข้อ 8):
 *   - Waiting Time, Turnaround Time, Throughput, Resource Wait Time
 *   - ต้องถูกอัปเดตจากหลาย Worker พร้อมกันได้อย่างปลอดภัย
 *   - ผลต้องสอดคล้องกับสมการตรวจสอบ:
 *       Turnaround = Waiting + workMs + Resource Wait + resourceMs
 *     ใช้สมการนี้ตรวจงานทีละชิ้นได้ว่าค่าไหนคำนวณผิด
 *
 * ข้อควรระวัง: ค่าเฉลี่ยของ Resource Wait ให้คิดเฉพาะงานที่ใช้ resource
 * ส่วนงานที่ resource = NONE ให้ถือว่า Resource Wait เป็น 0
 */
public class Statistics {

    // ✅ เก็บข้อมูลของงานที่เสร็จแล้ว หรือเก็บผลรวมไว้คำนวณทีหลัง
    private int completed = 0;
    private int running = 0;

    private long totalWaitingTime = 0;
    private long totalTurnAroundTime = 0;
    private long totalResourceWaitTime = 0;

    private int resourceJobCount = 0;

    // บันทึกว่างานชิ้นหนึ่งเสร็จแล้ว เรียกโดย Worker หลายตัวพร้อมกันได้
    // มี Synchronized ทำให้ worker เข้ามา update ข้อมูลใน method นี้ได้ทีละตัว
    public synchronized void recordCompletion(Job job) {
        // ✅ TODO

        long waitingTime = job.getStartTime() - job.getActualArrivalTime();
        
        long turnaroundTime = job.getCompletionTime() - job.getActualArrivalTime();

        totalWaitingTime += waitingTime;
        totalTurnAroundTime += turnaroundTime;

        if( job.resource != ResourceType.NONE )
        {
            totalResourceWaitTime += job.getResourceWaitTime();
            resourceJobCount++;
        }

        running--;
        completed++;
        
    }

    /** จำนวนงานที่เสร็จแล้ว ใช้โดย Monitor และใช้ตรวจว่างานครบหรือยัง */
    public synchronized int completedCount() {
        // ✅ TODO
        return completed;
    }

    // เรียกเมื่อ Worker เริ่มทำ Job
    public synchronized void recordStart() {
        running++;
    }

    // จำนวน Job ที่กำลังทำอยู่
    public synchronized int runningCount() {
        return  running;
    }

    /**
     * พิมพ์ตารางสรุปผลตอนจบโปรแกรม
     * อย่างน้อยต้องมี avg Waiting Time, avg Turnaround Time,
     * Throughput และ avg Resource Wait Time
     *
     * ตามหัวข้อ 14 ให้รายงานเวลาเป็นจำนวนเต็มหน่วย ms
     * และ Throughput อย่างน้อย 2 ตำแหน่งทศนิยม
     */
    public synchronized void printSummary(List<Job> allJobs, long makespanMs) {
        // ✅ TODO

        double avgWaitingTime = 0;
        double avgTurnaroundTime = 0;
        double avgResourceWaitTime = 0;
        double throughput = 0; // throughput = ในหนึ่งวิทำงานเสร็จได้กี่งาน, makespanMs = ใช้เวลาทำงานทั้งหมดนานเท่าไหร่ 

        if( completed > 0 ) {
            avgWaitingTime = (double) totalWaitingTime / completed;
            avgTurnaroundTime = (double) totalTurnAroundTime / completed;
        }

        if( resourceJobCount > 0 ) {
            avgResourceWaitTime = (double) totalResourceWaitTime / resourceJobCount;
        }
        
        if( makespanMs > 0 ) {
            throughput = (double) completed / makespanMs * 1000;
        }

        System.out.println("\n========== SUMMARY ==========");
        System.out.println("Total jobs         : " + allJobs.size());
        System.out.println("Completed jobs     : " + completed );
        System.out.println("Avg Waiting Time   : " +  Math.round(avgWaitingTime) + " ms");
        System.out.println("Avg Turnaround     : " + Math.round(avgTurnaroundTime) + " ms");
        System.out.println("Avg Resource Wait  : " + Math.round(avgResourceWaitTime) + " ms");

        System.out.printf("Throughput       : %.2f jobs/sec%n", throughput);
        System.out.println("Makespan        : " + makespanMs + " ms");
        System.out.println("=============================");
    }
}
