# Trung Cang HIS

Dự án này là hệ thống thông tin y tế bệnh viện (Hospital Information System – HIS) xây dựng bằng Java Spring Boot, tập trung vào quy trình khám bệnh, điều trị, thuốc và thanh toán.

## 1. Tổng quan

Project hiện đang ở trạng thái khung backend + mô hình dữ liệu + giao diện demo. Các thành phần chính đã được bổ sung gồm:

- Backend Java/Spring Boot
- JPA Entity cho mô hình bệnh viện
- Tầng repository, service, controller theo mô hình CRUD cơ bản
- Giao diện web demo bằng HTML/CSS/JavaScript
- Mô hình nghiệp vụ khám bệnh, thuốc, thanh toán

Các chức năng chính đang được định hình bao gồm:

- Tiếp nhận bệnh nhân
- Phân phòng khám
- Khám bệnh và chẩn đoán
- Dịch vụ y tế / cận lâm sàng
- Kê đơn thuốc
- Xuất thuốc / kiểm kho
- Thanh toán viện phí

## 2. Công nghệ sử dụng

- Java 21
- Spring Boot 4.1.1
- Spring Data JPA
- Spring Security
- Spring Web MVC
- Thymeleaf
- MySQL Connector
- Lombok
- Maven
- HTML, CSS, Bootstrap, JavaScript

## 3. Cấu trúc dự án

```text
trung-cang-his/
├── .mvn/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── trungcang/
│   │   │           └── trung_cang_his/
│   │   │               ├── domain/
│   │   │               ├── controller/
│   │   │               ├── repository/
│   │   │               ├── service/
│   │   │               ├── service/impl/
│   │   │               └── TrungCangHisApplication.java
│   │   ├── resources/
│   │   │   └── application.properties
│   │   └── webapp/
│   │       ├── resources/
│   │       │   ├── css/
│   │       │   └── js/
│   │       └── WEB-INF/
│   │           ├── fragments/
│   │           │   ├── head.html       # title, meta và CSS dùng chung
│   │           │   ├── header.html     # thương hiệu, người dùng, đăng xuất
│   │           │   ├── sidebar.html    # menu theo quyền (hiển thị trên navbar)
│   │           │   └── footer.html     # chân trang và JavaScript dùng chung
│   │           └── client/
│   │               ├── login.html
│   │               ├── reception.html
│   │               ├── medical-examination.html
│   │               ├── examination-history.html
│   │               ├── pharmacy.html
│   │               ├── payment.html
│   │               └── rooms.html
│   └── test/
│       └── java/
│           └── com/
│               └── trungcang/
│                   └── trung_cang_his/
│                       └── TrungCangHisApplicationTests.java
├── HELP.md
├── pom.xml
├── mvnw
├── mvnw.cmd
├── .gitignore
├── README.md
└── target/
```

## 4. Các module nghiệp vụ chính

### Quy trình tiếp nhận hiện tại

