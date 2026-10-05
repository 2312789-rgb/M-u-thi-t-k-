package vn.macstudio.macstudio.pattern.observer;

import vn.macstudio.macstudio.model.CustomerOrder;
import vn.macstudio.macstudio.model.OrderStatus;

public interface OrderStatusObserver {
    void onStatusChanged(CustomerOrder order, OrderStatus previousStatus);
}