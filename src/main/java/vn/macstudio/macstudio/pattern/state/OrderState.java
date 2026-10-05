package vn.macstudio.macstudio.pattern.state;

import vn.macstudio.macstudio.model.OrderStatus;

public abstract class OrderState {
    private static final OrderState PENDING = new Pending();
    private static final OrderState CONFIRMED = new Confirmed();
    private static final OrderState SHIPPING = new Shipping();
    private static final OrderState DELIVERED = new Delivered();
    private static final OrderState CANCELLED = new Cancelled();

    private OrderState() {}

    public abstract boolean canTransitionTo(OrderStatus target);

    public static OrderState from(OrderStatus status) {
        return switch (status) {
            case PENDING -> PENDING;
            case CONFIRMED -> CONFIRMED;
            case SHIPPING -> SHIPPING;
            case DELIVERED -> DELIVERED;
            case CANCELLED -> CANCELLED;
        };
    }

    private static final class Pending extends OrderState {
        @Override
        public boolean canTransitionTo(OrderStatus target) {
            return target == OrderStatus.CONFIRMED || target == OrderStatus.CANCELLED;
        }
    }

    private static final class Confirmed extends OrderState {
        @Override
        public boolean canTransitionTo(OrderStatus target) {
            return target == OrderStatus.SHIPPING || target == OrderStatus.CANCELLED;
        }
    }

    private static final class Shipping extends OrderState {
        @Override
        public boolean canTransitionTo(OrderStatus target) {
            return target == OrderStatus.DELIVERED || target == OrderStatus.CANCELLED;
        }
    }

    private static final class Delivered extends OrderState {
        @Override
        public boolean canTransitionTo(OrderStatus target) {
            return false;
        }
    }

    private static final class Cancelled extends OrderState {
        @Override
        public boolean canTransitionTo(OrderStatus target) {
            return false;
        }
    }
}
