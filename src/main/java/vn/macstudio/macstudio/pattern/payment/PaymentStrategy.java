package vn.macstudio.macstudio.pattern.payment;

import java.math.BigDecimal;
import vn.macstudio.macstudio.model.PaymentMethod;

public interface PaymentStrategy {
    PaymentMethod method();
    String instructions(long orderId, BigDecimal amount);
}