- Tài khoản thuộc nhóm Admin và Lễ Tân được phép tiếp nhận bệnh nhân qua API `/api/patients`.
- Phiên đăng nhập được gửi kèm các request API bằng cookie session.
- Đối tượng khám được cố định là **Dịch vụ**; hệ thống không hiển thị hoặc xử lý lựa chọn BHYT trong form tiếp nhận.
- Nhóm `ADMIN` và `RECEPTION` được phép tạo, sửa, xóa hồ sơ bệnh nhân; cả bốn nhóm được đọc hồ sơ phục vụ nghiệp vụ.
- Quyền được kiểm tra ở máy chủ bằng Spring Security; ẩn menu không thay thế kiểm tra quyền. Mã nhóm được lưu không có tiền tố `ROLE_` trong database.
- Form tiếp nhận không còn khối tra cứu riêng; danh sách tiếp nhận trong ngày hiển thị ngay các hồ sơ từ API. Hồ sơ có trạng thái `waiting` được phép sửa thông tin hoặc đổi phòng, còn hồ sơ đã bắt đầu khám/đã có xử lý chuyên môn thì bị khóa chỉnh sửa.
- Các trường bắt buộc trên form tiếp nhận gồm họ tên, số điện thoại và phòng khám; mỗi trường hiển thị dấu `*` màu đỏ và được kiểm tra bằng thuộc tính `required`.
- Có thể chọn trực tiếp một bệnh nhân trong bảng “Danh sách Tiếp nhận trong ngày” để nạp thông tin vào form; thao tác này chỉ được phép khi hồ sơ còn ở trạng thái `waiting`.
- Placeholder trên toàn bộ website được thu nhỏ và tự ẩn khi người dùng focus vào ô nhập liệu.
- Trang khám bệnh có tra cứu ICD-10: khi nhập mã có trong danh mục, hệ thống tự điền bệnh chính tương ứng.
- Ô kê thuốc hỗ trợ gợi ý theo tên thuốc, hoạt chất, hàm lượng hoặc dạng dùng; bác sĩ có thể chọn thuốc từ danh mục rồi nhập số lượng và cách dùng. ICD và thuốc được tải từ database qua API, không dùng danh sách cố định trong JavaScript.
- Số CCCD chỉ là thông tin nhận diện tùy chọn, không phải mã BHYT.
- Sau khi tạo hồ sơ thành công, danh sách bệnh nhân trong ngày được tải lại từ database.
- Danh sách tiếp nhận và hàng chờ khám chỉ hiển thị bệnh nhân của ngày hiện tại theo múi giờ `Asia/Ho_Chi_Minh`, tự làm mới lúc 00:00 và khi quay lại tab/máy sau thời gian nghỉ. Số thứ tự hiển thị bắt đầu lại từ 1 mỗi ngày. Ngày tiếp nhận lấy từ lượt tiếp nhận trong trình duyệt, hoặc `createdAt` của hồ sơ nếu chưa có lượt; hồ sơ cũ không có ngày không tự đưa vào hàng chờ hôm nay.
- Làm mới theo ngày không xóa hồ sơ, lịch sử khám, khoản chờ thanh toán hoặc đơn chờ cấp thuốc. Hồ sơ khám đang mở được giữ để bác sĩ hoàn tất; hàng chờ tiếp nhận/khám vẫn dùng `localStorage` theo giới hạn hiện tại của bản demo.
- Kiểm thử đổi ngày phía trình duyệt: `node src/test/js/daily-patients.test.cjs`; kiểm thử backend: `mvnw.cmd test`.

### 4.1 Quản lý bệnh nhân

File chính:

- `src/main/java/com/trungcang/trung_cang_his/domain/Patient.java`

Chứa thông tin bệnh nhân như:

- mã bệnh nhân
- họ tên
- ngày sinh
- giới tính
- SĐT
- CCCD
- BHYT
- địa chỉ
- tiền sử bệnh
- dị ứng
- trạng thái hoạt động

### 4.2 Quản lý lượt khám

File chính:

- `src/main/java/com/trungcang/trung_cang_his/domain/Visit.java`

Mô hình hóa lượt khám với:

- mã lượt khám
- bệnh nhân
- phòng khám
- bác sĩ
- ngày giờ khám
- loại khám
- hình thức thanh toán
- lý do khám
- trạng thái

### 4.3 Khám bệnh và chẩn đoán

File chính:

- `src/main/java/com/trungcang/trung_cang_his/domain/Examination.java`

Mô tả quá trình khám:

- tiền sử bệnh
- triệu chứng
- kết quả khám
- chẩn đoán
- lời khuyên
- trạng thái khám

### 4.4 Dịch vụ y tế

File chính:

- `src/main/java/com/trungcang/trung_cang_his/domain/MedicalService.java`

Quản lý các dịch vụ kỹ thuật, xét nghiệm, hình ảnh, thủ thuật, ...

### 4.5 Thuốc và đơn thuốc

File chính:

- `Medicine.java`
- `Prescription.java`
- `PrescriptionDetail.java`
- `MedicineBatch.java`

Quản lý thuốc, đơn thuốc, số lượng, liều dùng, cách dùng, đơn giá, tồn kho.

### 4.6 Hóa đơn và thanh toán

File chính:

- `Invoice.java`
- `InvoiceDetail.java`

Quản lý hóa đơn, chi tiết thanh toán, giảm giá, bảo hiểm, trạng thái thanh toán.

### 4.7 Người dùng và phân quyền

File chính:

- `User.java`
- `Role.java`
- `Department.java`
- `ExaminationRoom.java`

Quản lý nhân sự, phòng ban, vai trò, phòng khám.

## 5. Lớp controller, service, repository

Dự án đã có các lớp CRUD scaffold theo từng nghiệp vụ cơ bản, bao gồm:

- `AuthController`
- `PatientController`
- `VisitController`
- `ExaminationController`
- `ServiceOrderController`
- `ServiceResultController`
- `PrescriptionController`
- `MedicineController`
- `InvoiceController`
- `DiagnosisController`
- `MedicalServiceController`
- `DepartmentController`
- `ExaminationRoomController`

