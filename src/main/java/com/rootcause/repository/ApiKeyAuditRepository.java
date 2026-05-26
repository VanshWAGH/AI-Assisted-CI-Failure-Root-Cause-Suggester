package com.rootcause.repository;

import com.rootcause.domain.ApiKeyAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ApiKeyAuditRepository extends JpaRepository<ApiKeyAudit, UUID> {
}
