# Bài tập Multicast — chương trình net send

## Chương trình này làm gì?

Chương trình cho phép các máy trong cùng mạng LAN (ví dụ cùng Wi-Fi trong lớp) gửi tin nhắn cho nhau. Có **cả chức năng gửi và nhận** trong cùng một chương trình.

Có ba cách gửi:

| Cách gửi | Ví dụ | Ai nhận được? |
|---|---|---|
| Tới một IP | `net send 192.168.1.20 Xin chao B` | Máy có IP `192.168.1.20` đang chạy lệnh nhận |
| Tới một nhóm | `net send lop1 Xin chao nhom` | Những máy đang chạy lệnh nhận và tham gia `lop1` |
| Tới tất cả | `net send * Xin chao tat ca` | Tất cả máy đang chạy lệnh nhận trong LAN hỗ trợ multicast |

`net` trong hướng dẫn này là file `net.bat` của bài tập. Bạn chạy lệnh trong thư mục chứa file đó bằng Windows CMD. Đây không phải dịch vụ nhắn tin có sẵn của Windows.

## Chuẩn bị trên từng máy

**Bạn phải gửi chương trình cho người khác để họ chạy thì họ mới nhận được tin.** Chỉ biết IP máy đó là chưa đủ.

1. Chép các file `Net.java`, `build.bat`, `net.bat` sang mỗi máy Windows. Với Linux/macOS, chép `Net.java`, `build.sh`, `net.sh`.
2. Mỗi máy cài **JDK 8 trở lên**, có lệnh `java` và `javac` trong PATH. Kiểm tra bằng `java -version` và `javac -version`.
3. Kết nối các máy vào cùng mạng LAN.
4. Mở CMD hoặc terminal tại thư mục chứa các file vừa chép.

Không cần gửi thư mục `build/`: script sẽ tự tạo thư mục này và biên dịch `Net.java` thành file `.class`. `.gitignore` bỏ qua thư mục build khi đưa mã nguồn lên Git.

## Ví dụ với ba máy

Giả sử có ba máy như sau. **Hãy thay IP ví dụ bằng IP thật của máy bạn.** Trên Windows, dùng `ipconfig` để xem địa chỉ IPv4 của card Wi-Fi/Ethernet đang kết nối.

| Máy | IP ví dụ | Nhóm sẽ tham gia |
|---|---|---|
| A | `192.168.1.10` | `lop1` |
| B | `192.168.1.20` | `lop1` |
| C | `192.168.1.30` | `lop2` |

Tên nhóm như `lop1` do bạn tự đặt, không phải tên Wi-Fi. Các máy tham gia nhóm bằng lệnh `listen`. Tên nhóm phân biệt chữ hoa/thường, chỉ dùng chữ, số, dấu `_` hoặc `-` và không có khoảng trắng.

### Cách 1: Gửi riêng cho một máy bằng IP

Trên **máy B**, mở CMD và chạy:

```bat
net listen
```

Giữ cửa sổ này mở để chờ tin. Trên **máy A**, chạy:

```bat
net send 192.168.1.20 Xin chao B
```

Máy B hiển thị:

```text
[Tu 192.168.1.10 | IP] Xin chao B
```

Chỉ máy B nhận được tin này. Đây là **unicast**: gửi từ một máy tới một máy. Máy nhận không cần tham gia nhóm.

### Cách 2: Gửi tới một nhóm

Nếu đang chạy lệnh nhận ở ví dụ trước, nhấn `Ctrl+C` để dừng trước khi chạy lệnh nhận mới.

Trên **máy A và máy B**, mỗi máy mở một cửa sổ CMD và chạy:

```bat
net listen lop1
```

Trên **máy C**, chạy:

```bat
net listen lop2
```

Trên **máy A**, mở thêm một cửa sổ CMD để gửi:

```bat
net send lop1 Chieu nay hoc luc 2 gio
```

Cửa sổ nhận của A và B hiển thị:

```text
[Tu 192.168.1.10 | lop1] Chieu nay hoc luc 2 gio
```

Máy C không hiển thị tin vì chỉ tham gia `lop2`. Đây là **multicast**: gửi tới các máy tham gia cùng nhóm. Máy A cũng nhận được vì A đang nghe nhóm `lop1`.

### Cách 3: Gửi tới tất cả máy

Giữ các cửa sổ nhận của A, B và C ở ví dụ trên đang chạy. Trên máy A, gửi:

```bat
net send * Thong bao cho ca mang
```

Cả A, B và C đều hiển thị:

```text
[Tu 192.168.1.10 | *] Thong bao cho ca mang
```

