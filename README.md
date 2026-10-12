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

## Đăng nhập bằng Google và Apple

Đăng nhập xã hội chỉ hoạt động sau khi tạo ứng dụng OAuth ở Google/Apple và cấu hình biến môi trường. Không commit Client Secret, Apple `.p8` key hay Apple client-secret JWT lên GitHub. Nút của nhà cung cấp chưa cấu hình sẽ ở trạng thái tắt trên trang đăng nhập.

### Google

1. Trong Google Cloud Console, tạo/chọn project, cấu hình OAuth consent screen và tạo **OAuth client ID → Web application**.
2. Thêm Authorized JavaScript origin cho từng host, ví dụ `http://localhost:5128` và `https://<ten-service>.onrender.com`.
3. Thêm Authorized redirect URI tương ứng: `http://localhost:5128/login/oauth2/code/google` và `https://<ten-service>.onrender.com/login/oauth2/code/google`.
4. Đặt `GOOGLE_CLIENT_ID` và `GOOGLE_CLIENT_SECRET` trong environment của ứng dụng. Nếu consent screen còn ở Testing, thêm email Google thử nghiệm vào danh sách Test users.

### Apple

Sign in with Apple trên website yêu cầu Apple Developer account, App ID bật Sign in with Apple, **Services ID**, Sign in with Apple private key (`.p8`) và một domain HTTPS đã xác minh. `localhost` không dùng làm Return URL Apple; hãy cấu hình và thử Apple sau khi có hostname HTTPS, chẳng hạn Render.

1. Trong Apple Developer, bật Sign in with Apple cho App ID, sau đó tạo Services ID cho website.
2. Cấu hình domain/return URL của Services ID. Return URL phải trùng tuyệt đối với `https://<ten-service>.onrender.com/login/oauth2/code/apple`. Hoàn thành bước verify domain của Apple nếu Developer portal yêu cầu.
3. Tạo Sign in with Apple key, ghi lại Team ID, Key ID, Services ID và tải file `.p8`. Lưu file key ngoài repository.
4. Với Node.js đã cài, chạy trong CMD tại thư mục `java-version` để tạo client secret ký từ key của bạn (JWT hết hạn sau 180 ngày):

   ```cmd
   node tools\generate-apple-client-secret.cjs TEAMID1234 KEYID5678 vn.macstudio.web "C:\duong-dan-an-toan\AuthKey_KEYID5678.p8"
   ```

   Thay các giá trị ví dụ bằng Team ID, Key ID và Services ID thật của bạn.

5. Đặt `APPLE_CLIENT_ID` bằng Services ID và `APPLE_CLIENT_SECRET` bằng JWT vừa tạo trong environment của ứng dụng. Khi JWT hết hạn, tạo secret mới và cập nhật biến môi trường.

### Cấu hình local hoặc Render

Trong CMD, đặt biến môi trường trước khi chạy ứng dụng (thay giá trị bằng credential của bạn, không gửi chúng vào chat):

```cmd
set "GOOGLE_CLIENT_ID=your-google-client-id"
set "GOOGLE_CLIENT_SECRET=your-google-client-secret"
set "APPLE_CLIENT_ID=your-apple-services-id"
set "APPLE_CLIENT_SECRET=paste-the-generated-apple-jwt-here"
java-version\mvnw.cmd -f java-version\pom.xml spring-boot:run
```

Nếu chạy từ `java-version`, dùng `.\mvnw.cmd spring-boot:run` trong PowerShell sau khi đặt cùng các biến môi trường. Trên Render, thêm các biến ở **Environment** rồi redeploy. Callback Apple dùng `form_post`; profile `render` bật cookie Secure/SameSite=None để phiên đăng nhập OAuth được giữ qua callback. OAuth mới tự tạo tài khoản khách hàng cho email đã xác thực hoặc liên kết an toàn với tài khoản khách hàng có cùng email. Tài khoản quản trị không thể đăng nhập qua nhà cung cấp xã hội.

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
- Chat hỗ trợ lưu lịch sử giữa khách hàng và đội ngũ quản trị, tự gửi lời chào khi mở chat lần đầu, cảnh báo tin nhắn chưa đọc cho cả hai phía và tự làm mới mỗi 4 giây.
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