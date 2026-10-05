# Mặc Studio Java

Phiên bản chuyển đổi sang **Java 21 + Spring Boot 4 + Thymeleaf + Spring Data JPA + Spring Security**. Bản C# ở thư mục cha được giữ làm dự phòng; database Java mặc định là H2 file-local, không ghi vào SQL Server `ClothingStoreDb` của C#.

## Chạy

Mở terminal tại thư mục `java-version`:

```powershell
./mvnw.cmd test
./mvnw.cmd spring-boot:run
```

Hoặc từ thư mục gốc:

```powershell
./java-version/mvnw.cmd -f ./java-version/pom.xml spring-boot:run
```

Mở `http://localhost:5128`. JDK 21 đã cài là đủ; Maven Wrapper tải Maven/dependency tự động ở lần đầu. H2 lưu dữ liệu trong `java-version/data/clothingstore-java` và tự tạo schema/seed khi app khởi động. Console dev: `http://localhost:5128/h2-console`.

## Cho thiết bị cùng mạng LAN truy cập

Chỉ sử dụng trong mạng Wi-Fi/LAN tin cậy; không cấu hình port forwarding trên router để đưa ứng dụng demo ra Internet.

1. Trên máy chạy ứng dụng, mở PowerShell tại `java-version` và chạy:

   ```powershell
   .\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=lan
   ```

   Profile `lan` bind ứng dụng vào các giao diện mạng (`0.0.0.0`) và tắt H2 Console.

2. Trên máy chủ, chạy `ipconfig`, tìm `IPv4 Address` của adapter Wi-Fi/Ethernet đang kết nối. Không dùng địa chỉ `127.0.0.1`.

3. Trên thiết bị khác cùng mạng Wi-Fi/LAN, mở `http://<IPv4-cua-may-chu>:5128`, ví dụ `http://192.168.1.20:5128`.

4. Nếu bị timeout, cho phép inbound TCP port `5128` trên **Private network** trong Windows Defender Firewall. Có thể mở PowerShell bằng quyền Administrator và tạo rule:

   ```powershell
   New-NetFirewallRule -DisplayName "MacStudio LAN demo" -Direction Inbound -Protocol TCP -LocalPort 5128 -Action Allow -Profile Private
   ```

   Xóa rule sau buổi demo bằng:

   ```powershell
   Remove-NetFirewallRule -DisplayName "MacStudio LAN demo"
   ```

Tài khoản admin trong seed là thông tin công khai phục vụ demo. Chỉ chia sẻ địa chỉ với người tin cậy, không dùng dữ liệu thật, và không gửi tài khoản admin cho người xem. Profile LAN chỉ giới hạn mục tiêu truy cập trong mạng nội bộ; nó không biến cấu hình demo thành triển khai production.

## Triển khai link Internet cố định miễn phí trên Render

Render tạo hostname công khai ổn định dạng `https://<ten-service>.onrender.com`. Gói miễn phí có thể đưa service về trạng thái ngủ khi ít truy cập; lần mở đầu sau đó sẽ khởi động chậm. H2, ảnh upload và đơn hàng dùng dữ liệu tạm trong `/tmp`, có thể mất khi service restart/redeploy. Không nhập dữ liệu cá nhân hoặc dữ liệu thanh toán thật.

1. Đưa **riêng nội dung thư mục `java-version`** lên một GitHub repository của bạn; không đẩy thư mục Downloads/home chứa dữ liệu cá nhân lên GitHub. Không đưa mật khẩu/token vào repository.
2. Trên Render chọn **New → Web Service**, kết nối repository vừa tạo, chọn runtime **Docker**, và đặt **Root Directory** là `java-version` (nếu repository chỉ chứa nội dung của thư mục này thì để trống).
3. Chọn gói Free và tạo service. Render tự build từ `Dockerfile`; đợi trạng thái Live rồi mở hostname được hiển thị trên trang service.
4. Trong **Environment**, thêm `SPRING_PROFILES_ACTIVE=render` và `STORE_ADMIN_PASSWORD` là mật khẩu mạnh, riêng, tối thiểu 16 ký tự. Có thể đặt `STORE_ADMIN_EMAIL` nếu muốn đổi email admin; mặc định là `admin@macstudio.vn`. Không gửi mật khẩu admin cho người xem.
5. Đặt secret trước lần deploy đầu tiên: khi tài khoản admin đã được tạo, đổi biến `STORE_ADMIN_PASSWORD` không tự đổi mật khẩu đã mã hóa trong database. Nếu cần thay do lộ secret, xóa/tạo lại demo service để seed mới rồi kiểm tra đăng nhập.

