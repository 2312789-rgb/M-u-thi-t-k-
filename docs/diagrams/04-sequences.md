# Sơ đồ tuần tự

## Đặt hàng

```mermaid
sequenceDiagram
    actor Customer as Khách hàng
    participant Cart as CartController
    participant Service as OrderService
    participant UserRepo as AppUserRepository
    participant ProductRepo as ProductRepository
    participant OrderRepo as CustomerOrderRepository
    participant Payment as PaymentFactory

    Customer->>Cart: POST /cart/checkout
    Cart->>Cart: kiểm tra form và giỏ hàng
    Cart->>Service: placeOrder(email, form, lines)
    activate Service
    Service->>UserRepo: tìm tài khoản theo email
    UserRepo-->>Service: AppUser có role CUSTOMER
    loop từng sản phẩm trong giỏ
        Service->>ProductRepo: đọc giá và tồn kho hiện tại
        ProductRepo-->>Service: Product
        Service->>Service: kiểm tra trạng thái, số lượng, tính snapshot giá
        Service->>Service: giảm tồn, tăng soldCount
    end
    Service->>OrderRepo: saveAndFlush(CustomerOrder)
    OrderRepo-->>Service: đơn hàng đã lưu
    deactivate Service
    Cart->>Payment: create(paymentMethod)
    Payment-->>Cart: PaymentStrategy
    Cart->>Cart: xóa session cart sau khi đặt thành công
    Cart-->>Customer: chuyển tới trang xác nhận
```

Nếu không đủ tồn kho, service ném `IllegalArgumentException`; transaction rollback các thay đổi tồn kho và đơn hàng không được lưu.

## Admin cập nhật trạng thái đơn

```mermaid
sequenceDiagram
    actor Admin as Quản trị viên
    participant Controller as AdminController
    participant Service as OrderService
    participant State as OrderState
    participant Order as CustomerOrder
    participant Observer as OrderStatusObserver

    Admin->>Controller: POST trạng thái mới
    Controller->>Service: changeStatus(id, target)
    Service->>Order: đọc trạng thái hiện tại
    Service->>State: from(current).canTransitionTo(target)
    alt chuyển trạng thái không hợp lệ
        State-->>Service: false
        Service-->>Controller: lỗi nghiệp vụ
        Controller-->>Admin: hiển thị lỗi
    else chuyển hợp lệ
        State-->>Service: true
        opt trạng thái đích là CANCELLED
            Service->>Order: hoàn tồn kho và cập nhật số đã bán
        end
        Service->>Order: cập nhật và lưu trạng thái
        Service-->>Controller: đơn hàng đã cập nhật
        Controller->>Observer: thông báo các observer
        Controller-->>Admin: thông báo thành công
    end
```
