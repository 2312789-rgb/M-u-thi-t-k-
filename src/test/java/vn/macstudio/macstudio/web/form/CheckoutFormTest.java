package vn.macstudio.macstudio.web.form;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CheckoutFormTest {
    @Test
    void buildsShippingAddressFromStreetWardAndProvince() {
        CheckoutForm form = new CheckoutForm();
        form.setSpecificAddress("  12 Nguyễn Huệ  ");
        form.setWard("Phường Bến Nghé");
        form.setProvince("Thành phố Hồ Chí Minh");

        assertEquals("12 Nguyễn Huệ, Phường Bến Nghé, Thành phố Hồ Chí Minh", form.buildAddress());
    }
}
