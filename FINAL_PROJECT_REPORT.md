# Báo cáo đồ án cuối kỳ — Mặc Studio

> Tài liệu nền để hoàn thiện báo cáo theo mẫu của giảng viên. Hãy thay thông tin trong ngoặc vuông bằng thông tin nhóm/lớp thực tế và chụp ảnh giao diện sau khi chạy trên máy trình diễn.

## 1. Thông tin đề tài

- **Tên đề tài:** Xây dựng website bán quần áo Mặc Studio và áp dụng các mẫu thiết kế hướng đối tượng.
- **Môn học:** Mẫu thiết kế (Design Patterns).
- **Sinh viên/nhóm:** [Bổ sung họ tên, mã sinh viên, lớp, giảng viên hướng dẫn].
- **Công nghệ:** Java 21, Spring Boot 4, Spring MVC, Thymeleaf, Spring Data JPA, Spring Security, H2; hỗ trợ cấu hình SQL Server.

## 2. Bài toán và mục tiêu

Cửa hàng thời trang cần một ứng dụng để giới thiệu sản phẩm, phân loại và tìm kiếm catalog, nhận đơn trực tuyến, kiểm tra tồn kho, theo dõi lịch sử mua và quản trị hoạt động bán hàng. Mục tiêu là triển khai các luồng nghiệp vụ này đồng thời thể hiện cách một số mẫu thiết kế giúp tách trách nhiệm và giới hạn thay đổi khi hệ thống mở rộng.

### Phạm vi chức năng

**Khách chưa đăng nhập:** xem, tìm và lọc catalog; đăng ký; đăng nhập.  
**Khách hàng:** quản lý giỏ hàng, đặt hàng COD/chuyển khoản mô phỏng, xem xác nhận và lịch sử đơn, sửa hồ sơ cá nhân.  
**Quản trị viên:** xem dashboard, quản lý sản phẩm/danh mục/khách hàng, cập nhật trạng thái đơn. Hủy đơn hợp lệ sẽ hoàn lại tồn kho và điều chỉnh số lượng đã bán.

Ngoài phạm vi: kết nối cổng thanh toán thực, gửi email/SMS thật, vận chuyển tích hợp, phân quyền nhiều cấp quản trị và triển khai production.

## 3. Kiến trúc và cấu trúc mã nguồn

Ứng dụng tổ chức theo các lớp:

- `model/`: entity JPA và enum nghiệp vụ.
- `repository/`: truy cập dữ liệu qua Spring Data JPA.
- `service/`: nghiệp vụ giỏ hàng/đặt hàng; `OrderService` quản lý transaction.
- `pattern/`: các lớp/contract minh họa Strategy, Factory, Observer và State.
- `web/` và `web/form/`: controller MVC, form và validation.
- `templates/`: giao diện Thymeleaf; `static/`: CSS, ảnh và tài nguyên giao diện.

Các sơ đồ use case, ERD, lớp và sequence nằm trong [docs/diagrams](docs/diagrams/).

## 4. Áp dụng mẫu thiết kế

| Mẫu | Vấn đề | Áp dụng trong đề tài | Hạn chế/phạm vi |
|---|---|---|---|
| Singleton | Dữ liệu metadata dùng chung trong ứng dụng | `StoreMetadata` là Spring bean singleton scope, cung cấp tên cửa hàng | Container đảm bảo vòng đời; không phải Singleton GoF với static accessor |
| Repository | Tránh để nghiệp vụ phụ thuộc truy vấn SQL cụ thể | Các Spring Data repository cho User, Product, Category, Order và OrderItem | Đây là abstraction do Spring Data sinh triển khai |
| Strategy | Thay đổi cách cung cấp hướng dẫn thanh toán | `PaymentStrategy`, các strategy COD/chuyển khoản | Mới mô phỏng hướng dẫn, không xử lý giao dịch thật |
| Factory | Chọn strategy phù hợp với phương thức thanh toán | `PaymentFactory.create(PaymentMethod)` | Factory chọn các bean đã đăng ký trong Spring |
| Observer | Tách phản ứng thông báo khỏi cập nhật trạng thái/đơn mới | Observer lưu thông báo cho customer/admin trong cùng transaction; header có badge và trang `/notifications` để đọc | Chưa gửi email/SMS/push |
| State | Quy tắc chuyển trạng thái phụ thuộc trạng thái hiện hành | `OrderState` quyết định chuyển tiếp hợp lệ; trạng thái kết thúc không nhận chuyển tiếp mới | Enum vẫn được lưu trong entity/DB; State chỉ đóng gói hành vi chuyển tiếp |

