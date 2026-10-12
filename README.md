# Net send — bài tập Multicast

Gửi và nhận tin giữa các máy cùng mạng LAN trong một cửa sổ. Gửi nhiều lần liên tiếp, gõ `exit` để thoát.

## Chuẩn bị

Mỗi máy cài **JDK 8 trở lên**, chép file `Net.java` và mở CMD/terminal tại thư mục chứa file. Kiểm tra bằng `java -version` và `javac -version`.

## Biên dịch và chạy

**Windows CMD:**

```bat
if not exist build mkdir build
javac -encoding UTF-8 -d build Net.java
java -cp build Net listen lop1
```

**Linux/macOS:**

```sh
mkdir -p build
javac -encoding UTF-8 -d build Net.java
java -cp build Net listen lop1
```

Chỉ cần biên dịch lại khi sửa `Net.java`. Những lần sau chạy `java -cp build Net listen lop1`.

## Vừa nghe nhận, vừa gửi tin

**Cả máy A và máy B** đều chạy:

```text
java -cp build Net listen lop1
```

Chương trình tự nghe và hiển thị tin đến, không cần gõ lệnh nhận. Giữ cửa sổ mở; mỗi máy có thể nhập lệnh gửi ngay trong cửa sổ đó.

Sau khi chương trình mở, nhập tại dấu `>` rồi nhấn Enter:

```text
net listen group1
net send group1 Xin chao group1
net outgroup group1
net send 192.168.1.20 Xin chao mot may
net send lop1 Xin chao nhom
net send * Xin chao tat ca
exit
```

Đây là lệnh nhập **trong chương trình**, không phải lệnh của CMD/terminal. Không cần dấu nháy cho `*`.

| Đích gửi | Ai nhận được? |
|---|---|
| IP | Máy có IP đó đang chạy chương trình |
| `lop1` | Các máy đang tham gia nhóm `lop1` |
| `*` | Mọi máy đang chạy chương trình trong LAN hỗ trợ multicast |

Thay IP ví dụ bằng IP thật: xem bằng `ipconfig` trên Windows hoặc `ip -4 addr` trên Linux.

Ví dụ A và B chạy `listen lop1`, C chạy `listen lop2`: A gửi tới IP của B thì chỉ B nhận; gửi tới `lop1` thì A và B nhận; gửi tới `*` thì cả ba nhận.

Máy nhận hiển thị: `[Tu 192.168.1.10 | lop1] Xin chao nhom`. Có thể trả lời ngay trong cùng cửa sổ.

## Listen vừa nghe vừa gửi

`listen` tự nghe tin bằng một luồng riêng và dùng `Scanner` để nhập lệnh gửi liên tục. Không nhập gì thì chương trình vẫn nghe nhận tin. Gõ `exit` để đóng chương trình.

Nếu chỉ cần gửi một lần từ CMD/terminal:

```text
java -cp build Net send lop1 Xin chao nhom
```

Chế độ `send` này gửi xong rồi kết thúc; dùng `listen` để hội thoại liên tục.

## Chọn nhóm

```text
java -cp build Net listen lop2
java -cp build Net listen lop1 lop2
java -cp build Net listen
```

Lần lượt: tham gia `lop2`, tham gia hai nhóm, hoặc chỉ nhận tin tới IP và `*`. Tên nhóm phân biệt chữ hoa/thường.

Khi chương trình đang chạy, nhập `net listen group1` để tham gia thêm nhóm, hoặc `net listen group1 group2` để thêm nhiều nhóm. Các nhóm đã tham gia vẫn được giữ; nhập lại cùng nhóm không gây lỗi.

Rời nhóm bằng `net outgroup group1`, hoặc `net outgroup group1 group2` để rời nhiều nhóm. Sau khi rời, máy không hiển thị tin gửi tới nhóm đó; vẫn nhận tin tới IP, `*` và các nhóm còn lại.

Cũng có thể chạy trực tiếp `java Net.java` nếu dùng JDK 11 trở lên, rồi nhập `net listen group1` và `net send group1 Xin chao` trong chương trình.

## Ghi nhớ

- Máy khác cũng phải có mã nguồn, biên dịch và chạy chương trình mới nhận được tin.
- Nếu không nhận: kiểm tra cùng LAN, đúng IP/nhóm và firewall cho phép UDP cổng **50000**. Wi-Fi có thể chặn multicast hoặc cô lập các máy.
- Nếu không tìm thấy `java`/`javac`, kiểm tra cài đặt JDK và PATH.
- `Da gui` chỉ xác nhận thao tác gửi, chưa xác nhận máy đích nhận được.
- `Scanner` đọc lệnh liên tục; luồng riêng chờ nhận tin. IP dùng unicast, nhóm và `*` dùng multicast.
- File biên dịch nằm trong `build/`, được bỏ qua bởi `.gitignore`.
