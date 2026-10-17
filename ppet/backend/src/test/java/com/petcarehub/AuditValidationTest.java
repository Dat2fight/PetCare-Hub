package com.petcarehub;

import com.petcarehub.dto.auth.RegisterRequest;
import com.petcarehub.dto.product.ProductCreateRequest;
import com.petcarehub.dto.health.*;
import com.petcarehub.exception.GlobalExceptionHandler;
import com.petcarehub.service.impl.VaccinationServiceImpl;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class AuditValidationTest {
    @Test void requiredFieldsAndNegativePricesAreRejected() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertFalse(validator.validate(new RegisterRequest()).isEmpty());
            assertFalse(validator.validate(new MedicalRecordCreateRequest()).isEmpty());
            assertFalse(validator.validate(new TreatmentCreateRequest()).isEmpty());
            assertFalse(validator.validate(new VaccinationCreateRequest()).isEmpty());
            var product = new ProductCreateRequest();
            product.setName("Test"); product.setCategoryId(1L); product.setPrice(BigDecimal.valueOf(-1));
            assertTrue(validator.validate(product).stream().anyMatch(v -> v.getPropertyPath().toString().equals("price")));
            product.setPrice(BigDecimal.ZERO);
            assertTrue(validator.validate(product).isEmpty());
        }
    }

    @Test void reversedVaccinationDatesAreRejectedBeforePersistence() {
        var service = new VaccinationServiceImpl(null, null, null);
        var request = new VaccinationCreateRequest();
        request.setDateAdministered(LocalDate.now()); request.setNextDueDate(LocalDate.now().minusDays(1));
        assertThrows(com.petcarehub.exception.BadRequestException.class, () -> service.createVaccination(request));
        assertThrows(com.petcarehub.exception.BadRequestException.class, () -> service.updateVaccination(1L, request));
    }

    @Test void exceptionMappingsDoNotLeakInternalDetails() {
        var handler = new GlobalExceptionHandler();
        var request = new ServletWebRequest(new MockHttpServletRequest());
        var error = handler.handleGeneral(new RuntimeException("SECRET_SQL"), request).getBody();
        assertNotNull(error.correlationId()); assertFalse(error.message().contains("SECRET_SQL"));
        assertEquals(404, handler.handleResourceNotFound(new jakarta.persistence.EntityNotFoundException(), request).getStatusCode().value());
        assertEquals(409, handler.handleInvalidStateTransition(new IllegalStateException(), request).getStatusCode().value());
        assertEquals(409, handler.handleIntegrity(new RuntimeException("SECRET_SQL"), request).getStatusCode().value());
        assertEquals(400, handler.handleUnreadable(new RuntimeException(), request).getStatusCode().value());
        assertEquals(401, handler.handleBadCredentials(new org.springframework.security.authentication.BadCredentialsException("secret"), request).getStatusCode().value());
        assertEquals(403, handler.handleAccessDenied(new org.springframework.security.access.AccessDeniedException("secret"), request).getStatusCode().value());
        assertEquals(400, handler.handleMissing(new org.springframework.web.bind.MissingServletRequestParameterException("id", "long"), request).getStatusCode().value());
        assertEquals(400, handler.handleType(new org.springframework.web.method.annotation.MethodArgumentTypeMismatchException("x", Long.class, "id", null, null), request).getStatusCode().value());
        assertEquals(400, handler.handleConstraint(new jakarta.validation.ConstraintViolationException(java.util.Set.of()), request).getStatusCode().value());
    }
}
