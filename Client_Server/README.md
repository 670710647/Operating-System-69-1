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

## 4. สิ่งที่ต้องเตรียม

- Java Development Kit (JDK)
- Command Prompt หรือ Terminal
- ไฟล์สำหรับทดสอบการรับส่ง ซึ่งอยู่ในโฟลเดอร์ที่ Server ใช้เก็บไฟล์

## 5. การ Compile โปรแกรม

เปิด Command Prompt หรือ Terminal ในโฟลเดอร์โปรเจกต์ แล้ว Compile ไฟล์ Java

```bash
javac -d out src/*.java
```

คำสั่งนี้เป็นตัวอย่างสำหรับกรณีที่ไฟล์ Java อยู่ในโฟลเดอร์ `src` 

## 6. การเริ่มต้น Server

เปิด Terminal หน้าต่างแรก แล้วรันคำสั่ง

```bash
java -cp out Server
```

ให้ Server ทำงานค้างไว้ระหว่างที่ Client เชื่อมต่อและรับส่งไฟล์

## 7. การใช้งาน Client

เปิด Terminal อีกหน้าต่างหนึ่งในโฟลเดอร์โปรเจกต์

### 7.1 ดาวน์โหลดไฟล์โดยตรง

ตัวอย่างการดาวน์โหลดไฟล์ `Test.txt` ด้วย Traditional I/O และ Worker จำนวน 1 เธรด

```bash
java -cp out Client Test.txt traditional 1
```

รูปแบบคำสั่งทั่วไป

```text
java -cp out Client <filename> [traditional|nio|native] [workers] [destination]
```

ความหมายของพารามิเตอร์

- `filename` คือชื่อไฟล์ที่ต้องการดาวน์โหลดจาก Server
- `traditional|nio|native` คือวิธีการรับส่งไฟล์ที่ต้องการใช้
- `workers` คือจำนวน Worker Threads
- `destination` คือพาธปลายทางสำหรับบันทึกไฟล์ที่ดาวน์โหลด โดยเป็นพารามิเตอร์ที่ไม่จำเป็นต้องระบุเสมอไป

### 7.2 การใช้งาน Interactive Command Mode

สามารถเริ่ม Client โดยไม่ต้องระบุอาร์กิวเมนต์ได้

```bash
java -cp out Client
```

จากนั้นพิมพ์คำสั่งที่ต้องการใช้งาน

| คำสั่ง | รายละเอียด |
|---|---|
| `list` | แสดงรายการไฟล์บน Server |
| `info <filename>` | แสดงข้อมูลและขนาดของไฟล์ |
| `get <filename> [method] [workers] [destination]` | ดาวน์โหลดไฟล์ |
| `exit` | ออกจากโปรแกรม Client |

ตัวอย่างการใช้งาน

```text
list
info Test.txt
get Test.txt traditional 1
get Test.txt nio 10
get Test.txt native 10
exit
```

## 8. การทดลองเปรียบเทียบประสิทธิภาพ

การทดลองมีจุดประสงค์เพื่อศึกษาความแตกต่างของประสิทธิภาพในการรับส่งไฟล์ โดยเปรียบเทียบวิธีการรับส่งข้อมูลและจำนวน Worker Threads

ตัวอย่างชุดการทดลอง

| การทดลอง | วิธีรับส่งไฟล์ | จำนวน Worker |
|---|---|---:|
| 1 | Traditional | 1 |
| 2 | Traditional | 10 |
| 3 | NIO | 1 |
| 4 | NIO | 10 |

ใช้ไฟล์ทดสอบเดียวกันและกำหนดเงื่อนไขการทดลองให้เหมือนกันในแต่ละกรณี รวมถึงทดลองซ้ำ 3 ครั้งเพื่อคำนวณค่าเฉลี่ย

## 9. ตัวชี้วัดประสิทธิภาพ

- **Transfer Time:** ระยะเวลาที่ใช้ในการรับส่งไฟล์จนเสร็จสมบูรณ์
- **Throughput (MB/s):** ปริมาณข้อมูลที่รับส่งได้ต่อวินาที โดยใช้หน่วยเมกะไบต์ต่อวินาที
- **SHA-256 Verification:** การตรวจสอบความถูกต้องของไฟล์ที่ดาวน์โหลด โดยเปรียบเทียบค่าแฮชกับไฟล์ต้นฉบับ

ผลการตรวจสอบความถูกต้องควรผ่านก่อนนำผลการทดลองของแต่ละครั้งไปวิเคราะห์

## 10. ตำแหน่งจัดเก็บผลการทดลอง

ผลการทดลองจะถูกบันทึกไว้ในไฟล์

```text
result/experiment_results.csv
```


## 11. ข้อควรระวัง

- ต้องเริ่มต้น Server ก่อนใช้งาน Client
- ตรวจสอบว่าไฟล์ที่ต้องการดาวน์โหลดมีอยู่ในโฟลเดอร์ที่ Server ให้บริการ
- ใช้ไฟล์ทดสอบเดียวกันในการเปรียบเทียบแต่ละวิธี
- ตรวจสอบจำนวน Worker Threads ให้ตรงกับชุดการทดลอง
- ตรวจสอบผล SHA-256 ก่อนสรุปผลการทดลอง
- ตรวจสอบว่าข้อมูลในไฟล์ CSV ถูกบันทึกครบถ้วนก่อนนำไปคำนวณค่าเฉลี่ย




