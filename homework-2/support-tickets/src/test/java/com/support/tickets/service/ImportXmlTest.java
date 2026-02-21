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
class ImportXmlTest {

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

    // 1. Valid XML → all records imported
    @Test
    void validXml_allImported() {
        String xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <TicketsXmlWrapper>
              <tickets>
                <customerId>C1</customerId>
                <customerEmail>alice@example.com</customerEmail>
                <customerName>Alice</customerName>
                <subject>Login problem from XML</subject>
                <description>I cannot access my account and my password reset is not working either.</description>
              </tickets>
              <tickets>
                <customerId>C2</customerId>
                <customerEmail>bob@example.com</customerEmail>
                <customerName>Bob</customerName>
                <subject>Billing XML import</subject>
                <description>My invoice was incorrect and I need a refund for the overcharged amount.</description>
              </tickets>
            </TicketsXmlWrapper>
            """;
        BulkImportResponse result = importService.importXml(toStream(xml));
        assertThat(result.getTotal()).isEqualTo(2);
        assertThat(result.getSuccessful()).isEqualTo(2);
        assertThat(result.getFailed()).isEqualTo(0);
    }

    // 2. XML with one invalid record → partial success
    @Test
    void xmlWithInvalidRecord_partialSuccess() {
        String xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <TicketsXmlWrapper>
              <tickets>
                <customerId>C1</customerId>
                <customerEmail>valid@example.com</customerEmail>
                <customerName>Valid User</customerName>
                <subject>Valid subject</subject>
                <description>Valid description that has enough characters to pass validation rules.</description>
              </tickets>
              <tickets>
                <customerId>C2</customerId>
                <customerEmail>not-valid-email</customerEmail>
                <customerName>Invalid User</customerName>
                <subject>Invalid XML record</subject>
                <description>This record has an invalid email format and should fail validation.</description>
              </tickets>
            </TicketsXmlWrapper>
            """;
        BulkImportResponse result = importService.importXml(toStream(xml));
        assertThat(result.getSuccessful()).isEqualTo(1);
        assertThat(result.getFailed()).isEqualTo(1);
    }

    // 3. Malformed XML → meaningful error
    @Test
    void malformedXml_returnsError() {
        String xml = "<TicketsXmlWrapper><tickets><customerId>C1</customerId><UNCLOSED>";
        BulkImportResponse result = importService.importXml(toStream(xml));
        assertThat(result.getFailed()).isEqualTo(1);
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getMessage()).contains("Malformed XML");
    }

    // 4. Empty XML wrapper → 0 records
    @Test
    void emptyXml_zeroRecords() {
        String xml = "<TicketsXmlWrapper></TicketsXmlWrapper>";
        BulkImportResponse result = importService.importXml(toStream(xml));
        assertThat(result.getTotal()).isEqualTo(0);
        assertThat(result.getSuccessful()).isEqualTo(0);
    }

    // 5. XML with extra attributes / whitespace → handled gracefully
    @Test
    void xmlWithExtraWhitespace_handledGracefully() {
        String xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <TicketsXmlWrapper>
              <tickets>
                <customerId>  C1  </customerId>
                <customerEmail>  test@example.com  </customerEmail>
                <customerName>  Test User  </customerName>
                <subject>  Subject with whitespace  </subject>
                <description>  Description that is long enough even with whitespace around it.  </description>
              </tickets>
            </TicketsXmlWrapper>
            """;
        BulkImportResponse result = importService.importXml(toStream(xml));
        // Should not throw an exception
        assertThat(result).isNotNull();
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
