# Quy tắc thiết kế

Nguồn: `src/main/webapp/resources/css/styles.css`, các fragment và trang trong `WEB-INF/client`, chụp ngày 2026-09-29. Khi sửa HIS, mã hiện tại trong dự án là nguồn ưu tiên nếu có khác biệt với bản chụp.

## Màu và chữ

| Thành phần | Giá trị hiện tại |
| --- | --- |
| Font | Inter, system-ui, -apple-system, sans-serif |
| Cỡ chữ body | `0.925rem` |
| Chữ nội dung | `#334155` |
| Nền trang | `#f1f5f9` |
| `--med-primary` | `#0284c7` |
| `--med-primary-dark` | `#0369a1` |
| `--med-navy` / tiêu đề | `#0f172a` |
| `--med-surface` | `#f8fafc` |
| `--med-card-border` | `#e2e8f0` |
| Placeholder | `0.78rem`, `#94a3b8`; opacity về 0 khi focus |

Các biến `--med-*` không ghi đè toàn bộ màu primary Bootstrap. Giữ sự phân biệt này khi tái tạo giao diện. Font Inter và các thư viện đang được tải từ CDN; nếu dự án đích dùng tài nguyên nội bộ, dùng cơ chế tải tương ứng.

## Bố cục

- Khung chính: `container-fluid px-3 px-lg-4 py-3`.
- Navbar: `navbar navbar-expand-lg navbar-dark med-navbar sticky-top`. Nền navy, chữ trắng; menu thường `#94a3b8`, menu active xanh `--med-primary`, bo `0.5rem`.
- Đầu trang: tiêu đề đậm, icon ở trái `text-primary me-2`, hành động ở phải; dùng `flex-wrap gap-2` khi có nhiều nút.
- Lưới: `row g-3`. Tiếp nhận và cấp thuốc dùng hai cột `col-lg-5` / `col-lg-7`; thanh toán dùng `col-lg-4` / `col-lg-8`. Chọn tỷ lệ theo nội dung, không bắt buộc mọi trang có hai cột.
- Card: nền trắng, viền 1px, bo `0.75rem`, bóng `0 1px 3px rgba(0,0,0,0.05)`. Header padding `0.85rem 1.25rem`, weight 600. Body thường `p-3`.
- Khám bệnh dùng các card xếp dọc, liên kết tới hàng chờ, bệnh án, chỉ định; không ép về bố cục form/danh sách của tiếp nhận.

## Biểu mẫu và thao tác

- Form nhiều trường: `row g-2` hoặc `g-3`, label `form-label small fw-semibold mb-1`, input `form-control form-control-sm`, select `form-select form-select-sm`.
- Dấu bắt buộc `.required-mark`: `#dc3545`, weight 700. Đồng thời đặt `required` đúng theo nghiệp vụ; không chỉ đánh dấu bằng màu.
- Nút chính `btn btn-primary`, nút phụ `btn-outline-primary` hoặc `btn-outline-secondary`; các thao tác trong bảng thường dùng `btn-sm`.
- Một số trang cũ có `btn-xs` và `extra-small`, nhưng CSS gốc không định nghĩa chúng. Với phần mới, dùng `btn-sm` và `small`, hoặc thêm class có định nghĩa rõ ràng khi cần.
- Giữ placeholder ngắn và giữ label nhìn thấy khi focus. Không dùng placeholder thay label.
- Modal dùng Bootstrap `.modal.fade`, `.modal-dialog`, `.modal-content`, header/body/footer và `data-bs-dismiss="modal"`. Gắn `aria-labelledby` vào tiêu đề và tên tiếng Việt cho nút đóng.

## Bảng và trạng thái

Bao bảng bằng `.table-responsive`; mẫu phổ biến là `table table-hover table-custom align-middle mb-0`. Header `.table-custom th`: nền `#f8fafc`, chữ `#475569`, weight 600, uppercase, cỡ `0.75rem`, tracking `0.5px`. Cột thao tác/số tiền canh phải khi phù hợp.

| Trạng thái | Class | Nền | Chữ |
| --- | --- | --- | --- |
| Chờ khám | `badge-status-waiting` | `#fef3c7` | `#d97706` |
| Đang khám | `badge-status-examining` | `#e0f2fe` | `#0369a1` |
| Hoàn tất | `badge-status-done` | `#dcfce7` | `#15803d` |

Kết hợp với `.badge`. Đây là tên trình bày, không phải hợp đồng giá trị trạng thái API; kiểm tra dữ liệu thực tế trước khi ánh xạ.

Loading/empty/error đặt trong vùng trạng thái hoặc hàng `td colspan` đủ số cột. Đếm bản ghi từ dữ liệu, không sao chép các con số demo. Dữ liệu do người dùng nhập nên được render bằng `textContent` hoặc cơ chế escape của template.

## Thành phần chuyên biệt

- `.vital-card`: nền `#f8fafc`, viền nét đứt `#cbd5e1`, bo `0.5rem`, padding `0.6rem`, canh giữa; `.vital-val` cỡ `1.15rem`, weight 700.
- Toast ở góc dưới phải: `position-fixed bottom-0 end-0 p-3 toast-container`, z-index 1090. Trong HIS, helper `showToast` phụ thuộc ID `liveToast` và `toastMessage`.
- Login: `body.login-page` căn giữa theo chiều dọc, gradient `#f8fafc` đến `#e2e8f0`; `.login-card` rộng 420px, tối đa 92vw, bo 1rem.
- In: CSS gốc ẩn mọi phần tử và chỉ hiện `#printableArea`; chỉ dùng cho luồng có vùng in này, kiểm tra print preview sau khi sửa.
- `.page-section` luôn hiển thị, còn `.section-view` mặc định ẩn và chỉ hiện khi có `.active`. Không thay thế lẫn nhau khi chưa kiểm tra hành vi.
