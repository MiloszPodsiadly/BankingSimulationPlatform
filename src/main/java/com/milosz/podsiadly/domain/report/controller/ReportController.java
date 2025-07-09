package com.milosz.podsiadly.domain.report.controller;


import com.milosz.podsiadly.domain.report.dto.*;
import com.milosz.podsiadly.domain.report.service.ReportGenerator;
import com.milosz.podsiadly.domain.report.service.ReportSubscriptionService;
import com.milosz.podsiadly.common.exception.ReportGenerationException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Slf4j
public class ReportController {

    private final ReportGenerator reportGenerator;
    private final ReportSubscriptionService reportSubscriptionService;

    /**
     * Endpoint to generate various financial and audit reports.
     * The type of report and its parameters are specified in the request body.
     * @param requestDto The DTO containing report generation parameters.
     * @return The generated report DTO, cast to specific type based on request.
     */
    @PostMapping("/generate")
    public ResponseEntity<?> generateReport(@Valid @RequestBody ReportRequestDto requestDto) {
        log.info("Received request to generate report: {}", requestDto);
        try {
            Object report = reportGenerator.generateReport(requestDto);
            // Return specific DTO type based on ReportType
            return ResponseEntity.ok(report);
        } catch (ReportGenerationException e) {
            log.error("Error generating report: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("An unexpected error occurred during report generation: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred.");
        }
    }

    // --- Report Subscription Endpoints ---

    /**
     * Endpoint to create a new report subscription.
     * @param subscriptionRequest The DTO containing subscription details.
     * @return The created ReportSubscription record.
     */
    @PostMapping("/subscriptions")
    public ResponseEntity<ReportSubscriptionService.ReportSubscription> createReportSubscription(
            @Valid @RequestBody ReportSubscriptionRequest subscriptionRequest) {
        log.info("Received request to create report subscription for user: {}", subscriptionRequest.userId());
        ReportSubscriptionService.ReportSubscription subscription = reportSubscriptionService.createSubscription(
                subscriptionRequest.userId(),
                subscriptionRequest.reportRequestDto(),
                subscriptionRequest.frequency(),
                subscriptionRequest.deliveryMethod()
        );
        return new ResponseEntity<>(subscription, HttpStatus.CREATED);
    }

    /**
     * Endpoint to retrieve all active report subscriptions.
     * @return A list of ReportSubscription records.
     */
    @GetMapping("/subscriptions")
    public ResponseEntity<List<ReportSubscriptionService.ReportSubscription>> getAllReportSubscriptions() {
        log.info("Received request to get all report subscriptions.");
        List<ReportSubscriptionService.ReportSubscription> subscriptions = reportSubscriptionService.getAllSubscriptions();
        return ResponseEntity.ok(subscriptions);
    }

    // You might also add endpoints for:
    // - Getting a specific subscription by ID
    // - Updating a subscription
    // - Deleting a subscription
    // - Triggering a scheduled report manually (for testing/admin)
}