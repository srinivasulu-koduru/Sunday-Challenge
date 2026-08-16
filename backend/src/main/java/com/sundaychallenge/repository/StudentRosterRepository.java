package com.sundaychallenge.repository;

import com.sundaychallenge.entity.StudentRoster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRosterRepository extends JpaRepository<StudentRoster, Long> {

    Optional<StudentRoster> findByRollNumberIgnoreCase(String rollNumber);

    Optional<StudentRoster> findByEmailIgnoreCase(String email);

    boolean existsByRollNumberIgnoreCase(String rollNumber);

    boolean existsByEmailIgnoreCase(String email);

    List<StudentRoster> findAllByOrderByRollNumberAsc();
}