Tầng service tương ứng đã được tạo với pattern:

- `getAll()`
- `getById()`
- `create()`
- `update()`
- `delete()`

Repository thực hiện kế thừa `JpaRepository` cho các entity chính như:

- `PatientRepository`
- `VisitRepository`
- `ExaminationRepository`
- `InvoiceRepository`
- `MedicineRepository`
- `PrescriptionRepository`
- `ServiceOrderRepository`
- `ServiceResultRepository`
- `DiagnosisRepository`
- `MedicalServiceRepository`
- `DepartmentRepository`
- `ExaminationRoomRepository`

### Quản lý nhân viên (Admin)

- Menu **Quản lý nhân viên** tại `/employees` chỉ dành cho nhóm `ADMIN`; máy chủ kiểm tra quyền cho cả trang và API.
- Thêm nhân viên đồng thời tạo tài khoản; sửa họ tên, mã nhân viên, ngày sinh, giới tính, CCCD/giấy tờ, liên hệ, địa chỉ, khoa/bộ phận, chức vụ và chuyên môn.
- Phân một trong bốn nhóm quyền `ADMIN`, `DOCTOR`, `RECEPTION`, `PHARMACIST`; tìm kiếm và lọc theo nhóm quyền/trạng thái.
- Đổi mật khẩu qua thao tác riêng; mật khẩu được băm BCrypt, tối thiểu 8 ký tự và tối đa 72 byte UTF-8, không trả về trong API.
- Đổi quyền, đổi mật khẩu, khóa hoặc xóa nhân viên làm phiên cũ hết hiệu lực ở yêu cầu tiếp theo. Admin đổi mật khẩu của mình sẽ phải đăng nhập lại.
- Không cho tự xóa, tự khóa hoặc tự bỏ quyền Admin. Xóa nhân viên là xóa mềm: ẩn khỏi danh sách và chặn đăng nhập, giữ liên kết với lịch sử nghiệp vụ. Tên đăng nhập và mã nhân viên đã dùng vẫn được giữ riêng cho hồ sơ đó.
- Các thay đổi được giữ sau khi khởi động lại. Hibernate bổ sung các cột hồ sơ và trạng thái xóa/phiên vào bảng `users` khi khởi động với cấu hình `ddl-auto=update` hiện tại.
- API: `GET/POST /api/employees`, `GET /api/employees/roles`, `PUT/DELETE /api/employees/{id}`, `PUT /api/employees/{id}/password`. Các yêu cầu ghi cần CSRF token từ trang quản lý nhân viên và cookie phiên đăng nhập.

### API chính

Các resource nghiệp vụ đều hỗ trợ `GET /api/{resource}`, `GET /api/{resource}/{id}`, `POST`, `PUT /{id}` và `DELETE /{id}`:

```text
/api/patients
/api/visits
/api/examinations
/api/service-orders
/api/service-results
/api/prescriptions
/api/medicines
/api/invoices
/api/diagnoses
/api/medical-services
/api/departments
/api/examination-rooms
```

API xác thực:

- `POST /api/auth/login` với JSON `{ "username": "...", "password": "..." }`
- `GET /api/auth/me`
- `POST /api/auth/logout`

Sau khi đăng nhập, hệ thống lưu Security Context bằng session cookie. Các API bảo vệ được gọi qua session này, không dùng HTTP Basic nên trình duyệt không hiển thị hộp thoại xác thực native.

Tài khoản được seed vào database khi ứng dụng khởi động:

| Tài khoản | Họ tên  | Nhóm                   | Menu                                                      |
| --------- | ------- | ---------------------- | --------------------------------------------------------- |
| `admin`   | Admin   | Admin (`ADMIN`)        | Tất cả menu                                               |
| `bacsi`   | bác sĩ  | Bác Sĩ (`DOCTOR`)      | Thiết lập phòng, Khám bệnh, Lịch sử khám bệnh             |
| `letan`   | lễ tân  | Lễ Tân (`RECEPTION`)   | Thiết lập phòng, Tiếp nhận, Lịch sử khám bệnh             |
| `duocsi`  | dược sĩ | Dược Sĩ (`PHARMACIST`) | Thiết lập phòng, Cấp thuốc, Thanh toán, Lịch sử khám bệnh |

Mật khẩu của các tài khoản trên và tài khoản nhân sự trong `DatabaseSeeder.java`: `pkdktc68@`.

