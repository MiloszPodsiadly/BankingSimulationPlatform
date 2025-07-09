package com.milosz.podsiadly.domain.compliance.controller;

import com.milosz.podsiadly.domain.compliance.dto.AuditLogEntryDto;
import com.milosz.podsiadly.domain.compliance.dto.AuditReportDto;
import com.milosz.podsiadly.domain.compliance.dto.ComplianceAlertDto;
import com.milosz.podsiadly.domain.compliance.dto.ComplianceReportDto;
import com.milosz.podsiadly.domain.compliance.model.AuditLog;
import com.milosz.podsiadly.domain.compliance.model.ComplianceAlert;
import com.milosz.podsiadly.domain.compliance.mapper.AuditMapper;
import com.milosz.podsiadly.domain.compliance.service.AuditService;
import com.milosz.podsiadly.domain.compliance.service.ComplianceMonitoringService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/compliance")
@RequiredArgsConstructor
public class ComplianceController {

    private final AuditService auditService;
    private final ComplianceMonitoringService complianceMonitoringService;
    private final AuditMapper auditMapper; // Inject the MapStruct mapper

    // --- Audit Log Endpoints ---

    /**
     * Endpoint to manually log an audit event.
     * In a real system, most audit events would be logged internally by services,
     * but this can be useful for specific external integrations or testing.
     * @param auditLogEntryDto The DTO representing the audit log entry.
     * @return The saved AuditLogEntryDto.
     */
    @PostMapping("/audit/log")
    public ResponseEntity<AuditLogEntryDto> logAuditEvent(@Valid @RequestBody AuditLogEntryDto auditLogEntryDto) {
        // Note: For logging, 'id' and 'timestamp' might not be provided in DTO,
        // and 'status' might be inferred or set by the service.
        // For simplicity, we'll map directly, but typically you'd build the entity more carefully here.
        AuditLog auditLog = auditService.logEvent(
                auditLogEntryDto.username(),
                auditLogEntryDto.action(),
                auditLogEntryDto.entityType(),
                auditLogEntryDto.entityId(),
                auditLogEntryDto.details(),
                auditLogEntryDto.status()
        );
        return new ResponseEntity<>(auditMapper.toDto(auditLog), HttpStatus.CREATED);
    }

    /**
     * Retrieves all audit logs, with optional filtering by status.
     * @param status Optional status to filter audit logs (e.g., SUCCESS, FAILURE).
     * @return A list of AuditLogEntryDto.
     */
    @GetMapping("/audit/logs")
    public ResponseEntity<List<AuditLogEntryDto>> getAllAuditLogs(@RequestParam(required = false) AuditLog.AuditStatus status) {
        List<AuditLog> auditLogs;
        if (status != null) {
            auditLogs = auditService.getAuditLogsByStatus(status);
        } else {
            auditLogs = auditService.getAllAuditLogs();
        }
        return ResponseEntity.ok(auditMapper.toDtoList(auditLogs));
    }

    /**
     * Retrieves audit logs for a specific user.
     * @param username The username to filter by.
     * @return A list of AuditLogEntryDto for the given user.
     */
    @GetMapping("/audit/logs/user/{username}")
    public ResponseEntity<List<AuditLogEntryDto>> getAuditLogsByUser(@PathVariable String username) {
        List<AuditLog> auditLogs = auditService.getAuditLogsByUsername(username);
        return ResponseEntity.ok(auditMapper.toDtoList(auditLogs));
    }

    /**
     * Generates a comprehensive audit report for a given period.
     * @param startDate The start date/time for the report period (e.g., "2023-01-01T00:00:00").
     * @param endDate The end date/time for the report period.
     * @return An AuditReportDto containing aggregated audit data.
     */
    @GetMapping("/audit/report")
    public ResponseEntity<AuditReportDto> getAuditReport(
            @RequestParam LocalDateTime startDate,
            @RequestParam LocalDateTime endDate) {

        List<AuditLog> entriesInPeriod = auditService.getAuditLogsByTimestampRange(startDate, endDate);

        long totalEntries = entriesInPeriod.size();
        long successfulEntries = entriesInPeriod.stream().filter(log -> log.getStatus() == AuditLog.AuditStatus.SUCCESS).count();
        long failedEntries = totalEntries - successfulEntries;

        Map<String, Long> entriesByActionType = entriesInPeriod.stream()
                .collect(Collectors.groupingBy(AuditLog::getAction, Collectors.counting()));

        // Example: Get critical failed actions (you might define "critical" based on action type or other criteria)
        List<AuditLogEntryDto> criticalFailedActions = entriesInPeriod.stream()
                .filter(log -> log.getStatus() == AuditLog.AuditStatus.FAILURE &&
                        (log.getAction().contains("LOGIN") || log.getAction().contains("TRANSFER"))) // Example filter
                .map(auditMapper::toDto)
                .collect(Collectors.toList());

        AuditReportDto report = new AuditReportDto(
                LocalDateTime.now(),
                startDate,
                endDate,
                totalEntries,
                successfulEntries,
                failedEntries,
                entriesByActionType,
                criticalFailedActions,
                auditMapper.toDtoList(entriesInPeriod) // Include all entries for detailed report
        );

        return ResponseEntity.ok(report);
    }

