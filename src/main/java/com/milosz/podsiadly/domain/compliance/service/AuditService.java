package com.milosz.podsiadly.domain.compliance.service;

import com.milosz.podsiadly.domain.compliance.model.AuditLog;
import com.milosz.podsiadly.domain.compliance.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Logs an audit event to the database.
     * @param username The user who performed the action.
     * @param action The type of action performed (e.g., "DEPOSIT_CREATED", "LOGIN_SUCCESS").
     * @param entityType The type of entity affected (optional).
     * @param entityId The ID of the entity affected (optional).
     * @param details Additional details about the action (e.g., JSON payload, error message).
     * @param status The status of the action (SUCCESS or FAILURE).
     * @return The saved AuditLog entity.
     */
    @Transactional
    public AuditLog logEvent(String username, String action, String entityType, Long entityId, String details, AuditLog.AuditStatus status) {
        AuditLog auditLog = AuditLog.builder()
                .username(username)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .details(details)
                .status(status)
                .timestamp(LocalDateTime.now()) // Will be overridden by @CreatedDate if JPA Auditing is fully set up
                .build();
        return auditLogRepository.save(auditLog);
    }

    /**
     * Retrieves all audit logs.
     * @return A list of all AuditLog entries.
     */
    @Transactional(readOnly = true)
    public List<AuditLog> getAllAuditLogs() {
        return auditLogRepository.findAll();
    }

    /**
     * Retrieves audit logs for a specific user.
     * @param username The username to filter by.
     * @return A list of AuditLog entries for the given user.
     */
    @Transactional(readOnly = true)
    public List<AuditLog> getAuditLogsByUsername(String username) {
        return auditLogRepository.findByUsername(username);
    }

    /**
     * Retrieves audit logs within a specified time range.
     * @param startDate The start date/time of the range.
     * @param endDate The end date/time of the range.
     * @return A list of AuditLog entries within the specified range.
     */
    @Transactional(readOnly = true)
    public List<AuditLog> getAuditLogsByTimestampRange(LocalDateTime startDate, LocalDateTime endDate) {
        return auditLogRepository.findByTimestampBetween(startDate, endDate);
    }

    /**
     * Retrieves audit logs for a specific entity.
     * @param entityType The type of the entity (e.g., "BankAccount").
     * @param entityId The ID of the entity.
     * @return A list of AuditLog entries related to the specified entity.
     */
    @Transactional(readOnly = true)
    public List<AuditLog> getAuditLogsByEntity(String entityType, Long entityId) {
        return auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId);
    }

    /**
     * Retrieves audit logs by action status (SUCCESS/FAILURE).
     * @param status The status to filter by.
     * @return A list of AuditLog entries with the given status.
     */
    @Transactional(readOnly = true)
    public List<AuditLog> getAuditLogsByStatus(AuditLog.AuditStatus status) {
        return auditLogRepository.findByStatus(status);
    }
}