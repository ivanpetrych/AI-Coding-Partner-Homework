package com.support.tickets.service;

import com.support.tickets.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ClassificationService {

    private static final Logger log = LoggerFactory.getLogger(ClassificationService.class);

    private static final Map<TicketCategory, List<String>> CATEGORY_KEYWORDS = new LinkedHashMap<>();
    private static final List<String> URGENT_KEYWORDS   = List.of("can't access", "cannot access", "critical", "production down", "security", "outage", "down for all");
    private static final List<String> HIGH_KEYWORDS     = List.of("important", "blocking", "asap", "high priority", "urgent");
    private static final List<String> LOW_KEYWORDS      = List.of("minor", "cosmetic", "suggestion", "nice to have", "low priority");

    static {
        CATEGORY_KEYWORDS.put(TicketCategory.ACCOUNT_ACCESS,
                List.of("login", "password", "2fa", "sign in", "sign-in", "locked out", "forgot password", "authentication", "access denied"));
        CATEGORY_KEYWORDS.put(TicketCategory.TECHNICAL_ISSUE,
                List.of("error", "bug", "crash", "not working", "broken", "exception", "failure", "500", "timeout", "server error"));
        CATEGORY_KEYWORDS.put(TicketCategory.BILLING_QUESTION,
                List.of("payment", "invoice", "refund", "charge", "subscription", "billing", "price", "cost", "receipt"));
        CATEGORY_KEYWORDS.put(TicketCategory.FEATURE_REQUEST,
                List.of("feature", "enhancement", "suggestion", "improve", "add support for", "would like", "wish", "could you add"));
        CATEGORY_KEYWORDS.put(TicketCategory.BUG_REPORT,
                List.of("reproduce", "steps to reproduce", "version", "expected behavior", "actual behavior", "regression", "defect"));
    }

    public ClassificationResult classify(String subject, String description) {
        String text = (subject + " " + description).toLowerCase();

        // Category classification
        TicketCategory bestCategory = TicketCategory.OTHER;
        int bestCount = 0;
        List<String> bestKeywords = new ArrayList<>();

        for (Map.Entry<TicketCategory, List<String>> entry : CATEGORY_KEYWORDS.entrySet()) {
            List<String> matched = new ArrayList<>();
            for (String kw : entry.getValue()) {
                if (text.contains(kw)) {
                    matched.add(kw);
                }
            }
            if (matched.size() > bestCount) {
                bestCount = matched.size();
                bestCategory = entry.getKey();
                bestKeywords = matched;
            }
        }

        // Confidence: ratio of matched keywords vs total keywords for winning category
        double confidence;
        if (bestCategory == TicketCategory.OTHER) {
            confidence = 0.1;
        } else {
            int total = CATEGORY_KEYWORDS.get(bestCategory).size();
            confidence = Math.min(1.0, (double) bestCount / total);
        }

        // Priority classification
        TicketPriority priority = TicketPriority.MEDIUM;
        List<String> priorityKeywords = new ArrayList<>();

        for (String kw : URGENT_KEYWORDS) {
            if (text.contains(kw)) {
                priorityKeywords.add(kw);
                priority = TicketPriority.URGENT;
                break;
            }
        }
        if (priority == TicketPriority.MEDIUM) {
            for (String kw : HIGH_KEYWORDS) {
                if (text.contains(kw)) {
                    priorityKeywords.add(kw);
                    priority = TicketPriority.HIGH;
                    break;
                }
            }
        }
        if (priority == TicketPriority.MEDIUM) {
            for (String kw : LOW_KEYWORDS) {
                if (text.contains(kw)) {
                    priorityKeywords.add(kw);
                    priority = TicketPriority.LOW;
                    break;
                }
            }
        }

        List<String> allKeywords = new ArrayList<>(bestKeywords);
        allKeywords.addAll(priorityKeywords);

        String reasoning = buildReasoning(bestCategory, priority, bestKeywords, priorityKeywords);

        log.info("Classification result: category={}, priority={}, confidence={}, keywords={}",
                bestCategory, priority, confidence, allKeywords);

        return new ClassificationResult(bestCategory, priority, confidence, reasoning, allKeywords);
    }

    private String buildReasoning(TicketCategory category, TicketPriority priority,
                                   List<String> categoryKeywords, List<String> priorityKeywords) {
        StringBuilder sb = new StringBuilder();
        if (category == TicketCategory.OTHER) {
            sb.append("No category keywords matched; defaulting to OTHER.");
        } else {
            sb.append("Category '").append(category.getValue())
              .append("' matched by keywords: ").append(categoryKeywords).append(".");
        }
        if (!priorityKeywords.isEmpty()) {
            sb.append(" Priority '").append(priority.getValue())
              .append("' matched by keywords: ").append(priorityKeywords).append(".");
        } else {
            sb.append(" No priority keywords matched; defaulting to MEDIUM.");
        }
        return sb.toString();
    }
}