Sau đăng nhập, cả bốn nhóm đến `/rooms`. Danh sách khoa chỉ hiện các nghiệp vụ được cấp quyền; chọn phòng sẽ mở màn hình tương ứng.

`DatabaseSeeder` tự cập nhật dữ liệu nhóm/tài khoản trong transaction khi khởi động; không cần thay đổi cấu trúc bảng `roles` và `users`. Các nhóm cũ `NURSE`/`RECEPTIONIST` được chuyển sang `RECEPTION`, `ACCOUNTANT`/`CASHIER` sang `PHARMACIST`; role cũ có tiền tố `ROLE_` cũng được hợp nhất. Người dùng được chuyển nhóm trước khi xóa nhóm cũ để giữ khóa ngoại. Các nhóm khác không còn người dùng được xóa. Chạy lại không tạo tài khoản trùng. Dữ liệu bệnh nhân hiện có được giữ nguyên; chỉ tạo bệnh nhân demo khi bảng rỗng.

Quyền API theo nghiệp vụ:

- Cả bốn nhóm: đọc bệnh nhân, lượt khám, khoa, phòng và lịch sử khám bệnh.
- Admin/Lễ Tân: tạo, sửa, xóa bệnh nhân; Admin/Lễ Tân/Bác Sĩ: cập nhật lượt khám.
- Admin/Bác Sĩ: khám bệnh, chỉ định, kết quả dịch vụ, kê đơn; đọc ICD và danh mục dịch vụ.
- Admin/Bác Sĩ/Dược Sĩ: đọc thuốc và đơn thuốc.
- Admin/Dược Sĩ: quản lý thuốc và hóa đơn/thanh toán.
- Chỉnh sửa danh mục khoa, phòng, ICD và dịch vụ: Admin.

Người chưa đăng nhập được chuyển về `/login` khi mở trang nghiệp vụ; API trả HTTP 401. Người đã đăng nhập nhưng sai quyền nhận HTTP 403, kể cả nhập URL trực tiếp.

## 6. Giao diện người dùng

Dự án có các màn hình demo theo từng nghiệp vụ:

Các trang được render bằng Thymeleaf. `ClientPageController` trả tên view trong `client/`; các trang dùng `th:replace` để ghép thành phần từ `WEB-INF/fragments`. Sửa menu tại `fragments/sidebar.html` sẽ áp dụng cho tất cả trang nghiệp vụ. `sidebar.html` giữ bố cục menu ngang hiện tại.

Maven đóng gói `WEB-INF` vào `classpath:/templates/`, còn CSS/JS vào `classpath:/static/resources/`. Template không được phục vụ như tài nguyên tĩnh; truy cập trực tiếp `/WEB-INF/**` bị chặn. Sau thay đổi cấu trúc, nên chạy `mvnw.cmd clean package` để loại tài nguyên build cũ.

### 6.1 Login

- `src/main/webapp/WEB-INF/client/login.html`

### 6.2 Tiếp nhận bệnh nhân

- `src/main/webapp/WEB-INF/client/reception.html`

### 6.3 Khám bệnh

- `src/main/webapp/WEB-INF/client/medical-examination.html`

### 6.4 Dược

- `src/main/webapp/WEB-INF/client/pharmacy.html`

### 6.5 Thanh toán

- `src/main/webapp/WEB-INF/client/payment.html`

### 6.6 Quản lý phòng

- `src/main/webapp/WEB-INF/client/rooms.html`

### 6.7 Quy trình khám liên thông

- Lễ tân chọn phòng khám theo triệu chứng khi tiếp nhận. Thông tin bệnh nhân, phòng và trạng thái được lưu trong `localStorage.hisPatientFlow` để các màn hình nghiệp vụ dùng chung.
- Trang khám bệnh hiển thị hàng chờ của phòng đang đăng nhập theo bố cục danh sách bệnh nhân bên trái/nghiệp vụ hồ sơ ở bên phải, gồm dấu hiệu sinh tồn, triệu chứng, chẩn đoán, chỉ định cận lâm sàng và đơn thuốc.
- Khi bấm **Hoàn tất khám & chuyển viện phí**, ứng dụng gọi `POST /api/examinations/complete` để lưu lượt khám, hồ sơ, ICD và đơn thuốc trong một transaction. Chỉ sau khi lưu thành công, trạng thái trình duyệt mới chuyển thành `awaiting_payment` và hàng chờ được tải lại. Gửi lại cùng mã yêu cầu không tạo hồ sơ trùng.
- Danh sách phòng ở Thiết lập phòng và Tiếp nhận lấy từ database để hồ sơ khám liên kết đúng phòng. Hàng chờ tiếp nhận vẫn dùng `localStorage` như bản demo hiện tại; lịch sử đã lưu dùng chung qua database.
- Khi thu tiền thành công, trạng thái chuyển thành `awaiting_pharmacy` và mở trang cấp thuốc.
- Khi dược xác nhận phát thuốc, trạng thái chuyển thành `completed`.
- Các trạng thái giao diện: `waiting` → `examining` → `awaiting_payment` → `awaiting_pharmacy` → `completed`.

