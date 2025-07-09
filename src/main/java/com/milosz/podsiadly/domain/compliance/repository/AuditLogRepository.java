package com.milosz.podsiadly.domain.compliance.repository;

import com.milosz.podsiadly.domain.compliance.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    // Custom query to find audit logs by username
    List<AuditLog> findByUsername(String username);

    // Custom query to find audit logs by action type
    List<AuditLog> findByAction(String action);

    // Custom query to find audit logs by entity type and ID
    List<AuditLog> findByEntityTypeAndEntityId(String entityType, Long entityId);

    // Custom query to find audit logs within a specific time range
    List<AuditLog> findByTimestampBetween(LocalDateTime startDate, LocalDateTime endDate);

    // Custom query to find audit logs by status
    List<AuditLog> findByStatus(AuditLog.AuditStatus status);

    // More complex query example: Find all failed transactions for a specific user
    List<AuditLog> findByUsernameAndActionContainingAndStatus(String username, String actionKeyword, AuditLog.AuditStatus status);
}
