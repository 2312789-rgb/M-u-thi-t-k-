# Sơ đồ lớp các mẫu thiết kế

## Thanh toán: Strategy + Factory

```mermaid
classDiagram
    class CartController {
        +checkout()
    }
    class PaymentFactory {
        +create(method) PaymentStrategy
    }
    class PaymentStrategy {
        <<interface>>
        +instructions(orderId, amount) String
    }
    class CodPaymentStrategy
    class BankTransferPaymentStrategy
    CartController --> PaymentFactory
    PaymentFactory --> PaymentStrategy : chọn theo PaymentMethod
    PaymentStrategy <|.. CodPaymentStrategy
    PaymentStrategy <|.. BankTransferPaymentStrategy
```

## Vòng đời đơn hàng: State

```mermaid
classDiagram
    class OrderService {
        +changeStatus(orderId, target) CustomerOrder
    }
    class OrderState {
        <<abstract>>
        +from(status)$ OrderState
        +canTransitionTo(target)* boolean
    }
    class Pending
    class Confirmed
    class Shipping
    class Delivered
    class Cancelled
    class CustomerOrder {
        -status: OrderStatus
    }
    OrderService ..> OrderState : kiểm tra chuyển trạng thái
    OrderService --> CustomerOrder : thay đổi trạng thái
    OrderState <|-- Pending
    OrderState <|-- Confirmed
    OrderState <|-- Shipping
    OrderState <|-- Delivered
    OrderState <|-- Cancelled
```

Các trạng thái cụ thể là lớp lồng nhau `private` trong `OrderState`; hình biểu diễn vai trò của chúng, không hàm ý mỗi trạng thái là một file công khai riêng.

## Thông báo thay đổi trạng thái: Observer

```mermaid
classDiagram
    class AdminController {
        +changeStatus(orderId, status)
    }
    class OrderService {
        +changeStatus(orderId, target)
    }
    class OrderStatusObserver {
        <<interface>>
        +onStatusChanged(order, previous)
    }
    class AdminOrderObserver
    class CustomerOrderObserver
    AdminController --> OrderService
    AdminController o-- OrderStatusObserver : danh sách observers
    OrderStatusObserver <|.. AdminOrderObserver
    OrderStatusObserver <|.. CustomerOrderObserver
```

## Truy cập dữ liệu: Repository

```mermaid
classDiagram
    class JpaRepository~T, ID~ {
        <<Spring Data>>
    }
    class ProductRepository
    class CategoryRepository
    class AppUserRepository
    class CustomerOrderRepository
    class OrderItemRepository
    JpaRepository <|.. ProductRepository
    JpaRepository <|.. CategoryRepository
    JpaRepository <|.. AppUserRepository
    JpaRepository <|.. CustomerOrderRepository
    JpaRepository <|.. OrderItemRepository
```

Spring quản lý `StoreMetadata` theo singleton scope. Đây là singleton do container quản lý, không phải triển khai Singleton GoF bằng constructor riêng/static accessor.
