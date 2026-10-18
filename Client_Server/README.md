# งานปฏิบัติการ: Multi-threaded File Download Client/Server 

## 1. ภาพรวมโปรเจกต์

เป็นการพัฒนาระบบรับส่งไฟล์ผ่านเครือข่ายในเครื่องเดียวกันด้วยภาษา Java โดยประกอบด้วย Server และ Client ซึ่งสื่อสารกันผ่าน Network Socket เพื่อให้ Client สามารถเรียกดูข้อมูลไฟล์และดาวน์โหลดไฟล์จาก Server ได้


## 2. ความสามารถของระบบ

- **แสดงรายการไฟล์ (List Files):** แสดงรายชื่อไฟล์ที่มีอยู่บน Server
- **ดูข้อมูลไฟล์ (File Information):** แสดงชื่อและขนาดของไฟล์บน Server
- **ดาวน์โหลดไฟล์ (File Download):** ดาวน์โหลดไฟล์จาก Server มายัง Client
- **การทำงานแบบหลายเธรด (Multithreading):** ใช้ Worker Threads ช่วยรับส่งข้อมูลพร้อมกัน
- **วิธีการรับส่งไฟล์ (Transfer Methods):** รองรับ Traditional I/O, NIO และ Native Transfer
- **ตรวจสอบความถูกต้องของไฟล์ (File Integrity Verification):** ใช้ SHA-256 เปรียบเทียบความถูกต้องของไฟล์ต้นฉบับและไฟล์ที่ดาวน์โหลด
- **บันทึกผลการทดลอง (Performance Measurement):** บันทึกเวลาที่ใช้และ Throughput ลงในไฟล์ CSV

## 3. โครงสร้างโปรเจกต์

```text
Multithreaded_2_final/
├── src/
│   ├── Client.java
│   ├── Server.java
│   └── LoadWorker.java
├── server_files/
│   └── Test.txt
├── downloads/
├── result/
│   └── experiment_results.csv
└── README.md
```

