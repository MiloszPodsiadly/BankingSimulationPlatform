package com.milosz.podsiadly.domain.risk.controller;

import com.milosz.podsiadly.domain.risk.dto.RiskAlertDto;
import com.milosz.podsiadly.domain.risk.dto.RiskAssessmentDto;
import com.milosz.podsiadly.domain.risk.dto.RiskIndicatorDto;
import com.milosz.podsiadly.domain.risk.dto.RiskReportDto;
import com.milosz.podsiadly.domain.risk.mapper.RiskMapper;
import com.milosz.podsiadly.domain.risk.model.RiskAlert;
import com.milosz.podsiadly.domain.risk.model.RiskAssessment;
import com.milosz.podsiadly.domain.risk.model.RiskIndicator;
import com.milosz.podsiadly.domain.risk.service.RiskCalculationService;
import com.milosz.podsiadly.domain.risk.service.RiskMonitoringService;
import com.milosz.podsiadly.domain.risk.service.RiskReportingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/risk")
@RequiredArgsConstructor
@Slf4j
public class RiskController {

    private final RiskCalculationService riskCalculationService;
    private final RiskMonitoringService riskMonitoringService;
    private final RiskReportingService riskReportingService;
    private final RiskMapper riskMapper;

    // --- Endpoints for Risk Indicators (Management) ---
    @PostMapping("/indicators")
    public ResponseEntity<RiskIndicatorDto> createRiskIndicator(@Valid @RequestBody RiskIndicatorDto indicatorDto) {
        log.info("Request to create or update risk indicator: {}", indicatorDto.indicatorCode());
        RiskIndicator indicator = riskMapper.toRiskIndicatorEntity(indicatorDto);
        RiskIndicator savedIndicator = riskCalculationService.createOrUpdateRiskIndicator(indicator);
        return new ResponseEntity<>(riskMapper.toRiskIndicatorDto(savedIndicator), HttpStatus.CREATED);
    }

    @GetMapping("/indicators/{indicatorCode}")
    public ResponseEntity<RiskIndicatorDto> getRiskIndicatorByCode(@PathVariable String indicatorCode) {
        log.info("Request to get risk indicator by code: {}", indicatorCode);
        return riskCalculationService.getRiskIndicatorByCode(indicatorCode)
                .map(riskMapper::toRiskIndicatorDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @GetMapping("/indicators")
    public ResponseEntity<List<RiskIndicatorDto>> getAllRiskIndicators() {
        log.info("Request to get all risk indicators");
        // Zakładam, że RiskCalculationService ma metodę do pobrania wszystkich wskaźników
        // Dodać w RiskCalculationService: public List<RiskIndicator> getAllRiskIndicators() { return riskIndicatorRepository.findAll(); }
        List<RiskIndicator> indicators = riskCalculationService.getAllRiskIndicators(); // Wymaga dodania metody do serwisu
        return ResponseEntity.ok(riskMapper.toRiskIndicatorDtoList(indicators));
    }


    // --- Endpoints for Risk Assessments ---
    @PostMapping("/assessments/account/{accountId}")
    public ResponseEntity<RiskAssessmentDto> assessAccountRisk(@PathVariable Long accountId) {
        log.info("Request to perform risk assessment for account ID: {}", accountId);
        RiskAssessment assessment = riskCalculationService.performAccountRiskAssessment(accountId);
        riskMonitoringService.monitorRiskAssessment(assessment); // Monitoruj nową ocenę pod kątem alertów
        return new ResponseEntity<>(riskMapper.toRiskAssessmentDto(assessment), HttpStatus.CREATED);
    }

    @PostMapping("/assessments/user/{userId}")
    public ResponseEntity<RiskAssessmentDto> assessUserRisk(@PathVariable Long userId) {
        log.info("Request to perform risk assessment for user ID: {}", userId);
        RiskAssessment assessment = riskCalculationService.performUserRiskAssessment(userId);
        riskMonitoringService.monitorRiskAssessment(assessment); // Monitoruj nową ocenę pod kątem alertów
        return new ResponseEntity<>(riskMapper.toRiskAssessmentDto(assessment), HttpStatus.CREATED);
    }

    @GetMapping("/assessments/{assessmentId}")
    public ResponseEntity<RiskAssessmentDto> getRiskAssessmentById(@PathVariable Long assessmentId) {
        log.info("Request to get risk assessment by ID: {}", assessmentId);
        // Dodać w RiskCalculationService lub RiskReportingService:
        // public Optional<RiskAssessment> getRiskAssessmentById(Long id) { return riskAssessmentRepository.findById(id); }
        return riskCalculationService.getRiskAssessmentById(assessmentId) // Wymaga dodania metody do serwisu
                .map(riskMapper::toRiskAssessmentDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }


    // --- Endpoints for Risk Alerts ---
    @GetMapping("/alerts/open")
    public ResponseEntity<List<RiskAlertDto>> getOpenRiskAlerts() {
        log.info("Request to get all open risk alerts");
        List<RiskAlert> alerts = riskMonitoringService.getOpenRiskAlerts();
        return ResponseEntity.ok(riskMapper.toRiskAlertDtoList(alerts));
    }

    @PutMapping("/alerts/{alertId}/resolve")
    public ResponseEntity<RiskAlertDto> resolveRiskAlert(@PathVariable Long alertId, @RequestParam String resolvedBy) {
        log.info("Request to resolve risk alert ID: {} by {}", alertId, resolvedBy);
        RiskAlert resolvedAlert = riskMonitoringService.resolveRiskAlert(alertId, resolvedBy);
        return ResponseEntity.ok(riskMapper.toRiskAlertDto(resolvedAlert));
    }

    @PutMapping("/alerts/{alertId}/dismiss")
    public ResponseEntity<RiskAlertDto> dismissRiskAlert(@PathVariable Long alertId, @RequestParam String dismissedBy) {
        log.info("Request to dismiss risk alert ID: {} by {}", alertId, dismissedBy);
        RiskAlert dismissedAlert = riskMonitoringService.dismissRiskAlert(alertId, dismissedBy);
        return ResponseEntity.ok(riskMapper.toRiskAlertDto(dismissedAlert));
    }


    // --- Endpoints for Risk Reports ---
    @GetMapping("/reports/comprehensive")
    public ResponseEntity<RiskReportDto> getComprehensiveRiskReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        log.info("Request to generate comprehensive risk report for period {} to {}", startDate, endDate);
        RiskReportDto report = riskReportingService.generateComprehensiveRiskReport(startDate, endDate);
        return ResponseEntity.ok(report);
    }

    // Endpoint do uruchomienia okresowych sprawdzeń (może być uruchamiany przez administratora lub scheduler)
    @PostMapping("/monitor/run-checks")
    public ResponseEntity<String> runPeriodicRiskChecks() {
        log.info("Request to manually trigger periodic risk checks.");
        riskMonitoringService.runPeriodicRiskChecks();
        return ResponseEntity.ok("Periodic risk checks initiated successfully.");
    }
}