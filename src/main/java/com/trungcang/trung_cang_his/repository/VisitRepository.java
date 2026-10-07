package com.trungcang.trung_cang_his.repository;

import com.trungcang.trung_cang_his.domain.Visit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface VisitRepository extends JpaRepository<Visit, Long> {
	List<Visit> findAllByStatusOrderByVisitDateAscVisitTimeAsc(Visit.Status status);
	java.util.Optional<Visit> findByVisitCode(String visitCode);
	@org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
	@org.springframework.data.jpa.repository.Query("select v from Visit v where v.id = :id")
	java.util.Optional<Visit> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") Long id);
}
