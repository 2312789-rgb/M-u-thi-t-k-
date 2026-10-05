package vn.macstudio.macstudio.pattern.payment;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;
import vn.macstudio.macstudio.model.PaymentMethod;

@Component
public class CodPaymentStrategy implements PaymentStrategy {
    @Override public PaymentMethod method() { return PaymentMethod.COD; }
    @Override public String instructions(long orderId, BigDecimal amount) { return "Thanh toán tiền mặt khi nhận hàng."; }
}