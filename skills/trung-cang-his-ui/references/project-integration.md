# Tích hợp vào Trung Cang HIS

Các đường dẫn dưới đây tính từ thư mục dự án có `pom.xml`. Không phụ thuộc đường dẫn tuyệt đối trên máy tạo skill.

## Nơi sửa

| Mục đích | Nguồn |
| --- | --- |
| Theme chung | `src/main/webapp/resources/css/styles.css` |
| Meta, title và CSS | `src/main/webapp/WEB-INF/fragments/head.html` |
| Navbar, người dùng và đăng xuất | `src/main/webapp/WEB-INF/fragments/header.html` |
| Menu và điều kiện quyền | `src/main/webapp/WEB-INF/fragments/sidebar.html` |
| Footer và script chung | `src/main/webapp/WEB-INF/fragments/footer.html` |
| Trang nghiệp vụ | `src/main/webapp/WEB-INF/client/` |
| Logic dùng chung, tiếp nhận và các thao tác nghiệp vụ | `src/main/webapp/resources/js/main.js` |
| Script riêng | `employees.js`, `examination-data.js`, `examination-history.js` trong cùng thư mục JS |

`head.html` hiện tải Bootstrap 5.3.3, Font Awesome 6.5.1, Inter rồi CSS dự án. `footer :: scripts` tải Bootstrap bundle trước `main.js`. Đây là phiên bản đang dùng, không phải yêu cầu nâng cấp thư viện.

Trang bắt đầu bằng `th:replace="~{fragments/head :: head('Tên trang')}"` và `fragments/header :: header`; kết thúc bằng `fragments/footer :: footer`, `fragments/footer :: scripts`, rồi script riêng nếu cần. Không nạp thêm một bản Bootstrap hoặc `main.js` nữa.

## Hợp đồng cần giữ khi chỉnh giao diện

- `main.js` chọn phần tử theo ID, chứa handler được gọi trực tiếp từ HTML và đánh dấu menu active bằng `.navbar-nav .nav-link`. Tìm bằng `rg` trước khi đổi ID/href/handler.
- `/examination` là đường dẫn menu khám bệnh; file template là `medical-examination.html`. Không suy ra URL trực tiếp từ tên file.
- Fragment menu dùng `canManageStaff`, `canReceive`, `canExamine`, `canDispense`. Trang mới cần khớp controller và cấu hình quyền đang có; việc ẩn menu không thay thế kiểm tra quyền máy chủ.
- `employees.html` có `staffCsrfToken`, `staffCsrfHeader` do Thymeleaf cấp. Giữ chúng khi sửa form; kiểm tra cơ chế cookie phiên và CSRF của endpoint trước khi nối thao tác ghi.
- Các trang dùng `showToast(msg)` cần có `liveToast` và `toastMessage`; helper này dành cho thông báo thành công. Dùng vùng `role="alert"` hoặc cơ chế lỗi phù hợp cho lỗi, không báo lỗi bằng toast thành công.
- Form tiếp nhận chính dùng `inlineReceptionForm`, `recPatientId`, `recName`, `recPhone`, `recRoom`, `receptionSubmitLabel`; danh sách dùng `receptionTableBody`, `totalQueueCount`. Đây là ví dụ các điểm liên kết, không phải danh sách đầy đủ.
- Phòng đang làm việc được lưu trong `hisRoomSelection`. Hàng chờ demo còn dùng localStorage kết hợp API; không tạo một nguồn dữ liệu khác chỉ để hiển thị giao diện mới.

## Quy tắc nghiệp vụ hiện tại ảnh hưởng UI

Chỉ áp dụng khi chỉnh HIS; kiểm tra mã hiện tại nếu yêu cầu liên quan thay đổi nghiệp vụ.

- Tiếp nhận bắt buộc họ tên, điện thoại, phòng khám; đối tượng là Dịch vụ, CCCD tùy chọn. Không khôi phục lựa chọn BHYT từ nhãn cũ trong modal.
- Hồ sơ chỉ được chỉnh ở trạng thái `waiting`; đã bắt đầu khám/xử lý chuyên môn thì khóa chỉnh sửa.
- Hàng chờ theo ngày tại `Asia/Ho_Chi_Minh`. Đổi ngày không được xóa lịch sử, đơn thuốc hoặc khoản chờ thanh toán.
- ICD và danh mục thuốc lấy từ API/database; không sao chép dữ liệu mẫu thành danh mục cố định.

## Kiểm tra theo phạm vi

Đổi template/CSS: xem các trang chịu ảnh hưởng và kiểm tra responsive, focus, navbar, modal, trạng thái rỗng/lỗi. Đổi logic hàng chờ theo ngày: chạy `node src/test/js/daily-patients.test.cjs`. Khi sửa Java hoặc tích hợp phía máy chủ: dùng `mvnw.cmd test` trên Windows hoặc `./mvnw test` ở môi trường phù hợp, với cấu hình dự án cần thiết. Không tuyên bố đã kiểm tra trình duyệt khi chỉ kiểm tra file.