### 6.8 Lịch sử khám bệnh

- Trang `/examination-history`, dùng chung cho Admin, Bác Sĩ, Lễ Tân và Dược Sĩ sau đăng nhập.
- Tìm theo mã bệnh nhân, họ tên, số điện thoại; lọc khoảng ngày khám; phân trang 20 hồ sơ.
- Xem ngày khám, phòng, người khám, chẩn đoán, triệu chứng, tiền sử, chỉ định/ghi chú, dặn dò và thuốc đã kê.
- `GET /api/examination-history?q=&from=YYYY-MM-DD&to=YYYY-MM-DD&page=0&size=20` và `GET /api/examination-history/{id}` chỉ trả DTO cần hiển thị, không trả entity người dùng hoặc mật khẩu.
- Hồ sơ lấy từ bảng `examinations` cùng các bảng lượt khám/chẩn đoán/đơn thuốc hiện có; không cần bảng mới. Dữ liệu demo trước đây chỉ lưu trạng thái trong trình duyệt không được tự dựng thành hồ sơ khám.
- Lưu hoàn tất khám chỉ dành cho Admin/Bác Sĩ; các nhóm còn lại có quyền đọc lịch sử.

## 7. Dữ liệu cấu hình

File cấu hình chính:

- `src/main/resources/application.properties`

Project đã có cấu hình datasource MySQL và JPA. Thông tin kết nối được đọc từ biến môi trường `MYSQL_HOST`, `MYSQL_USERNAME` và `MYSQL_PASSWORD`; không nên ghi mật khẩu thật trực tiếp vào source code.

### Script import dữ liệu danh mục

- `sql/import_khoa_kham_benh.sql`: import 125 mã thuốc/vật tư và 216 lô từ `KhoaKhamBenh.xls`.
- `sql/import_icd_first_50.sql`: import 50 mã ICD đầu tiên từ `DM_ICD.xls` vào bảng `diagnoses`.
- `sql/import_icd_common_100.sql`: bổ sung 100 mã ICD chưa có, chọn từ nhiều nhóm bệnh trong `DM_ICD.xls`, giữ nguyên tên và mô tả của file nguồn.
- `sql/import_common_medicines_100.sql`: bổ sung 100 mục thuốc generic, mã `TD001`–`TD100`; dữ liệu dễ xem tại `sql/common_medicines_100.tsv`.

