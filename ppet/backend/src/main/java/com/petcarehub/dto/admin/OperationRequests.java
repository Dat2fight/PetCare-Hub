package com.petcarehub.dto.admin;
import jakarta.validation.constraints.*;
import com.petcarehub.enums.AppointmentStatus;
public final class OperationRequests {
    private OperationRequests() {}
    public record Action(@NotBlank @Pattern(regexp="confirm|process|ship|deliver|cancel") String action,@Size(max=500) String reason) {}
    public record Refund(@NotBlank @Size(max=100) String reference,@NotBlank @Size(max=500) String reason) {}
    public record Note(@NotBlank @Size(max=2000) String body) {}
    public record Stock(@NotNull Long productId,@NotBlank @Pattern(regexp="STOCK_IN|STOCK_OUT|ADJUSTMENT") String type,@NotNull @Min(0) Integer quantity,@NotBlank @Size(max=500) String reason) {}
    public record Assign(@NotNull Long staffId,@NotBlank @Size(max=500) String reason) {}
    public record Status(@NotNull AppointmentStatus status,@Size(max=500) String reason) {}
    public record Moderate(@NotBlank @Pattern(regexp="APPROVED|REJECTED|HIDDEN") String action,@Size(max=500) String reason) {}
}
