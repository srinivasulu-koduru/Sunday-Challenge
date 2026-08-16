package com.sundaychallenge.repository;

import com.sundaychallenge.entity.AdminAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdminAccessRepository extends JpaRepository<AdminAccess, Long> {

    Optional<AdminAccess> findByEmailIgnoreCase(String email);

    Optional<AdminAccess> findByEmailIgnoreCaseAndActiveTrue(String email);

    boolean existsByEmailIgnoreCaseAndActiveTrue(String email);

    long countByActiveTrue();

    List<AdminAccess> findAllByOrderByCreatedAtDesc();
}