### Luồng State đơn hàng

`PENDING → CONFIRMED → SHIPPING → DELIVERED`; đơn được hủy từ `PENDING`, `CONFIRMED` hoặc `SHIPPING`. `DELIVERED` và `CANCELLED` là trạng thái cuối. Khi hủy, việc hoàn kho nằm cùng transaction với cập nhật đơn hàng.

### Luồng checkout và toàn vẹn tồn kho

`OrderService.placeOrder` chạy transaction mức `SERIALIZABLE`, xác thực tài khoản là khách hàng, đọc lại sản phẩm và giá hiện tại, xác thực trạng thái/tồn kho, tạo snapshot `unitPrice`, điều chỉnh kho và lưu đơn. Giỏ session chỉ được xóa sau khi đặt hàng và tạo hướng dẫn thanh toán thành công.

## 5. Mô hình dữ liệu

Các thực thể chính: `AppUser`, `Category`, `Product`, `CustomerOrder`, `OrderItem`.

- Một danh mục chứa nhiều sản phẩm.
- Một khách hàng có thể có nhiều đơn hàng.
- Một đơn hàng gồm một hoặc nhiều dòng hàng.
- Dòng hàng tham chiếu sản phẩm và lưu snapshot giá, số lượng, thành tiền.

Chi tiết quan hệ và thuộc tính chính xem [ERD](docs/diagrams/02-erd.md).

## 6. Bảo mật và lưu ý triển khai

- Mật khẩu tài khoản được mã hóa BCrypt.
- Route admin yêu cầu role `ADMIN`; checkout/lịch sử đơn yêu cầu role `CUSTOMER`.
- Các form nghiệp vụ dùng POST và Spring Security CSRF mặc định.
- Ảnh upload giới hạn 4 MB và loại MIME được phép trong ứng dụng.
- Tài khoản trong `DataInitializer` là dữ liệu demo có mật khẩu cố định; H2 console được bật trong cấu hình mặc định hiện tại. **Chỉ dùng trong môi trường demo/local:** không đưa dữ liệu demo/console ra Internet, thay đổi/xóa tài khoản mẫu và tắt console trước khi triển khai thật.
- SQL Server credentials đọc từ environment variables; không commit credentials.
- Hệ thống chưa được kiểm thử tải, rà soát an ninh triển khai, sao lưu/khôi phục hoặc kiểm thử trên môi trường production. Không mô tả ứng dụng là production-ready.

## 7. Cài đặt và chạy

Yêu cầu JDK 21. Mở PowerShell tại thư mục `java-version`:

```powershell
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

Ứng dụng mặc định chạy tại `http://localhost:5128`. Cơ sở dữ liệu H2 file-local được tạo và seed tự động lần đầu khởi chạy.

### Trình diễn trên mạng LAN

Để thiết bị cùng Wi-Fi/LAN truy cập, chạy `.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=lan`, lấy IPv4 adapter hiện tại bằng `ipconfig`, rồi mở `http://<IPv4>:5128` trên thiết bị đó. Profile `lan` bind vào giao diện mạng và tắt H2 Console. Nếu cần, chỉ mở inbound TCP 5128 cho Windows Firewall profile Private trong thời gian demo; không cấu hình port forwarding hoặc đưa ứng dụng demo ra Internet. Xem hướng dẫn chi tiết và lệnh gỡ firewall rule tại [README.md](README.md).

### Chia sẻ link Internet cố định

Có thể triển khai service Docker lên Render để nhận hostname `onrender.com`. Cấu hình profile `render` tắt H2 Console và tài khoản mẫu; mật khẩu admin phải cung cấp bằng biến môi trường bí mật `STORE_ADMIN_PASSWORD`. Gói free có thể ngủ và dữ liệu H2/ảnh/đơn hàng trong `/tmp` không bền vững qua restart/redeploy; đây chỉ là cách trình diễn, không dùng cho dữ liệu thật. Hướng dẫn kết nối repository và deploy nằm tại [README.md](README.md).

### Tài khoản demo

- **Admin:** `admin@macstudio.vn` / `Admin123!`
- **Khách:** `khach1@example.com` / `Customer123!`

