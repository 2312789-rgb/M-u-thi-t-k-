package vn.macstudio.macstudio.web.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import vn.macstudio.macstudio.model.PaymentMethod;

public class CheckoutForm {
    @NotBlank @Size(max = 100) private String recipientName;
    @NotBlank @Pattern(regexp = "^\\+?[0-9 ()-]{8,20}$") private String phone;
    @NotBlank @Size(min = 8, max = 300) private String address;
    private PaymentMethod paymentMethod = PaymentMethod.COD;
    @Size(max = 500) private String note;
    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String recipientName) { this.recipientName = recipientName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}