Hai script bổ sung 100 mục chỉ thêm bản ghi mới; chạy lại không ghi đè bản ghi có cùng mã. Thuốc được phân biệt theo hoạt chất, hàm lượng và dạng dùng (không phải 100 hoạt chất khác nhau). Đối chiếu dữ liệu với [WHO Model List of Essential Medicines, 24th list (2025)](https://www.who.int/publications/i/item/B09474), [bản PDF](https://www.iccp-portal.org/sites/default/files/2025-09/B09474-eng.pdf). Đây là dữ liệu danh mục, không kèm chỉ định hoặc liều điều trị. Giá `0` nghĩa là chưa cấu hình giá; không tạo nhà sản xuất, số đăng ký hoặc lô tồn kho giả.

Ngày 25/09/2026 đã áp dụng hai script bổ sung vào MySQL: `diagnoses` từ 55 lên 155 bản ghi; `medicines` từ 125 lên 225 bản ghi; giữ nguyên 216 lô thuốc và toàn bộ bản ghi danh mục cũ. Đã chạy mỗi script hai lần để xác nhận không tạo trùng.

Hai script chạy trong transaction và cập nhật bản ghi đã có theo khóa duy nhất. Sau khi cài MySQL client và cấu hình kết nối, chạy ví dụ:

```powershell
mysql -u root -p trung_cang_his < sql\import_icd_first_50.sql
```

Ngày 24/09/2026, script `import_icd_first_50.sql` đã được chạy thành công trên database cấu hình bởi `application.properties`; kiểm tra sau import xác nhận có đủ 50 mã ICD.

Ngày 25/09/2026, script `import_khoa_kham_benh.sql` đã được chạy thành công trên cùng database; kiểm tra sau import xác nhận có 125 mã thuốc/vật tư và 216 lô thuốc.

### Kiểm thử phân quyền

Chạy `mvnw.cmd test`. Bộ kiểm thử dùng H2 trong bộ nhớ, không ghi vào MySQL. `AccessControlTests` kiểm tra đăng nhập/session, menu và URL theo bốn nhóm, lựa chọn khoa, quyền API, chặn đọc template trực tiếp và chuyển đổi nhóm cũ có thể chạy lại.

`ExaminationHistoryTests` kiểm tra quyền đọc của cả bốn nhóm, quyền lưu, chi tiết ICD/thuốc, tìm kiếm/phân trang/lọc ngày, gửi lại yêu cầu không tạo trùng và rollback toàn bộ khi thuốc không hợp lệ. Sau bổ sung lịch sử: 23 kiểm thử đạt.

Ngày 25/09/2026: 15 kiểm thử đạt, đóng gói JAR thành công; đã chạy bản JAR với MySQL và xác nhận đủ bốn nhóm, bốn tài khoản đăng nhập, menu và quyền truy cập URL tương ứng. Dữ liệu tài khoản/nhóm đã được áp dụng vào MySQL cấu hình hiện tại. Khởi động lại ứng dụng và đăng nhập lại để dùng giao diện và quyền mới.

## 8. Chạy dự án

Triển khai website miễn phí bằng Render và Aiven MySQL: xem [DEPLOY.md](DEPLOY.md). Repository có sẵn `Dockerfile`, `render.yaml` và GitHub Actions; profile `prod` tắt dữ liệu/tài khoản demo, dùng mật khẩu qua biến môi trường.

### Yêu cầu

- JDK 21
- Maven
- MySQL (nếu sẽ kết nối database thực tế)

### Chạy bằng Maven

```bash
./mvnw clean package
./mvnw spring-boot:run
```

Trên Windows:

```powershell
mvnw.cmd clean package
mvnw.cmd spring-boot:run
```

Sau khi ứng dụng chạy, truy cập:

```text
http://localhost:8080/login
```

Các màn hình chính:

- `/rooms`
- `/reception`
- `/examination`
- `/pharmacy`
- `/payment`
- `/examination-history`

## 9. Trạng thái hiện tại

Project đang ở giai đoạn phát triển khung HIS với backend scaffolding hoàn chỉnh hơn so với bản đầu. Đã có:

- entity JPA chính
- controller CRUD cho nghiệp vụ chủ chốt
- repository + service cơ bản
- mock UI cho quy trình bệnh viện
- security config với xác thực session và tài khoản database-backed
- database seeder tạo các tài khoản nhân sự còn thiếu, giữ nguyên thông tin tài khoản đã có
- điều hướng giao diện theo role sau khi đăng nhập
- route controller cho các trang trong `WEB-INF/client`
- giao diện login, tiếp nhận và quản lý phòng đã gọi API backend

Tình trạng hiện tại đã được xác minh bằng lệnh compile Maven với JDK 21:

```bash
JAVA_HOME="/c/Program Files/Microsoft/jdk-21.0.12.101-hotspot" ./mvnw -q -DskipTests compile
```

Lệnh này đã chạy thành công sau khi chuyển môi trường sang Java 21.

## 10. Ghi chú quan trọng

- Nguyên nhân lỗi compile trước đây là sử dụng Java 24 trong khi project yêu cầu Java 21.
- Khi build bằng đúng JDK 21, Maven có thể compile dự án.
- Cần khởi động MySQL và đặt các biến môi trường datasource trước khi chạy đầy đủ ứng dụng.
- Tài khoản được đọc từ bảng `users`; `DatabaseSeeder` chỉ tạo tài khoản còn thiếu, không ghi đè mật khẩu, quyền, trạng thái hoặc hồ sơ đã chỉnh sửa.
- Mật khẩu dùng chung hiện phục vụ môi trường demo; production cần quản lý secret ngoài source code và bắt buộc đổi mật khẩu.

## 11. Kết luận

Trung Cang HIS là một dự án nền tảng cho hệ thống quản lý bệnh viện, với mô hình dữ liệu, nghiệp vụ, giao diện demo và tầng backend CRUD đang dần được hoàn thiện. Dự án đã có nền tảng khả quan để tiếp tục mở rộng thành hệ thống HIS vận hành thực tế.
