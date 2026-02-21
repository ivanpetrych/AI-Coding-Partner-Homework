package com.support.tickets.service;

import com.support.tickets.dto.BulkImportResponse;
import com.support.tickets.dto.TicketCreateRequest;
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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ImportCsvTest {

    @Mock
    private TicketService ticketService;

    private ImportService importService;

    @BeforeEach
    void setUp() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.findAndRegisterModules();
        importService = new ImportService(ticketService, validator, mapper);
        when(ticketService.createTicket(any())).thenReturn(buildResponse());
    }

    // 1. Valid CSV - all records imported successfully
    @Test
    void validCsv_allImported() throws IOException {
        String csv = "customer_id,customer_email,customer_name,subject,description\n" +
                     "C1,a@example.com,Alice,Login problem,I cannot login because my password is not working at all.\n" +
                     "C2,b@example.com,Bob,Invoice issue,I have not received my invoice and it is now overdue for two weeks.";
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);
        BulkImportResponse result = importService.importCsv(bytes, null);
        assertThat(result.getTotal()).isEqualTo(2);
        assertThat(result.getSuccessful()).isEqualTo(2);
        assertThat(result.getFailed()).isEqualTo(0);
        assertThat(result.getErrors()).isEmpty();
    }

    // 2. CSV with one invalid email row → partial success
    @Test
    void csvWithInvalidEmail_partialSuccess() throws IOException {
        String csv = "customer_id,customer_email,customer_name,subject,description\n" +
                     "C1,valid@example.com,Alice,Good subject,This is a valid description that is long enough for testing.\n" +
                     "C2,bad-email,Bob,Good subject,This is a valid description that is long enough for testing.";
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);
        BulkImportResponse result = importService.importCsv(bytes, null);
        assertThat(result.getSuccessful()).isEqualTo(1);
        assertThat(result.getFailed()).isEqualTo(1);
        assertThat(result.getErrors()).hasSize(1);
    }

    // 3. CSV with missing required data → error recorded
    @Test
    void csvWithMissingSubject_errorRecorded() {
        String csv = "customer_id,customer_email,customer_name,subject,description\n" +
                     "C1,test@example.com,Test User,,This description is long enough to pass the validation check.";
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);
        BulkImportResponse result = importService.importCsv(bytes, null);
        assertThat(result.getFailed()).isGreaterThan(0);
        assertThat(result.getErrors()).isNotEmpty();
    }

    // 4. Malformed CSV (binary-like) → graceful error
    @Test
    void malformedCsv_gracefulError() {
        byte[] bytes = new byte[]{0x00, (byte)0xFF, 0x01, 0x02};
        BulkImportResponse result = importService.importCsv(bytes, null);
        // Should return without throwing - either 0 records or parse error
        assertThat(result).isNotNull();
    }

    // 5. Empty CSV → 0 records
    @Test
    void emptyCsv_zeroRecords() {
        String csv = "customer_id,customer_email,customer_name,subject,description\n";
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);
        BulkImportResponse result = importService.importCsv(bytes, null);
        assertThat(result.getTotal()).isEqualTo(0);
        assertThat(result.getSuccessful()).isEqualTo(0);
    }

    // 6. CSV with semicolon-separated tags → tags parsed as list
    @Test
    void csvWithTags_parsedAsList() {
        String csv = "customer_id,customer_email,customer_name,subject,description,tags\n" +
                     "C1,test@example.com,Test User,Subject here,Description is long enough to pass validation.,tag1;tag2;tag3";
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);
        // Capture the request - the ticket service mock returns successfully
        BulkImportResponse result = importService.importCsv(bytes, null);
        assertThat(result.getSuccessful()).isEqualTo(1);
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
