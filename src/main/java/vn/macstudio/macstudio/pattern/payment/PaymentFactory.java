package vn.macstudio.macstudio.pattern.payment;

import java.util.List;
import org.springframework.stereotype.Component;
import vn.macstudio.macstudio.model.PaymentMethod;

@Component
public class PaymentFactory {
    private final List<PaymentStrategy> strategies;
    public PaymentFactory(List<PaymentStrategy> strategies) { this.strategies = List.copyOf(strategies); }
    public PaymentStrategy create(PaymentMethod method) {
        return strategies.stream().filter(strategy -> strategy.method() == method).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Phương thức thanh toán không được hỗ trợ."));
    }
}