    // --- Compliance Alert Endpoints ---

    /**
     * Retrieves all compliance alerts, with optional filtering by status or severity.
     * @param status Optional alert status to filter by (e.g., OPEN, RESOLVED, DISMISSED).
     * @param severity Optional alert severity to filter by (e.g., CRITICAL, HIGH, MEDIUM, LOW).
     * @return A list of ComplianceAlertDto.
     */
    @GetMapping("/alerts")
    public ResponseEntity<List<ComplianceAlertDto>> getComplianceAlerts(
            @RequestParam(required = false) ComplianceAlert.AlertStatus status,
            @RequestParam(required = false) ComplianceAlert.AlertSeverity severity) {

        List<ComplianceAlert> alerts;
        if (status != null) {
            alerts = complianceMonitoringService.getAlertsByStatus(status);
        } else if (severity != null) {
            alerts = complianceMonitoringService.getAlertsBySeverity(severity);
        } else {
            alerts = complianceMonitoringService.getAllAlerts();
        }

        // Map ComplianceAlert entities to ComplianceAlertDto records
        List<ComplianceAlertDto> alertDtos = alerts.stream()
                .map(this::mapComplianceAlertToDto) // Use a helper method for this mapping
                .collect(Collectors.toList());

        return ResponseEntity.ok(alertDtos);
    }

    /**
     * Resolves a specific compliance alert.
     * @param alertId The ID of the alert to resolve.
     * @param resolvedBy The username of the person resolving the alert.
     * @return The updated ComplianceAlertDto.
     */
    @PutMapping("/alerts/{alertId}/resolve")
    public ResponseEntity<ComplianceAlertDto> resolveComplianceAlert(
            @PathVariable Long alertId,
            @RequestParam String resolvedBy) {
        ComplianceAlert resolvedAlert = complianceMonitoringService.resolveAlert(alertId, resolvedBy);
        if (resolvedAlert != null) {
            return ResponseEntity.ok(mapComplianceAlertToDto(resolvedAlert));
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Dismisses a specific compliance alert.
     * @param alertId The ID of the alert to dismiss.
     * @param dismissedBy The username of the person dismissing the alert.
     * @return The updated ComplianceAlertDto.
     */
    @PutMapping("/alerts/{alertId}/dismiss")
    public ResponseEntity<ComplianceAlertDto> dismissComplianceAlert(
            @PathVariable Long alertId,
            @RequestParam String dismissedBy) {
        ComplianceAlert dismissedAlert = complianceMonitoringService.dismissAlert(alertId, dismissedBy);
        if (dismissedAlert != null) {
            return ResponseEntity.ok(mapComplianceAlertToDto(dismissedAlert));
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Helper method to map ComplianceAlert entity to ComplianceAlertDto record.
     * This would typically be in a dedicated MapStruct mapper, but for a single DTO,
     * it can be defined here or as a static helper.
     * For full MapStruct power, you'd create a `ComplianceAlertMapper` interface.
     * @param alert The ComplianceAlert entity.
     * @return The corresponding ComplianceAlertDto record.
     */
    private ComplianceAlertDto mapComplianceAlertToDto(ComplianceAlert alert) {
        return new ComplianceAlertDto(
                alert.getId(),
                alert.getAlertCode(),
                alert.getDescription(),
                alert.getSeverity(),
                alert.getStatus(),
                alert.getTriggeredByEntityType(),
                alert.getTriggeredByEntityId(),
                alert.getRelatedDetails(),
                alert.getCreatedAt(),
                alert.getResolvedAt(),
                alert.getResolvedBy()
        );
    }
}