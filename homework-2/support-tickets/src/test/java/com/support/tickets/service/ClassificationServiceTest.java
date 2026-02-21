package com.support.tickets.service;

import com.support.tickets.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClassificationServiceTest {

    private ClassificationService classificationService;

    @BeforeEach
    void setUp() {
        classificationService = new ClassificationService();
    }

    // 1. "can't access" → URGENT + ACCOUNT_ACCESS
    @Test
    void cantAccess_returnsUrgentAccountAccess() {
        ClassificationResult result = classificationService.classify(
                "I can't access my account", "Please help me login");
        assertThat(result.getPriority()).isEqualTo(TicketPriority.URGENT);
        assertThat(result.getCategory()).isEqualTo(TicketCategory.ACCOUNT_ACCESS);
    }

    // 2. "production down" → URGENT
    @Test
    void productionDown_returnsUrgent() {
        ClassificationResult result = classificationService.classify(
                "Production down for all users", "The entire production environment is unavailable.");
        assertThat(result.getPriority()).isEqualTo(TicketPriority.URGENT);
    }

    // 3. "invoice" and "refund" → BILLING_QUESTION with confidence > 0
    @Test
    void invoiceAndRefund_returnsBillingQuestion() {
        ClassificationResult result = classificationService.classify(
                "Invoice and refund request", "I need a refund because my billing invoice is incorrect.");
        assertThat(result.getCategory()).isEqualTo(TicketCategory.BILLING_QUESTION);
        assertThat(result.getConfidence()).isGreaterThan(0.0);
    }

    // 4. "feature" and "enhancement" → FEATURE_REQUEST
    @Test
    void featureAndEnhancement_returnsFeatureRequest() {
        ClassificationResult result = classificationService.classify(
                "Feature request: enhancement to dashboard",
                "Would like to see this improvement added to the product.");
        assertThat(result.getCategory()).isEqualTo(TicketCategory.FEATURE_REQUEST);
    }

    // 5. "reproduce" and "expected behavior" → BUG_REPORT
    @Test
    void reproduceAndExpectedBehavior_returnsBugReport() {
        ClassificationResult result = classificationService.classify(
                "Bug: steps to reproduce the crash",
                "Expected behavior: app works. Actual behavior: crash. Version 2.1.");
        assertThat(result.getCategory()).isEqualTo(TicketCategory.BUG_REPORT);
    }

    // 6. "cosmetic change" → LOW priority
    @Test
    void cosmeticChange_returnsLowPriority() {
        ClassificationResult result = classificationService.classify(
                "Minor cosmetic change request",
                "The button color is slightly off in some browsers. This is a minor issue.");
        assertThat(result.getPriority()).isEqualTo(TicketPriority.LOW);
    }

    // 7. "blocking" in subject → HIGH priority
    @Test
    void blockingKeyword_returnsHighPriority() {
        ClassificationResult result = classificationService.classify(
                "Blocking issue with the deployment",
                "This is blocking our team from releasing the new feature we need today.");
        assertThat(result.getPriority()).isEqualTo(TicketPriority.HIGH);
    }

    // 8. No matching keywords → OTHER, low confidence
    @Test
    void noKeywords_returnsOtherWithLowConfidence() {
        ClassificationResult result = classificationService.classify(
                "Hello", "I have a general question about your company.");
        assertThat(result.getCategory()).isEqualTo(TicketCategory.OTHER);
        assertThat(result.getConfidence()).isLessThanOrEqualTo(0.2);
    }

    // 9. Multiple categories matched → highest-count wins
    @Test
    void multipleCategoriesMatched_highestCountWins() {
        // billing keywords: payment, invoice, refund, charge, subscription, billing
        ClassificationResult result = classificationService.classify(
                "Payment invoice refund billing",
                "I need help with my subscription charge and billing payment.");
        assertThat(result.getCategory()).isEqualTo(TicketCategory.BILLING_QUESTION);
    }

    // 10. Keywords found list is populated
    @Test
    void classification_keywordsFoundPopulated() {
        ClassificationResult result = classificationService.classify(
                "Login error exception",
                "I get an error and exception when I try to login to the system.");
        assertThat(result.getKeywordsFound()).isNotEmpty();
    }
}
