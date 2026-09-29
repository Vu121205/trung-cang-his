package com.trungcang.trung_cang_his;

import com.trungcang.trung_cang_his.domain.Patient;
import com.trungcang.trung_cang_his.repository.PatientRepository;
import com.trungcang.trung_cang_his.service.impl.PatientServiceImpl;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PatientDateTests {
    @Test
    void creationUsesServerTimeInVietnamEvenIfClientSuppliesAnOldDate() {
        PatientRepository repository = mock(PatientRepository.class);
        when(repository.save(any(Patient.class))).thenAnswer(call -> call.getArgument(0));
        Patient patient = new Patient();
        patient.setCreatedAt(LocalDateTime.of(2000, 1, 1, 0, 0));
        LocalDateTime before = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        new PatientServiceImpl(repository).create(patient);
        LocalDateTime after = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        assertFalse(patient.getCreatedAt().isBefore(before));
        assertFalse(patient.getCreatedAt().isAfter(after));
        assertEquals(patient.getCreatedAt(), patient.getUpdatedAt());
    }

    @Test
    void editingOldPatientDoesNotMoveCreationDateToToday() {
        PatientRepository repository = mock(PatientRepository.class);
        Patient patient = new Patient();
        LocalDateTime original = LocalDateTime.of(2026, 9, 1, 10, 0);
        patient.setCreatedAt(original);
        when(repository.findById(1L)).thenReturn(Optional.of(patient));
        new PatientServiceImpl(repository).update(1L, new Patient());
        assertEquals(original, patient.getCreatedAt());
        assertNotNull(patient.getUpdatedAt());
    }
}
