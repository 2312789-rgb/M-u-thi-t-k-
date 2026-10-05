package vn.macstudio.macstudio.pattern.payment;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;
import vn.macstudio.macstudio.model.PaymentMethod;

@Component
public class BankTransferPaymentStrategy implements PaymentStrategy {
    @Override public PaymentMethod method() { return PaymentMethod.BANK_TRANSFER; }
    @Override public String instructions(long orderId, BigDecimal amount) {
        return "Chuyển khoản " + amount.toPlainString() + " VND với nội dung DH" + orderId + ".";
    }
}