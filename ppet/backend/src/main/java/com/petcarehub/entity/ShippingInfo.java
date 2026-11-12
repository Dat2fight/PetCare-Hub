package com.petcarehub.entity;

import org.hibernate.annotations.Nationalized;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Embeddable
public record ShippingInfo(
        @Nationalized @Column(name = "recipient_name", length = 100) @NotBlank @Size(min = 2, max = 100)
        @Pattern(regexp = "(?=.*\\p{L})[\\p{L}\\p{M} .'’\\-]+", message = "Tên người nhận không hợp lệ") String recipientName,
        @Nationalized @Column(name = "delivery_phone", length = 10) @NotBlank
        @Pattern(regexp = "0[35789][0-9]{8}", message = "Số di động Việt Nam không hợp lệ") String phone,
        @Nationalized @Column(length = 100) @NotBlank @Size(max = 100) String province,
        @Nationalized @Column(length = 100) @NotBlank @Size(max = 100) String district,
        @Nationalized @Column(length = 100) @NotBlank @Size(max = 100) String ward,
        @Nationalized @Column(name = "address_line", length = 200) @NotBlank @Size(min = 5, max = 200)
        @Pattern(regexp = "(?=.*\\p{L})\\S+ .+", message = "Địa chỉ phải có chữ và ít nhất hai từ") String addressLine,
        @Nationalized @Column(name = "delivery_note", length = 500) @Size(max = 500) String note) {
    public ShippingInfo {
        recipientName = clean(recipientName);
        phone = clean(phone);
        if (phone != null) phone = phone.replace(" ", "").replaceFirst("^\\+84", "0");
        province = clean(province);
        district = clean(district);
        ward = clean(ward);
        addressLine = clean(addressLine);
        note = clean(note);
    }

    private static String clean(String value) {
        return value == null ? null : value.replaceAll("(?U)\\s+", " ").strip();
    }

    public String formatted() {
        return String.join(", ", recipientName, phone, addressLine, ward, district, province);
    }
}
