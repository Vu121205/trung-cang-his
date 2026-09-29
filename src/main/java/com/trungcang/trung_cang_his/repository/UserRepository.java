package com.trungcang.trung_cang_his.repository;

import com.trungcang.trung_cang_his.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    List<User> findAllByRole_Id(Long roleId);
    boolean existsByRole_Id(Long roleId);
    boolean existsByUsername(String username);
    List<User> findAllByDeletedFalseOrderByFullNameAsc();
    boolean existsByUsernameIgnoreCase(String username);
    boolean existsByEmployeeCodeIgnoreCaseAndIdNot(String employeeCode, Long id);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select u from User u order by u.id")
    List<User> lockForAdministration();
}