Profile `render` tắt H2 Console và các tài khoản/đơn hàng mẫu khách hàng; chỉ tạo admin theo biến môi trường. Sản phẩm và danh mục demo vẫn được seed. Hãy test đường dẫn `/`, `/products` và `/admin` sau khi deploy. Nếu URL không trả lời, xem tab **Logs** của Render để biết lỗi khởi động.

**Lưu ý hiện trạng repository:** để Render tự động deploy, cần có GitHub repository riêng chứa `java-version` và kết nối tài khoản Render của bạn. Không commit/push cả thư mục `C:\Users\PC` hoặc thư mục Downloads. Mình không thể đăng nhập/tạo service bên thứ ba thay bạn; sau khi Render cấp hostname, gửi hostname đó cho người xem.

## Báo cáo và sơ đồ

- Báo cáo nền đồ án, hướng dẫn demo và giới hạn triển khai: [FINAL_PROJECT_REPORT.md](FINAL_PROJECT_REPORT.md).
- Mẫu thiết kế và câu hỏi vấn đáp: [DESIGN_PATTERNS.md](DESIGN_PATTERNS.md).
- Use case, ERD, class diagram và sequence diagram: [docs/diagrams](docs/diagrams/).
- Chạy toàn bộ test: `./mvnw.cmd test`.

## Tài khoản demo

- Admin: `admin@macstudio.vn` / `Admin123!`
- Customer: `khach1@example.com` / `Customer123!` (có thêm `khach2` đến `khach5`)

## Chức năng đã chuyển

- Trang chủ, catalog tìm/lọc theo tên, danh mục, size, màu, khoảng giá và sắp xếp.
- Đăng ký/đăng nhập bằng BCrypt, role Admin/Customer, hồ sơ khách hàng.
- Session cart, kiểm tra tồn kho, checkout transactional, snapshot giá, confirmation và lịch sử đơn.
- Thông báo lưu trong database: khách hàng nhận cập nhật trạng thái đơn; admin nhận thông báo đơn mới và trạng thái thay đổi; badge số chưa đọc và trang đánh dấu đã đọc.
- Admin dashboard, CRUD sản phẩm/danh mục/khách hàng, upload ảnh, quản lý trạng thái đơn; hủy đơn hoàn kho.
- Design Patterns: Singleton (`StoreMetadata`), Repository (Spring Data repositories), Strategy + Factory (payment), Observer (order status log), State (quy tắc chuyển trạng thái đơn hàng).
- Ảnh belt/scarf local ở `src/main/resources/static/images`; CSS của bản C# được dùng lại.

## SQL Server thay cho H2

Máy hiện tại cho phép kết nối Windows tới `SQLEXPRESS` qua local protocol, nhưng SQL Server TCP chưa bật. JDBC cần TCP; bật TCP/IP trong SQL Server Configuration Manager và khởi động lại instance, sau đó cấp SQL login cho ứng dụng. Ví dụ PowerShell:

```powershell
$env:STORE_DB_URL = "jdbc:sqlserver://localhost;instanceName=SQLEXPRESS;databaseName=ClothingStoreJavaDb;encrypt=true;trustServerCertificate=true"
$env:STORE_DB_USERNAME = "store_app"
$env:STORE_DB_PASSWORD = "<your-local-secret>"
./mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=sqlserver
```

Profile cấu hình nằm tại `src/main/resources/application-sqlserver.properties`. Nếu dùng cổng TCP cố định, đặt URL dạng `jdbc:sqlserver://localhost:1433;databaseName=ClothingStoreJavaDb;...` và bỏ `instanceName`.

## Thiết kế

- `model/`: JPA entities, enum trạng thái/role/phương thức thanh toán.
- `repository/`: Spring Data repositories.
- `service/`: cart, order transaction và user details.
- `pattern/payment/`: Strategy/Factory; `pattern/observer/`: Observer.
- `web/`: controllers và form validation.
- `templates/`: Thymeleaf pages/fragments; `static/`: CSS/ảnh.

Giải thích pattern và câu hỏi vấn đáp: [DESIGN_PATTERNS.md](DESIGN_PATTERNS.md).

**Giới hạn:** thông báo Observer hiện ghi log; COD/chuyển khoản chỉ mô phỏng hướng dẫn, chưa nối gateway. Ảnh sản phẩm seed ngoài hai ảnh phụ kiện local vẫn dùng Unsplash. Profile SQL Server yêu cầu TCP bật và cấu hình credentials bằng environment variables.

**Chỉ dùng demo/local:** tài khoản mẫu có mật khẩu cố định và H2 console đang bật trong cấu hình mặc định. Không triển khai cấu hình này ra Internet; xem mục bảo mật và checklist production trong [FINAL_PROJECT_REPORT.md](FINAL_PROJECT_REPORT.md).