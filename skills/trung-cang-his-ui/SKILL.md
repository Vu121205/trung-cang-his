---
name: trung-cang-his-ui
description: Tạo hoặc chỉnh sửa giao diện theo phong cách Trung Cang HIS, gồm bộ màu, bố cục nghiệp vụ, biểu mẫu, bảng và trạng thái tiếng Việt. Dùng khi mở rộng giao diện dự án Trung Cang HIS hoặc khi được yêu cầu tái sử dụng phong cách này trong dự án khác; không tự áp dụng cho mọi ứng dụng y tế.
---

# Trung Cang HIS UI

Tái sử dụng giao diện làm việc của Trung Cang HIS: thanh điều hướng ngang xanh navy, nền xám nhạt, thẻ trắng, thông tin dày vừa phải, biểu mẫu và bảng phục vụ thao tác nghiệp vụ bằng tiếng Việt.

## Cách áp dụng

1. Xác định đang sửa dự án HIS hay mang phong cách sang dự án khác. Trong HIS, tìm thư mục chứa `pom.xml` và `src/main/webapp`; thư mục này có thể nằm bên trong workspace một cấp.
2. Đọc [quy tắc thiết kế](references/design-system.md). Khi sửa HIS, đọc thêm [tích hợp dự án](references/project-integration.md) và trang hiện có gần nhất với yêu cầu.
3. Trong HIS, tái sử dụng CSS và Thymeleaf fragments đang có. Khi khởi tạo trang, dùng [mẫu trang Thymeleaf](assets/page.html), đổi tên trang, ID và nối dữ liệu phù hợp. Mẫu này cần backend cùng các fragment của HIS để render; không phải trang HTML độc lập.
4. Khi dùng ở dự án khác, giữ framework và hệ thống routing của dự án đích; chuyển bộ màu, spacing, card, bảng và trạng thái sang cách triển khai sẵn có. [styles.css](assets/styles.css) là bản chụp CSS giao diện HIS ngày 2026-09-29, dùng làm nguồn tham khảo hoặc sao chép vào dự án Bootstrap mới. Không ghi đè stylesheet hiện có bằng bản chụp.
5. Kiểm tra trang ở màn hình hẹp và rộng, trạng thái dữ liệu và các thao tác đã nối. Báo rõ phần đã kiểm tra và phần chưa chạy được.

## Quy tắc quyết định

- Giữ thanh điều hướng ngang; file `sidebar.html` hiện chứa menu navbar, không phải sidebar dọc.
- Dùng Bootstrap 5 và Font Awesome đã có trong HIS. Không thêm React, Tailwind hoặc một bộ component khác chỉ để tạo một màn hình.
- Giữ nội dung tiếng Việt có dấu; nút nói rõ hành động, trạng thái có nhãn chữ kèm màu.
- Giữ ID, handler, `data-*`, fragment và điều kiện quyền khi chỉnh giao diện hiện có; kiểm tra nơi JavaScript sử dụng trước khi đổi chúng.
- Tách phong cách khỏi nghiệp vụ. Không mang API, tài khoản, dữ liệu bệnh nhân, quy tắc phân quyền hay localStorage của HIS sang ứng dụng khác chỉ vì dùng chung giao diện.
- CSS gốc có các selector toàn cục (`body`, `.card`, placeholder và `@media print`). Trong ứng dụng khác, scope hoặc điều chỉnh chúng theo bố cục đích. Quy tắc in chỉ dành cho trang có `#printableArea`.
- Không tự biến mọi nút Bootstrap thành màu `--med-primary`: CSS hiện tại chỉ dùng biến này cho một số thành phần, còn `btn-primary`, `bg-primary`, `text-primary` giữ màu Bootstrap.

## Kiểm tra kết quả

- Navbar thu gọn được, lưới xếp chồng hợp lý, bảng rộng cuộn bên trong `.table-responsive`.
- Input có label liên kết bằng `for`/`id`; trường bắt buộc có `required` và dấu sao; nút chỉ có icon có tên truy cập.
- Thông báo tải, danh sách rỗng, lỗi và thành công phù hợp dữ liệu thực tế; không hiển thị thành công trước khi thao tác lưu hoàn tất.
- Các trang dùng chung CSS vẫn giữ bố cục. Kiểm tra vùng in nếu sửa CSS in hoặc thanh toán.
- Chỉ chạy kiểm thử liên quan đến phần thay đổi. Việc dùng skill cho thay đổi giao diện không mặc nhiên yêu cầu sửa backend.
