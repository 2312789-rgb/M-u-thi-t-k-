package vn.macstudio.macstudio.pattern.observer;

import vn.macstudio.macstudio.model.CustomerOrder;

public interface OrderCreatedObserver {
    void onOrderCreated(CustomerOrder order);
}
