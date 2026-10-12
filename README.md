# Bài tập Multicast — net send

Chương trình Java đơn giản, không cần thư viện ngoài. Cần cài **JDK 8 trở lên** và có `java`, `javac` trong PATH.

## Build và chạy

Windows CMD:

```bat
build.bat
net listen lop1
```

Mở CMD khác để gửi:

```bat
net send 192.168.1.20 Xin chao mot may
net send lop1 Xin chao ca nhom
net send * Xin chao tat ca
```

Trong PowerShell dùng `./net.bat` thay cho `net`.

Linux/macOS:

```sh
chmod +x build.sh net.sh
./build.sh
./net.sh listen lop1
```

Mở terminal khác để gửi:

```sh
./net.sh send 192.168.1.20 Xin chao mot may
./net.sh send lop1 Xin chao ca nhom
./net.sh send '*' Xin chao tat ca
```

Trên shell cần đặt `'*'` trong dấu nháy để tránh bị thay bằng danh sách file.
Các script tự build vào thư mục `build/`; thư mục này được bỏ qua trong `.gitignore`.

## Cách hoạt động

- Mỗi máy nhận phải chạy `listen`. Chạy `listen lop1 lop2` để tham gia nhiều nhóm, hoặc `listen` để chỉ nhận tin tới IP và `*`.
- Gửi tới IPv4: UDP unicast đến máy đó, cổng 50000.
- Gửi tới nhóm: chuyển tên nhóm thành địa chỉ multicast `239.11.x.x`. Tên nhóm phân biệt chữ hoa/thường. Máy nhận kiểm tra thêm tên nhóm trong gói tin để tránh hiển thị nhầm khi trùng địa chỉ.
- Gửi tới `*`: multicast đến `239.10.0.1`, tất cả máy đang chạy chương trình nhận trong LAN đều tham gia địa chỉ này.
- Máy nhận in `[Tu IP-may-gui | dich] noi dung` ra màn hình. IP được lấy từ gói UDP nhận được.

Ví dụ hai máy cùng LAN: máy B chạy `net listen lop1`; máy A gửi lần lượt tới IP máy B, `lop1` và `*`. Máy B sẽ hiển thị cả ba tin. Nếu máy B không tham gia `lop2`, tin gửi đến `lop2` sẽ không hiển thị.

Nếu máy có nhiều card mạng, chọn bằng biến môi trường `NET_INTERFACE` (tên card được Java nhận diện):

```sh
NET_INTERFACE=eth0 ./net.sh listen lop1
NET_INTERFACE=eth0 ./net.sh send lop1 Hello
```

Windows CMD: `set NET_INTERFACE=ten-card` trước khi chạy.

Cho phép UDP cổng 50000 qua firewall. Các máy cần cùng LAN có hỗ trợ multicast; Wi-Fi bật cô lập máy khách có thể chặn tin. `*` chỉ tới máy đang chạy `listen`, không tự hiện thông báo trên máy chưa chạy chương trình. UDP không bảo đảm giao hàng; dòng “Đã gửi” chỉ xác nhận thao tác gửi, không xác nhận máy đích đã nhận. Mỗi máy nên chạy một tiến trình nhận.
