# Triển khai miễn phí: Render + Aiven MySQL

Thư mục chứa `pom.xml`, `Dockerfile` và tài liệu này là thư mục gốc repository GitHub.

## 1. Tạo MySQL miễn phí

1. Đăng ký tại https://console.aiven.io/ bằng tài khoản của bạn.
2. Tạo dịch vụ **MySQL**, chọn đúng gói **Free** (không chọn bản dùng thử của gói trả phí).
3. Khi dịch vụ Running, mở Overview/Connection information và lấy host, port, username, password.
4. Sử dụng database `defaultdb`, tạo URL JDBC như sau (không đưa mật khẩu vào URL):

```text
jdbc:mysql://HOST:PORT/defaultdb?sslMode=REQUIRED&connectionTimeZone=Asia/Ho_Chi_Minh
```

`sslMode=REQUIRED` bắt buộc mã hóa đường truyền. Khi dùng dữ liệu thật, cấu hình truststore với CA của Aiven và `sslMode=VERIFY_IDENTITY` để xác minh máy chủ.

## 2. Đưa website lên Render

1. Đăng nhập https://dashboard.render.com/ bằng GitHub.
2. Chọn **New → Blueprint**, kết nối repository của dự án; cấp quyền riêng cho repository này nếu để private.
3. Render đọc `render.yaml`, tạo một web service Docker có **plan: free**.
4. Nhập các biến được yêu cầu:

| Biến | Giá trị |
| --- | --- |
| `JDBC_DATABASE_URL` | URL JDBC ở bước 1 |
| `JDBC_DATABASE_USERNAME` | Username MySQL do Aiven cấp |
| `JDBC_DATABASE_PASSWORD` | Password MySQL do Aiven cấp |
| `BOOTSTRAP_ADMIN_PASSWORD` | Mật khẩu admin tự chọn, tối thiểu 12 ký tự, tối đa 72 byte UTF-8 |

5. Deploy và chờ trạng thái **Live**. Mở URL HTTPS do Render cấp, thêm `/login`.
6. Đăng nhập bằng `admin` và mật khẩu vừa đặt; tạo nhân viên từ **Quản lý nhân viên**.

Profile `prod` chỉ tạo bốn nhóm quyền và tài khoản admin đầu tiên. Không tạo bệnh nhân hoặc nhân viên demo. Khởi động lại không ghi đè tài khoản có sẵn. Cấu hình này dành cho database mới; không tự thay mật khẩu của tài khoản đã tồn tại nếu nhập database cũ.

## 3. Danh mục nghiệp vụ

Database mới chưa có khoa/phòng, ICD, thuốc và giá khám. Trong thư mục `sql/` có các script nhập danh mục. Kiểm tra nội dung và chọn bộ dữ liệu phù hợp trước khi chạy bằng công cụ MySQL của bạn trên database Aiven. Không chạy `set_demo_inventory_1000.sql` trên dữ liệu thật. Bệnh nhân và hồ sơ trên MySQL máy cá nhân không tự chuyển lên cloud.

## 4. Cập nhật và kiểm tra

Mỗi lần push lên `main`, GitHub Actions kiểm thử JavaScript, chạy Maven verify và kiểm tra Docker build. Render được cấu hình triển khai khi các kiểm tra đạt. Các biến bí mật chỉ nhập trong phần Environment của Render; không commit `.env`, mật khẩu, token hay bản sao database.

```powershell
node src/test/js/daily-patients.test.cjs
.\mvnw.cmd verify
```

Dockerfile đặt Java 21, múi giờ Việt Nam và profile `prod`. Health check dùng `/login`. Phiên đăng nhập yêu cầu HTTPS trên bản triển khai.

## Giới hạn cần biết

- Render Free ngủ sau 15 phút không có truy cập; lần mở tiếp theo cần chờ khởi động. Gói này cấp 750 giờ miễn phí/workspace/tháng.
- Aiven MySQL Free có 1 GB lưu trữ và có thể tạm dừng dịch vụ không hoạt động theo chính sách của Aiven.
- Hàng chờ khám hiện lưu trong localStorage của từng trình duyệt; các máy khác nhau chưa đồng bộ hàng chờ. Hồ sơ đã hoàn tất được lưu trong MySQL.
- Gói miễn phí này phù hợp chạy thử và trình diễn. Không có cam kết hoạt động liên tục cho vận hành phòng khám.

Nguồn: [Render Free](https://render.com/docs/free), [Render Blueprint](https://render.com/docs/blueprint-spec), [Aiven MySQL Free](https://aiven.io/docs/products/mysql/concepts/mysql-free-tier).
