package com.support.tickets.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.support.tickets.dto.BulkImportResponse;
import com.support.tickets.dto.TicketResponse;
import com.support.tickets.model.TicketStatus;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ImportJsonTest {

    @Mock
    private TicketService ticketService;

    private ImportService importService;

    @BeforeEach
    void setUp() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        importService = new ImportService(ticketService, validator, mapper);
        when(ticketService.createTicket(any())).thenReturn(buildResponse());
    }

    // 1. Valid JSON array → all records imported
    @Test
    void validJsonArray_allImported() {
        String json = """
            [
              {
                "customerId": "C1",
                "customerEmail": "a@example.com",
                "customerName": "Alice",
                "subject": "Login issue",
                "description": "Cannot login for the past two days despite using the correct password."
              },
              {
                "customerId": "C2",
                "customerEmail": "b@example.com",
                "customerName": "Bob",
                "subject": "Billing error",
                "description": "I was charged twice for the same subscription this month."
              }
            ]
            """;
        BulkImportResponse result = importService.importJson(toStream(json));
        assertThat(result.getTotal()).isEqualTo(2);
        assertThat(result.getSuccessful()).isEqualTo(2);
        assertThat(result.getFailed()).isEqualTo(0);
    }

    // 2. JSON array with one invalid record → partial success
    @Test
    void jsonWithInvalidRecord_partialSuccess() {
        String json = """
            [
              {
                "customerId": "C1",
                "customerEmail": "good@example.com",
                "customerName": "Good User",
                "subject": "Valid subject",
                "description": "Valid description that is long enough for the test."
              },
              {
                "customerId": "C2",
                "customerEmail": "bad-email-here",
                "customerName": "Bad User",
                "subject": "Invalid record",
                "description": "This record has an invalid email and should fail."
              }
            ]
            """;
        BulkImportResponse result = importService.importJson(toStream(json));
        assertThat(result.getSuccessful()).isEqualTo(1);
        assertThat(result.getFailed()).isEqualTo(1);
    }

    // 3. Malformed JSON → meaningful error
    @Test
    void malformedJson_returnsError() {
        String json = "[ { broken json here ";
        BulkImportResponse result = importService.importJson(toStream(json));
        assertThat(result.getFailed()).isEqualTo(1);
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getMessage()).contains("Malformed JSON");
    }

    // 4. Empty JSON array → 0 records
    @Test
    void emptyJsonArray_zeroRecords() {
        BulkImportResponse result = importService.importJson(toStream("[]"));
        assertThat(result.getTotal()).isEqualTo(0);
        assertThat(result.getSuccessful()).isEqualTo(0);
    }

    // 5. JSON object instead of array → parse error
    @Test
    void jsonObject_notArray_returnsError() {
        String json = """
            {
              "customerId": "C1",
              "customerEmail": "a@example.com",
              "customerName": "Alice",
              "subject": "Test",
              "description": "A description that is certainly long enough."
            }
            """;
        BulkImportResponse result = importService.importJson(toStream(json));
        assertThat(result.getFailed()).isEqualTo(1);
        assertThat(result.getErrors()).hasSize(1);
    }

    private ByteArrayInputStream toStream(String text) {
        return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
    }

    private TicketResponse buildResponse() {
        TicketResponse r = new TicketResponse();
        r.setId(UUID.randomUUID());
        r.setStatus(TicketStatus.NEW);
        r.setCreatedAt(LocalDateTime.now());
        r.setUpdatedAt(LocalDateTime.now());
        return r;
    }
}