Dù tham gia nhóm khác nhau, các máy đều nhận tin `*`. Máy chưa chạy `listen` sẽ không nhận được thông báo.

Trong bài này, chức năng `*` dùng một địa chỉ multicast chung mà mọi máy chạy `listen` đều tham gia.

## Làm sao vừa gửi vừa nhận?

Mỗi máy mở **hai cửa sổ** tại thư mục chương trình:

- Cửa sổ thứ nhất chạy `net listen lop1` và giữ mở để nhận liên tục.
- Cửa sổ thứ hai dùng để gõ các lệnh `net send ...`.

Ví dụ B muốn gửi lại cho A: A giữ cửa sổ `net listen lop1` đang chạy, B mở cửa sổ khác và gõ:

```bat
net send 192.168.1.10 Xin chao A
```

Lệnh `listen lop1` nhận được cả tin gửi tới IP của máy, tới nhóm `lop1` và tới `*`.

Muốn tham gia nhiều nhóm:

```bat
net listen lop1 lop2
```

Muốn chỉ nhận tin tới IP và `*`:

```bat
net listen
```

Mỗi máy chỉ nên chạy một cửa sổ nhận. Nhấn `Ctrl+C` trong cửa sổ đó để dừng nhận.

## Chạy bằng PowerShell hoặc Linux/macOS

Các ví dụ phía trên dùng **Windows CMD**. Nếu dùng PowerShell, thay `net` bằng `./net.bat`:

```powershell
./net.bat listen lop1
./net.bat send 192.168.1.20 Xin chao B
./net.bat send lop1 Xin chao nhom
./net.bat send '*' Xin chao tat ca
```

Với Linux/macOS, cấp quyền chạy một lần:

```sh
chmod +x build.sh net.sh
```

Sau đó dùng các lệnh tương ứng bên dưới. Lệnh nhận chạy ở một terminal, lệnh gửi chạy ở terminal khác:

```sh
./net.sh listen lop1
./net.sh send 192.168.1.20 Xin chao B
./net.sh send lop1 Xin chao nhom
./net.sh send '*' Xin chao tat ca
```

Trên Linux/macOS cần đặt `'*'` trong dấu nháy để shell không thay nó bằng danh sách file.

Các script `net.bat` và `net.sh` tự build trước khi chạy. Nếu chỉ muốn build, chạy `build.bat` trên Windows hoặc `./build.sh` trên Linux/macOS.

## Nếu không nhận được tin

Kiểm tra lần lượt:

1. Máy nhận đã chạy `listen` và vẫn giữ cửa sổ đó mở chưa?
2. Hai máy có cùng mạng LAN và IP đích có đúng không?
3. Nếu gửi nhóm, máy nhận đã tham gia đúng tên nhóm chưa?
4. Firewall có cho phép Java nhận UDP cổng **50000** không?
5. Mạng Wi-Fi có chặn multicast hoặc bật cô lập các máy với nhau không?

Dòng `Da gui` chỉ cho biết chương trình đã gửi gói tin, không xác nhận máy đích đã nhận. UDP không bảo đảm tin luôn tới nơi.

Nếu máy có nhiều card mạng, chương trình tự chọn một card đang hoạt động hỗ trợ multicast. Bạn có thể chọn bằng biến môi trường `NET_INTERFACE`, dùng tên card mà Java nhận diện. Khi chạy `listen`, chương trình in tên card đang dùng.

Ví dụ Linux:

```sh
NET_INTERFACE=eth0 ./net.sh listen lop1
NET_INTERFACE=eth0 ./net.sh send lop1 Hello
```

Windows CMD: chạy `set NET_INTERFACE=ten-card` trước lệnh nhận hoặc gửi trong cửa sổ tương ứng.

## Các file và nguyên lý để trình bày bài

| File | Vai trò |
|---|---|
| `Net.java` | Mã nguồn gửi và nhận tin, có comment giải thích |
| `build.bat`, `build.sh` | Biên dịch mã nguồn vào thư mục `build/` |
| `net.bat`, `net.sh` | Build rồi chạy chương trình với tham số bạn nhập |
| `.gitignore` | Không đưa file đã biên dịch lên Git |

Chương trình dùng UDP cổng `50000`. Gửi tới IP dùng unicast; gửi tới nhóm dùng địa chỉ multicast dạng `239.11.x.x` được tính từ tên nhóm; gửi tới `*` dùng địa chỉ `239.10.0.1`. Gói tin chứa tên đích và nội dung UTF-8. Máy nhận kiểm tra tên đích rồi in nội dung cùng IP lấy từ gói UDP nhận được.
