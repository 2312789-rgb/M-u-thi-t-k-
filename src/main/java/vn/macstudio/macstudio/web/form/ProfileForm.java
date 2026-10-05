package vn.macstudio.macstudio.web.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ProfileForm {
    @NotBlank @Size(max = 100) private String fullName;
    @NotBlank @Pattern(regexp = "^\\+?[0-9 ()-]{8,20}$") private String phone;
    @NotBlank @Size(min = 8, max = 300) private String address;
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
}