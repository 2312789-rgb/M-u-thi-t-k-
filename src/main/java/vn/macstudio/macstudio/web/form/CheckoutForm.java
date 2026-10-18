package vn.macstudio.macstudio.web.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import vn.macstudio.macstudio.model.PaymentMethod;

public class CheckoutForm {
    @NotBlank @Size(max = 100) private String recipientName;
    @NotBlank @Pattern(regexp = "^\\+?[0-9 ()-]{8,20}$") private String phone;
    @NotBlank @Size(max = 60) private String province;
    @NotBlank @Size(max = 60) private String ward;
    @NotBlank @Size(min = 3, max = 170) private String specificAddress;
    private String address;
    private PaymentMethod paymentMethod = PaymentMethod.COD;
    @Size(max = 500) private String note;
    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String recipientName) { this.recipientName = recipientName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }
    public String getWard() { return ward; }
    public void setWard(String ward) { this.ward = ward; }
    public String getSpecificAddress() { return specificAddress; }
    public void setSpecificAddress(String specificAddress) { this.specificAddress = specificAddress; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String buildAddress() {
        return String.join(", ", specificAddress.trim(), ward.trim(), province.trim());
    }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}