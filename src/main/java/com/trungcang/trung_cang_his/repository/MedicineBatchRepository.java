package com.trungcang.trung_cang_his.repository;

import com.trungcang.trung_cang_his.domain.MedicineBatch;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface MedicineBatchRepository extends JpaRepository<MedicineBatch, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select batch from MedicineBatch batch where batch.medicine.id = :medicineId and batch.quantity > 0 and (batch.expiryDate is null or batch.expiryDate >= :today) order by batch.expiryDate, batch.id")
    List<MedicineBatch> findAvailableForDispensing(@Param("medicineId") Long medicineId, @Param("today") LocalDate today);

    @Query("select coalesce(sum(batch.quantity), 0) from MedicineBatch batch where batch.medicine.id = :medicineId and batch.quantity > 0 and (batch.expiryDate is null or batch.expiryDate >= :today)")
    Long countAvailableForDispensing(@Param("medicineId") Long medicineId, @Param("today") LocalDate today);
}