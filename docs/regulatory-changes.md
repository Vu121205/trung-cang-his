# Ghi nhận thay đổi theo quy định y tế

Ngày rà soát: 05/10/2026

## Phạm vi và giới hạn

Project được điều chỉnh theo mô hình **phần mềm hỗ trợ quản lý hồ sơ giấy**. Nhân viên y tế vẫn in biểu mẫu và ký tay trên bản giấy. Project không triển khai chữ ký số, chữ ký điện tử, xác nhận điện tử hoặc tuyên bố dữ liệu trong hệ thống có giá trị thay thế hồ sơ bệnh án giấy.

Do đó, các thay đổi dưới đây là biện pháp kiểm soát phần mềm hỗ trợ quy trình giấy; chúng không tự làm cho project trở thành hệ thống hồ sơ bệnh án điện tử đáp ứng đầy đủ Thông tư 13/2025/TT-BYT.

## Văn bản và điều khoản áp dụng

1. [Luật Khám bệnh, chữa bệnh số 15/2023/QH15](https://vanban.chinhphu.vn/?classid=1&docid=207396&pageid=27160), ban hành 09/01/2023, hiệu lực 01/01/2024. Các quy định về hồ sơ bệnh án, bảo mật thông tin người bệnh và lưu trữ hồ sơ là cơ sở để không xóa vật lý lịch sử khám và để tách hồ sơ giấy khỏi dữ liệu hỗ trợ trên phần mềm.
2. [Thông tư 32/2023/TT-BYT](https://e-services.moh.gov.vn/en/web/guest/dichvucong/-/dvc/thutuchanhchinh/0.html?_dichvucong_WAR_bytedvcportlet_cur=2&_dichvucong_WAR_bytedvcportlet_delta=75&_dichvucong_WAR_bytedvcportlet_mvcPath=%2Fhtml%2Fdvc%2Fportlet%2Fdichvucong%2Fxem_thutuc.jsp), ban hành 31/12/2023, quy định chi tiết một số điều của Luật Khám bệnh, chữa bệnh; Chương X là nguồn tham chiếu về nội dung hồ sơ bệnh án.
3. [Thông tư 13/2025/TT-BYT](https://vbpl.vn/boyte/Pages/vbpq-toanvan.aspx?ItemID=178219), ban hành 06/06/2025, hiệu lực 21/07/2025. Điều 1 định nghĩa hồ sơ bệnh án điện tử là hồ sơ được lập, cập nhật, hiển thị, ký và lưu trữ bằng phương tiện điện tử; Điều 3 yêu cầu việc ký/xác nhận điện tử trong hồ sơ điện tử; Điều 4 quy định lộ trình triển khai; Điều 5 có quy định chuyển tiếp cho hồ sơ giấy.
4. [Thông tư 26/2025/TT-BYT](https://vanban.chinhphu.vn/?classid=1&docid=214386&orggroupid=4&pageid=27160), ban hành 30/06/2025, hiệu lực 01/07/2025, quy định về đơn thuốc và kê đơn thuốc hóa dược, sinh phẩm trong điều trị ngoại trú.
5. [Thông tư 33/2025/TT-BYT](https://vanban.chinhphu.vn/?classid=1&docid=214427&orggroupid=4&pageid=27160), ban hành và có hiệu lực 01/07/2025, quy định thời hạn lưu trữ hồ sơ, tài liệu ngành y tế. Phụ lục của Thông tư có nhóm hồ sơ bệnh án nội trú, ngoại trú với thời hạn lưu trữ tương ứng.
6. [Luật Bảo vệ dữ liệu cá nhân số 91/2025/QH15](https://vanban.chinhphu.vn/?classid=1&docid=214590&orggroupid=1&pageid=27160), ban hành 26/06/2025, hiệu lực 01/01/2026; và [Nghị định 356/2025/NĐ-CP](https://vanban.chinhphu.vn/?classid=1&docid=216387&pageid=27160), ban hành 31/12/2025, hiệu lực 01/01/2026. Đây là cơ sở để áp dụng giới hạn truy cập, giảm dữ liệu lưu ở trình duyệt và lưu vết thao tác nghiệp vụ.

## Thay đổi đã thực hiện

### 1. Không xóa vật lý hồ sơ nghiệp vụ

- Xóa bệnh nhân chuyển thành `INACTIVE`, cập nhật `updatedAt`, giữ nguyên liên kết với lượt khám và lịch sử.
- Xóa đơn thuốc chuyển thành `CANCELLED`; đơn đã cấp phát không được hủy.
- Đây là xóa mềm phục vụ quản trị, không phải cơ chế tự động hủy hồ sơ theo thời hạn lưu trữ.

### 2. Trạng thái hồ sơ giấy ký tay

Thêm trạng thái cho hồ sơ khám và đơn thuốc:

```text
PENDING_PRINT -> PRINTED -> HAND_SIGNED
```

Trạng thái chỉ ghi nhận tiến độ của bản giấy ngoài hệ thống; không phải chữ ký điện tử và không tạo giá trị pháp lý của hồ sơ điện tử.

API mới:

```text
POST /api/examination-history/{id}/paper-status?status=PRINTED
POST /api/examination-history/{id}/paper-status?status=HAND_SIGNED
```

Chỉ Admin/Bác sĩ được cập nhật trạng thái này.

### 3. Audit nghiệp vụ

Thêm bảng `audit_events` để ghi nhận tối thiểu:

- Hoàn tất hồ sơ khám.
- Cập nhật trạng thái bản giấy.
- Thanh toán.
- Cấp phát thuốc.
- Ngừng hoạt động hồ sơ bệnh nhân.
- Hủy mềm đơn thuốc.

Audit chỉ lưu loại sự kiện, đối tượng, người thao tác, thời điểm và mô tả ngắn; không sao chép nội dung bệnh án vào log.

### 4. Bảo vệ workflow

- Gắn người dùng đang đăng nhập cho thao tác thanh toán và cấp phát thuốc.
- Giữ kiểm tra quyền server-side cho cập nhật trạng thái giấy.
- Không dùng trạng thái trong `localStorage` làm bằng chứng hồ sơ pháp lý.

## File bị ảnh hưởng

- `src/main/java/com/trungcang/trung_cang_his/domain/AuditEvent.java`
- `src/main/java/com/trungcang/trung_cang_his/domain/Examination.java`
- `src/main/java/com/trungcang/trung_cang_his/domain/Prescription.java`
- `src/main/java/com/trungcang/trung_cang_his/repository/AuditEventRepository.java`
- `src/main/java/com/trungcang/trung_cang_his/service/AuditService.java`
- `src/main/java/com/trungcang/trung_cang_his/service/ExaminationHistoryService.java`
- `src/main/java/com/trungcang/trung_cang_his/service/WorkflowService.java`
- `src/main/java/com/trungcang/trung_cang_his/service/impl/PatientServiceImpl.java`
- `src/main/java/com/trungcang/trung_cang_his/service/impl/PrescriptionServiceImpl.java`
- `src/main/java/com/trungcang/trung_cang_his/controller/ExaminationHistoryController.java`
- `src/main/java/com/trungcang/trung_cang_his/controller/PatientController.java`
- `src/main/java/com/trungcang/trung_cang_his/controller/PrescriptionController.java`
- `src/main/java/com/trungcang/trung_cang_his/controller/WorkflowController.java`
- `src/main/java/com/trungcang/trung_cang_his/config/SecurityConfig.java`

## Phần chưa thể khẳng định là đã tuân thủ đầy đủ

- Project chưa phải hồ sơ bệnh án điện tử theo Thông tư 13/2025/TT-BYT.
- Chưa có chữ ký số/chữ ký điện tử, kho lưu trữ hồ sơ giấy, quy trình scan/chuyển đổi, sao lưu pháp lý hoặc lịch lưu trữ tự động.
- Chưa triển khai đầy đủ biểu mẫu đơn thuốc theo toàn bộ phụ lục của Thông tư 26/2025/TT-BYT.
- Chưa có hệ thống quản trị đồng ý xử lý dữ liệu, yêu cầu của chủ thể dữ liệu và quy trình ứng phó sự cố dữ liệu.

Các nội dung trên cần được đơn vị khám chữa bệnh phê duyệt về quy trình và hạ tầng trước khi tuyên bố tuân thủ đầy đủ.

## Bổ sung mẫu đơn thuốc và bảng kê thuốc

### Căn cứ bổ sung

[Thông tư 26/2025/TT-BYT](https://datafiles.chinhphu.vn/cpp/files/vbpq/2025/7/26-byt.pdf) của Bộ Y tế, ban hành 30/06/2025 và có hiệu lực 01/07/2025. Các căn cứ triển khai lần này là:

- Điều 3: mẫu đơn thuốc được ban hành tại Phụ lục I; mẫu đơn “N” và “H” là các mẫu riêng cho thuốc gây nghiện, thuốc hướng thần/tiền chất.
- Điều 6 khoản 2–8: thông tin người bệnh, nơi cư trú, tên thuốc, nồng độ/hàm lượng, số lượng, liều mỗi lần, số lần mỗi ngày, đường dùng, thời điểm dùng, số ngày sử dụng và lời dặn.
- Điều 6 khoản 9: khi sửa hoặc điều chỉnh thuốc thì lập đơn mới thay đơn cũ.
- Phụ lục I: mã đơn thuốc, thông tin cơ sở, người bệnh, chẩn đoán, thuốc điều trị, lời dặn và phần “Bác sỹ/Y sỹ khám bệnh (Ký, ghi rõ họ tên)”.

### Thay đổi thực hiện

- Các trường `dosage`, `frequency`, `duration`, `route` của `PrescriptionDetail` được lấy từ lịch dùng thuốc trên giao diện và lưu vào database.
- Mã đơn thuốc mới dùng cấu trúc 5 ký tự mã cơ sở + 7 ký tự ngẫu nhiên + hậu tố `-C` cho đơn thuốc thông thường; không giả lập mã đơn “N” hoặc “H”.
- Thêm API `GET /api/prescriptions/{id}/print` trả dữ liệu đã chuẩn hóa cho bản in.
- Thêm nút **In đơn/bảng kê thuốc** trong chi tiết lịch sử khám.
- Bản in gồm thông tin cơ sở, mã đơn, người bệnh, định danh, BHYT, nơi cư trú, chẩn đoán, thuốc, liều, số lần/ngày, đường dùng, số ngày, số lượng, cách dùng, lời dặn và khu vực ký tay của người kê đơn.
- Đơn “N/H”, thuốc gây nghiện, thuốc hướng thần và liên thông Hệ thống đơn thuốc quốc gia chưa được triển khai vì project chưa có phân loại thuốc kiểm soát đặc biệt và vẫn hoạt động theo phạm vi bản giấy.

### File bổ sung/cập nhật

- `src/main/java/com/trungcang/trung_cang_his/service/PrescriptionService.java`
- `src/main/java/com/trungcang/trung_cang_his/service/impl/PrescriptionServiceImpl.java`
- `src/main/java/com/trungcang/trung_cang_his/controller/PrescriptionController.java`
- `src/main/java/com/trungcang/trung_cang_his/repository/PrescriptionRepository.java`
- `src/main/java/com/trungcang/trung_cang_his/service/ExaminationHistoryService.java`
- `src/main/webapp/resources/js/examination-data.js`
- `src/main/webapp/resources/js/examination-history.js`
- `src/main/webapp/WEB-INF/client/examination-history.html`
- `src/main/resources/application.properties`

### Giới hạn pháp lý cần lưu ý

Điều 13 khoản 3 của Thông tư 26/2025/TT-BYT quy định lộ trình kê đơn điện tử: bệnh viện trước 01/10/2025 và cơ sở khám chữa bệnh khác trước 01/01/2026. Vì người dùng đã xác định project vẫn ký tay, chức năng này chỉ tạo bản in hỗ trợ hồ sơ giấy; chưa thể tuyên bố đáp ứng lộ trình kê đơn điện tử hoặc liên thông quốc gia.
