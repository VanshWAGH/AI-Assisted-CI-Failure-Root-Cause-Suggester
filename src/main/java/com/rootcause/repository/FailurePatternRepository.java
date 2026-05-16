package com.rootcause.repository;

import com.rootcause.domain.FailurePattern;
import com.rootcause.domain.enums.FailureType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FailurePatternRepository extends JpaRepository<FailurePattern, UUID> {

    List<FailurePattern> findByActiveTrueOrderByPriorityDesc();

    List<FailurePattern> findByFailureTypeAndActiveTrue(FailureType failureType);

    long countByActiveTrue();
}
