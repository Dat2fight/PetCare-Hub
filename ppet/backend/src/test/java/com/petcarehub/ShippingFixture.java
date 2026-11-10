package com.petcarehub;

import com.petcarehub.entity.ShippingInfo;

public final class ShippingFixture {
    public static ShippingInfo valid() {
        return new ShippingInfo("Nguyễn An", "+84 912345678", "Hà Nội", "Hoàn Kiếm", "Tràng Tiền", "12 Phố Huế", null);
    }
}
