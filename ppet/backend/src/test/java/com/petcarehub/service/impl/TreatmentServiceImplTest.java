package com.petcarehub.service.impl;

import com.petcarehub.dto.health.TreatmentCreateRequest;
import com.petcarehub.entity.*;
import com.petcarehub.exception.BadRequestException;
import com.petcarehub.repository.*;
import com.petcarehub.security.Ownership;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TreatmentServiceImplTest {
    @Test void persistsTreatmentAndRejectsReversedDates() {
        var treatments = mock(TreatmentRepository.class);
        var records = mock(MedicalRecordRepository.class);
        var ownership = mock(Ownership.class);
        var service = new TreatmentServiceImpl(treatments, records, ownership);
        Pet pet = new Pet(); pet.setId(3L);
        MedicalRecord record = new MedicalRecord(); record.setId(2L); record.setPet(pet);
        when(records.findById(2L)).thenReturn(Optional.of(record));
        when(treatments.save(any())).thenAnswer(call -> { Treatment t=call.getArgument(0); t.setId(1L); return t; });
        var request = new TreatmentCreateRequest(); request.setMedicalRecordId(2L);
        request.setMedicine("Thuốc A"); request.setDosage("1 viên"); request.setFrequency("Mỗi ngày");
        request.setStartDate(LocalDate.now()); request.setEndDate(LocalDate.now().plusDays(1));
        var result=service.createTreatment(request);
        assertEquals(1L, result.getId()); assertEquals("Mỗi ngày", result.getFrequency());
        verify(ownership).requirePet(3L);
        request.setEndDate(LocalDate.now().minusDays(1));
        assertThrows(BadRequestException.class, () -> service.createTreatment(request));
        verify(treatments, times(1)).save(any());
    }
}
