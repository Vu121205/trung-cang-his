# Trung Cang HIS — Workflow Phát Triển

> **Phần mềm quản lý khám chữa bệnh ngoại trú cho Phòng khám Trung Cang**
> Đồ án sinh viên · Quy mô: 3 phòng (Răng Hàm Mặt, Mắt, Tai Mũi Họng)

---

## MỤC LỤC

- [PHASE 0 — Audit Project Hiện Tại](#phase-0--audit-project-hiện-tại)
- [PHASE 1 — Hoàn Thiện Tiếp Nhận Bệnh Nhân](#phase-1--hoàn-thiện-tiếp-nhận-bệnh-nhân)
- [PHASE 2 — Hoàn Thiện Visit & Hàng Chờ Khám](#phase-2--hoàn-thiện-visit--hàng-chờ-khám)
- [PHASE 3 — Hoàn Thiện Khám Bệnh](#phase-3--hoàn-thiện-khám-bệnh)
- [PHASE 4 — Hoàn Thiện Chỉ Định Dịch Vụ / Cận Lâm Sàng](#phase-4--hoàn-thiện-chỉ-định-dịch-vụ--cận-lâm-sàng)
- [PHASE 5 — Hoàn Thiện Kê Đơn Thuốc](#phase-5--hoàn-thiện-kê-đơn-thuốc)
- [PHASE 6 — Hoàn Thiện Thanh Toán](#phase-6--hoàn-thiện-thanh-toán)
- [PHASE 7 — Hoàn Thiện Cấp Thuốc & Tồn Kho](#phase-7--hoàn-thiện-cấp-thuốc--tồn-kho)
- [PHASE 8 — Hoàn Thiện Lịch Sử Khám Bệnh](#phase-8--hoàn-thiện-lịch-sử-khám-bệnh)
- [PHASE 9 — Hoàn Thiện Phân Quyền & Bảo Mật](#phase-9--hoàn-thiện-phân-quyền--bảo-mật)
- [PHASE 10 — Validation & Exception Handling](#phase-10--validation--exception-handling)
- [PHASE 11 — Testing Toàn Bộ Workflow](#phase-11--testing-toàn-bộ-workflow)
- [PHASE 12 — Hoàn Thiện UI/UX](#phase-12--hoàn-thiện-uiux)
- [PHASE 13 — Chuẩn Bị Deployment](#phase-13--chuẩn-bị-deployment)
- [PHASE 14 — Chuẩn Bị Demo & Báo Cáo Đồ Án](#phase-14--chuẩn-bị-demo--báo-cáo-đồ-án)
- [NGUYÊN TẮC THỰC HIỆN](#nguyên-tắc-thực-hiện)
- [PROJECT COMPLETION CHECKLIST](#project-completion-checklist)

---

## PHASE 0 — Audit Project Hiện Tại

### Mục tiêu

Xác định chính xác trạng thái của từng chức năng: cái gì chạy bằng database, cái gì vẫn phụ thuộc localStorage/mock data, cái gì mới chỉ có khung.

### Kết quả phân tích (Đã hoàn thành)

#### A. Chức năng ĐÃ HOÀN THÀNH (Backend + Frontend + Database)

| # | Chức năng | Backend | Frontend | Database |
|---|-----------|---------|----------|----------|
| 1 | Đăng nhập / Đăng xuất | ✅ `AuthController` | ✅ `login.html` | ✅ `users` table |
| 2 | Phân quyền 4 role | ✅ `SecurityConfig` | ✅ `sidebar.html` | ✅ `roles` table |
| 3 | Quản lý nhân viên | ✅ `EmployeeService` | ✅ `employees.html` | ✅ `users` table |
| 4 | Quản lý bệnh nhân | ✅ `PatientServiceImpl`| ✅ Trang tiếp nhận | ✅ `patients` table |
| 5 | Thiết lập phòng khám | ✅ `ExaminationRoomController` | ✅ `rooms.html` | ✅ `examination_rooms` |
| 6 | Hoàn tất khám | ✅ `ExaminationHistoryService` | ✅ `examination-data.js` | ✅ Visit/Examination/Prescription |
| 7 | Lịch sử khám bệnh | ✅ `ExaminationHistoryController`| ✅ `examination-history.html` | ✅ Query tables |
| 8 | Thanh toán | ✅ `WorkflowService.pay()` | ✅ `payment.html` | ✅ `invoices` |
| 9 | Cấp thuốc | ✅ `WorkflowService.dispense()`| ✅ `pharmacy.html` | ✅ `medicine_batches` |
| 10| Danh mục ICD/Thuốc | ✅ DB Scripts | ✅ Gợi ý JS | ✅ `diagnoses`/`medicines` |

#### B. Chức năng CÒN DÙNG localStorage (Cần chuyển sang Database)

- **Hàng chờ tiếp nhận**: `localStorage.hisPatientFlow` lưu trạng thái waiting/examining.
- **Hàng chờ khám bệnh**: Đọc từ `hisPatientFlow`.
- **Phân phòng khám khi tiếp nhận**: Lưu trong `hisPatientFlow.room` thay vì DB.

#### C. Chức năng MỚI CHỈ CÓ KHUNG (Chưa nghiệp vụ)

- Chỉ định/Kết quả CLS (ServiceOrder/ServiceResult)
- Quản lý dịch vụ y tế (MedicalService)

#### Sơ đồ rủi ro hiện tại

- Hàng chờ phụ thuộc localStorage có thể mất dữ liệu và không đồng bộ giữa nhiều máy.
- Visit chỉ tạo khi hoàn tất khám (nên tạo ngay khi tiếp nhận).

---

## PHASE 1 — Hoàn Thiện Tiếp Nhận Bệnh Nhân

### Mục tiêu
Khi lễ tân tiếp nhận bệnh nhân, hệ thống phải tạo **Visit** (lượt khám) ngay trong database thay vì lưu localStorage.

### Công việc cần thực hiện
- [ ] **TC-P01-T01**: Tạo API endpoint `POST /api/reception/admit` nhận `{patientId, roomId, reason}` → tạo `Visit` với `status=WAITING`.
- [ ] **TC-P01-T02**: Sinh `patientCode` tuần tự bên backend trong `PatientServiceImpl.create()`.
- [ ] **TC-P01-T03**: Tạo API `GET /api/reception/today?roomId=` trả danh sách Visit trong ngày.
- [ ] **TC-P01-T04**: Sửa `reception.html` + `main.js` gọi `POST /api/reception/admit`. Bỏ localStorage.
- [ ] **TC-P01-T05**: Sửa `renderDailyReceptionQueue()` load từ API.
- [ ] **TC-P01-T06**: API `PUT /api/reception/{visitId}` sửa thông tin / đổi phòng khi WAITING.
- [ ] **TC-P01-T07**: Viết test cho luồng tiếp nhận.

---

## PHASE 2 — Hoàn Thiện Visit & Hàng Chờ Khám

### Mục tiêu
Hàng chờ khám bệnh trên trang bác sĩ phải đọc từ database (`visits`).

### Công việc cần thực hiện
- [ ] **TC-P02-T01**: Tạo API `GET /api/visits/queue?roomId=&date=` cho hàng chờ.
- [ ] **TC-P02-T02**: Tạo API `PUT /api/visits/{id}/start` → đổi Visit status thành `EXAMINING`.
- [ ] **TC-P02-T03**: Sửa `loadExaminationQueue()` trong `main.js` dùng API.
- [ ] **TC-P02-T04**: Sửa `selectExaminationPatient()` gọi `PUT /api/visits/{id}/start`.
- [ ] **TC-P02-T05**: Sửa `ExaminationHistoryService.complete()` để cập nhật Visit hiện có thay vì tạo mới.
- [ ] **TC-P02-T06**: Viết test luồng queue.

---

## PHASE 3 — Hoàn Thiện Khám Bệnh

### Mục tiêu
Đảm bảo luồng khám hoạt động xuyên suốt trên database.

### Công việc cần thực hiện
- [ ] **TC-P03-T01**: Sửa `persistExamination()` truyền `visitId`.
- [ ] **TC-P03-T02**: Sửa `ExaminationHistoryService.complete()` cập nhật Visit.status sang `WAITING_PAYMENT`.
- [ ] **TC-P03-T03**: Xem xét hỗ trợ chẩn đoán phụ (tùy chọn).
- [ ] **TC-P03-T04**: Viết test end-to-end cho luồng khám.

---

## PHASE 4 — Hoàn Thiện Chỉ Định Dịch Vụ / Cận Lâm Sàng

### Mục tiêu
Chỉ định dịch vụ CLS, ghi kết quả, tính phí vào hóa đơn.

### Công việc cần thực hiện
- [ ] **TC-P04-T01**: Import danh mục dịch vụ CLS (SQL).
- [ ] **TC-P04-T02**: Sửa UI "Chỉ định CLS" gọi API danh mục.
- [ ] **TC-P04-T03**: Tạo API `POST /api/service-orders/create`.
- [ ] **TC-P04-T04**: Tạo API `POST /api/service-results/{orderId}/complete`.
- [ ] **TC-P04-T05**: Sửa `WorkflowService.billingLines()` thêm phí CLS.
- [ ] **TC-P04-T06**: Viết test CLS.

---

## PHASE 5 — Hoàn Thiện Kê Đơn Thuốc

### Mục tiêu
Kê đơn thuốc hoàn chỉnh, tương tác giá, in đơn.

### Công việc cần thực hiện
- [ ] **TC-P05-T01**: Cảnh báo kê thuốc `price = 0`.
- [ ] **TC-P05-T02**: Hiển thị giá đơn vị + thành tiền thuốc.
- [ ] **TC-P05-T03**: Tính năng xem đơn thuốc đã kê.
- [ ] **TC-P05-T04**: Giao diện in đơn thuốc.
- [ ] **TC-P05-T05**: Viết test kê thuốc.

---

## PHASE 6 — Hoàn Thiện Thanh Toán

### Mục tiêu
Thanh toán hoàn chỉnh, in hóa đơn, chuyển status.

### Công việc cần thực hiện
- [ ] **TC-P06-T01**: Tính thêm chi phí CLS.
- [ ] **TC-P06-T02**: Thêm nút "In hóa đơn".
- [ ] **TC-P06-T03**: Xử lý logic thanh toán xong → Visit.status = WAITING_PHARMACY / COMPLETED.
- [ ] **TC-P06-T04**: Viết test thanh toán.

---

## PHASE 7 — Hoàn Thiện Cấp Thuốc & Tồn Kho

### Mục tiêu
Cấp thuốc trừ tồn, UI nhập kho.

### Công việc cần thực hiện
- [ ] **TC-P07-T01**: UI quản lý lô thuốc (nhập tồn kho).
- [ ] **TC-P07-T02**: API `POST /api/medicine-batches`.
- [ ] **TC-P07-T03**: Cảnh báo thuốc sắp hết/hết hạn.
- [ ] **TC-P07-T04**: Hoàn tất lượt khám (Visit.status = COMPLETED).
- [ ] **TC-P07-T05**: Viết test cấp thuốc.

---

## PHASE 8 — Hoàn Thiện Lịch Sử Khám Bệnh

### Mục tiêu
Hiển thị đầy đủ thông tin vào lịch sử.

### Công việc cần thực hiện
- [ ] **TC-P08-T01**: Hiển thị kết quả CLS.
- [ ] **TC-P08-T02**: Hiển thị hóa đơn/thanh toán.
- [ ] **TC-P08-T03**: Hiển thị sinh hiệu đầy đủ.
- [ ] **TC-P08-T04**: In phiếu khám từ lịch sử.
- [ ] **TC-P08-T05**: Viết test lịch sử.

---

## PHASE 9 — Hoàn Thiện Phân Quyền & Bảo Mật

### Mục tiêu
Bảo mật các API mới.

### Công việc cần thực hiện
- [ ] **TC-P09-T01**: Rà soát `SecurityConfig` cho API mới.
- [ ] **TC-P09-T02**: Bật CSRF cho các mutation.
- [ ] **TC-P09-T03**: Cập nhật `AccessControlTests`.

---

## PHASE 10 — Validation & Exception Handling

### Mục tiêu
Validation backend cho API mới.

### Công việc cần thực hiện
- [ ] **TC-P10-T01**: Validation cho `PatientController`.
- [ ] **TC-P10-T02**: Validation cho Reception, MedicineBatch.
- [ ] **TC-P10-T03**: Rà soát message tiếng Việt.
- [ ] **TC-P10-T04**: Viết test validation.

---

## PHASE 11 — Testing Toàn Bộ Workflow

### Công việc cần thực hiện
- [ ] **TC-P11-T01**: Viết `WorkflowIntegrationTests`.
- [ ] **TC-P11-T02**: Test edge cases (không có thuốc).
- [ ] **TC-P11-T03**: Đảm bảo `mvnw.cmd test` pass toàn bộ.

---

## PHASE 12-14 — UI/UX, Deployment, Demo Đồ Án

### Công việc cần thực hiện
- [ ] **TC-P12-T01**: Thêm loading spinner, responsive.
- [ ] **TC-P13-T01**: Deploy production (Render), setup env vars.
- [ ] **TC-P14-T01**: Dữ liệu demo hoàn chỉnh, ERD, Use Case Diagram.

---

## NGUYÊN TẮC THỰC HIỆN
- Chỉ thực hiện MỘT task mỗi lần.
- Không thay đổi kiến trúc/database tùy tiện.
- Validation luôn ở backend.
- Đảm bảo Build/Test thành công sau mỗi Phase.
- Mọi thay đổi chức năng hoặc giao diện phải cập nhật ít nhất một tài liệu Markdown liên quan trong cùng thay đổi.

## Cập nhật 30/09/2026 — Kho thuốc và vật tư

- Kho được phân loại `MEDICINE` (thuốc) và `SUPPLY` (vật tư); dữ liệu chưa phân loại được mặc định là thuốc.
- Chức năng **Quản lý kho** lọc riêng Kho thuốc/Kho vật tư. Đơn vị lấy từ danh mục hàng hóa; tồn khả dụng và hạn gần nhất được tính cùng từ các lô còn số lượng, chưa hết hạn.
- Bác sĩ có thể chỉ định vật tư và số lượng trong hồ sơ khám. Vật tư được lưu cùng phiếu cấp phát, thanh toán và bị trừ lô khi dược xác nhận cấp phát.
- Trang Login và Thiết lập phòng không dùng footer; header dùng thương hiệu TrungCang HIS.
- Ngày 30/09/2026: nhập cột `DVT` từ `KhoaKhamBenh(cũ).xls` vào `medicines.unit` theo khóa `MAVATTU` = `medicines.code`; cập nhật 125 mã và kiểm tra sau import không còn đơn vị lỗi `??n v?`.
- Ngày 30/09/2026: đã tách 125 mã từ file nguồn theo nghiệp vụ thực tế: 121 mã thành `SUPPLY` và 4 mã thuốc/dung dịch tiêm thành `MEDICINE`. Bộ lọc Kho thuốc/Kho vật tư tại trang Quản lý kho dùng trực tiếp trường này.
