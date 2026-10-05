package vn.macstudio.macstudio.pattern.state;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import vn.macstudio.macstudio.model.OrderStatus;

class OrderStateTest {
    @Test
    void allowsOnlyTransitionsDefinedForCurrentState() {
        assertTrue(OrderState.from(OrderStatus.PENDING).canTransitionTo(OrderStatus.CONFIRMED));
        assertTrue(OrderState.from(OrderStatus.PENDING).canTransitionTo(OrderStatus.CANCELLED));
        assertFalse(OrderState.from(OrderStatus.PENDING).canTransitionTo(OrderStatus.SHIPPING));

        assertTrue(OrderState.from(OrderStatus.CONFIRMED).canTransitionTo(OrderStatus.SHIPPING));
        assertTrue(OrderState.from(OrderStatus.CONFIRMED).canTransitionTo(OrderStatus.CANCELLED));
        assertFalse(OrderState.from(OrderStatus.CONFIRMED).canTransitionTo(OrderStatus.DELIVERED));

        assertTrue(OrderState.from(OrderStatus.SHIPPING).canTransitionTo(OrderStatus.DELIVERED));
        assertTrue(OrderState.from(OrderStatus.SHIPPING).canTransitionTo(OrderStatus.CANCELLED));
        assertFalse(OrderState.from(OrderStatus.SHIPPING).canTransitionTo(OrderStatus.CONFIRMED));
    }

    @Test
    void deliveredAndCancelledAreTerminalStates() {
        for (OrderStatus target : OrderStatus.values()) {
            assertFalse(OrderState.from(OrderStatus.DELIVERED).canTransitionTo(target));
            assertFalse(OrderState.from(OrderStatus.CANCELLED).canTransitionTo(target));
        }
    }
}