Sau đăng nhập Admin, mở `/admin`. Các tài khoản demo chỉ phục vụ trình diễn. Không tái sử dụng các thông tin này ở môi trường công khai.

### Kịch bản demo bảo vệ (5–7 phút)

1. Mở trang chủ và catalog; trình bày tìm kiếm, lọc và chi tiết sản phẩm.
2. Đăng nhập tài khoản khách, thêm hàng vào giỏ và đặt đơn COD.
3. Mở lịch sử/chi tiết đơn và chỉ ra mã đơn, snapshot giá, trạng thái.
4. Đăng xuất khách, đăng nhập Admin và mở dashboard/đơn hàng.
5. Chuyển đơn theo thứ tự `PENDING → CONFIRMED → SHIPPING`; minh họa `DELIVERED` hoặc hủy đơn và giải thích hoàn kho.
6. Nêu role protection và chỉ ra các test, sơ đồ pattern trong báo cáo.

Trước buổi demo, chạy test và khởi động ứng dụng từ đầu để xác nhận H2/seed. Chuẩn bị ảnh chụp các màn hình home, catalog, checkout, lịch sử đơn và dashboard để đưa vào bản báo cáo theo yêu cầu giảng viên.

## 8. Kiểm thử

Chạy tất cả unit và application tests bằng `.\mvnw.cmd test`. Bộ test tập trung vào:

| Nhóm | Ca kiểm thử |
|---|---|
| State | Chuyển tiếp hợp lệ/không hợp lệ; trạng thái kết thúc |
| OrderService | Đặt đơn cập nhật kho và snapshot giá; không đủ kho không lưu đơn; chặn role không phải Customer; hủy đơn hoàn kho; chuyển trạng thái sai không ghi dữ liệu |
| Spring Security | Guest bị chuyển tới đăng nhập; Customer bị từ chối `/admin`; Admin truy cập dashboard được |
| Khởi động | Spring application context khởi động thành công |

Kết quả cần được lấy từ lần chạy `.\mvnw.cmd test` gần nhất; không ghi số test cố định trong báo cáo nếu bộ test đã thay đổi.

## 9. Giới hạn và hướng phát triển

1. Tích hợp payment gateway và callback/webhook an toàn.
2. Thay log observer bằng thông báo email/SMS qua adapter; cân nhắc xử lý bất đồng bộ và retry.
3. Tách cấu hình development/demo/production; tắt H2 console, seed tài khoản demo và cân nhắc `ddl-auto=validate`/migration khi deploy.
4. Bổ sung kiểm thử tích hợp với database riêng cho test, upload ảnh, CRUD và checkout đồng thời.
5. Bổ sung phân trang catalog/admin, quản lý tồn kho và báo cáo doanh thu theo khoảng thời gian.
6. Thực hiện threat model, security review, logging/audit và kiểm thử tải trước khi triển khai thật.

## 10. Câu hỏi gợi ý khi bảo vệ

1. State khác Strategy ở điểm nào? State thể hiện hành vi gắn với trạng thái hiện tại; Strategy thay một thuật toán/hành vi được lựa chọn.
2. Factory và Strategy phối hợp ra sao? Strategy định nghĩa hành vi thanh toán; Factory chọn implementation dựa trên enum phương thức thanh toán.
3. Tại sao cần snapshot `unitPrice`? Để lịch sử đơn không đổi khi giá catalog được cập nhật.
4. Điều gì xảy ra nếu checkout thất bại giữa chừng? Transaction rollback thay đổi đơn và tồn kho; session cart chỉ xóa sau khi service thành công.
5. Singleton trong bài có phải GoF thuần không? Không; Spring quản lý scope singleton. Đây là lựa chọn container-managed và cần nêu chính xác giới hạn đó.
6. Observer hiện làm gì? Lưu thông báo vào DB cho khách/admin sau khi đổi trạng thái và báo admin khi có đơn mới; chưa gửi email/SMS/push.

## Tài liệu tham khảo

- Gamma, Helm, Johnson, Vlissides, *Design Patterns: Elements of Reusable Object-Oriented Software*, Addison-Wesley, 1994.
- Spring Framework Reference Documentation: dependency injection, transaction management, Spring MVC.
- Spring Data JPA Reference Documentation.
- Spring Security Reference Documentation.
