# โปรเจกต์ Java Multithreaded Job Scheduler

## 1. รายละเอียดโปรเจกต์
โปรแกรมจำลองระบบจัดตารางงานโดยใช้ Java Thread รองรับการจัดตารางงานแบบ FCFS และ Priority Scheduling ใช้ Worker หลายตัวทำงานพร้อมกัน และใช้ Semaphore ควบคุมการเข้าถึงทรัพยากร Printer และ Database พร้อมบันทึก Log และคำนวณค่าประสิทธิภาพของระบบ

## 2. วิธี Compile

เปิด Terminal ในโฟลเดอร์ที่มีไฟล์ Java ของโปรเจกต์ แล้วใช้คำสั่ง

```bash
javac *.java
```

## 3. วิธี Run

รูปแบบคำสั่ง

```bash
java Main <workload.csv> <fcfs|priority> <workers> <printerPermits> <databasePermits>
```

ตัวอย่าง

```bash
java Main jobs_standard.csv priority 3 1 2
```

ความหมายของ Command-line Arguments

- `workload.csv` คือไฟล์ข้อมูลรายการงาน เช่น `jobs_standard.csv` หรือ `jobs_printer.csv`
- `fcfs|priority` คือรูปแบบการจัดตารางงาน โดยเลือก `fcfs` หรือ `priority`
- `workers` คือจำนวน Worker Threads
- `printerPermits` คือจำนวนสิทธิ์การใช้ Printer พร้อมกัน
- `databasePermits` คือจำนวนสิทธิ์การใช้ Database พร้อมกัน

## 4. การบันทึก Log

สามารถบันทึกผลลัพธ์ลงไฟล์ได้ด้วยคำสั่ง

```bash
java Main jobs_standard.csv priority 3 1 2 > run.log
```

## 5. ข้อจำกัดที่ควรรู้

- ต้องระบุ Command-line Arguments ให้ครบทั้ง 5 ค่า และใช้รูปแบบที่ถูกต้อง
- ไฟล์ CSV ที่ระบุต้องมีอยู่จริงและอยู่ใน Path ที่โปรแกรมเข้าถึงได้
- จำนวน Worker และจำนวนสิทธิ์การใช้ทรัพยากรต้องเป็นค่าที่ถูกต้องตามข้อกำหนดของโปรแกรม
- เวลาและลำดับบางเหตุการณ์ใน Log อาจแตกต่างกันเล็กน้อยระหว่างการรันแต่ละครั้ง เนื่องจากระบบปฏิบัติการจัดสรรเวลาการทำงานให้ Thread
- ควรใช้ Log จากการรันจริงในการวิเคราะห์ค่าประสิทธิภาพของระบบ