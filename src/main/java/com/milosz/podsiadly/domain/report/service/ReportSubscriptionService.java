package com.milosz.podsiadly.domain.report.service;

import com.milosz.podsiadly.domain.report.dto.ReportRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// This service would typically interact with a ReportSubscription entity and repository
// For now, let's keep it simple with in-memory subscriptions for demonstration.

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportSubscriptionService {

    private final ReportGenerator reportGenerator;

    // In a real application, these would be persisted in a database
    private final List<ReportSubscription> subscriptions = new ArrayList<>();

    // Inner class to represent a report subscription (could be an entity)
    public record ReportSubscription(
            String id,
            String userId,
            ReportRequestDto reportRequest,
            String frequency, // e.g., "DAILY", "WEEKLY", "MONTHLY"
            String deliveryMethod // e.g., "EMAIL", "SFTP"
            // Last run time, next run time, etc.
    ) {}

    /**
     * Creates a new report subscription.
     * @param userId The ID of the user subscribing.
     * @param requestDto The report request details.
     * @param frequency How often the report should be generated.
     * @param deliveryMethod How the report should be delivered.
     * @return The created ReportSubscription.
     */
    public ReportSubscription createSubscription(String userId, ReportRequestDto requestDto, String frequency, String deliveryMethod) {
        ReportSubscription subscription = new ReportSubscription(
                UUID.randomUUID().toString(),
                userId,
                requestDto,
                frequency,
                deliveryMethod
        );
        subscriptions.add(subscription);
        log.info("New report subscription created for user {}: type={}, frequency={}, delivery={}",
                userId, requestDto.reportType(), frequency, deliveryMethod);
        return subscription;
    }

    /**
     * A scheduled task to process active report subscriptions.
     * This uses Spring's `@Scheduled` annotation.
     * (e.g., every day at midnight, run this job)
     */
    @Scheduled(cron = "0 0 0 * * ?") // Runs every day at midnight
    public void processScheduledReports() {
        log.info("Starting scheduled report generation for {} subscriptions...", subscriptions.size());
        for (ReportSubscription subscription : subscriptions) {
            try {
                log.info("Generating scheduled report for subscription ID: {}", subscription.id());
                Object reportContent = reportGenerator.generateReport(subscription.reportRequest());
                // In a real system, here you'd handle the delivery of 'reportContent'
                // based on 'subscription.deliveryMethod()'.
                log.info("Successfully generated and (simulated) delivered scheduled report for subscription ID: {}", subscription.id());
            } catch (Exception e) {
                log.error("Failed to generate or deliver scheduled report for subscription ID: {}: {}", subscription.id(), e.getMessage(), e);
                // Log failure, potentially update subscription status
            }
        }
        log.info("Finished scheduled report generation.");
    }

    /**
     * Retrieves all active subscriptions.
     */
    public List<ReportSubscription> getAllSubscriptions() {
        return new ArrayList<>(subscriptions);
    }
}