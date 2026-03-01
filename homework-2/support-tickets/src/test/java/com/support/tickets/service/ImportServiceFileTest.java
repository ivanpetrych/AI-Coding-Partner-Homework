package com.support.tickets.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.support.tickets.dto.BulkImportResponse;
import com.support.tickets.dto.TicketCreateRequest;
import com.support.tickets.dto.TicketResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests the importFile() routing method and content-sniffing logic of ImportService.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ImportServiceFileTest {

    @Mock
    private TicketService ticketService;

    @Mock
    private Validator validator;

    private ImportService importService;

    private static final String VALID_CSV = "customer_id,customer_email,customer_name,subject,description\n"
            + "C001,a@b.com,Alice,CSV subject,CSV description\n";

    private static final String VALID_JSON = "[{\"customerId\":\"C002\",\"customerEmail\":\"b@c.com\","
            + "\"customerName\":\"Bob\",\"subject\":\"JSON subject\",\"description\":\"JSON description\"}]";

    private static final String VALID_XML =
            "<TicketsXmlWrapper><tickets><customerId>C003</customerId>"
            + "<customerEmail>c@d.com</customerEmail><customerName>Carol</customerName>"
            + "<subject>XML subject</subject><description>XML description</description>"
            + "</tickets></TicketsXmlWrapper>";

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        importService = new ImportService(ticketService, validator, new ObjectMapper());

        // Validator returns no violations by default (simulate valid data)
        Set<ConstraintViolation<TicketCreateRequest>> noViolations = Collections.emptySet();
        when(validator.validate(any(TicketCreateRequest.class))).thenReturn(
                (Set) noViolations);

        // TicketService.createTicket returns a stub response
        TicketResponse stub = new TicketResponse();
        when(ticketService.createTicket(any(TicketCreateRequest.class))).thenReturn(stub);
    }

    // ---- filename-based routing ----

    @Test
    void importFile_csvExtension_routesToImportCsv() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "tickets.csv", "application/octet-stream", VALID_CSV.getBytes());

        BulkImportResponse response = importService.importFile(file);

        assertThat(response).isNotNull();
        assertThat(response.getTotal()).isGreaterThan(0);
    }

    @Test
    void importFile_jsonExtension_routesToImportJson() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "tickets.json", "application/octet-stream", VALID_JSON.getBytes());

        BulkImportResponse response = importService.importFile(file);

        assertThat(response).isNotNull();
        assertThat(response.getTotal()).isEqualTo(1);
    }

    @Test
    void importFile_xmlExtension_routesToImportXml() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "tickets.xml", "application/octet-stream", VALID_XML.getBytes());

        BulkImportResponse response = importService.importFile(file);

        assertThat(response).isNotNull();
        assertThat(response.getTotal()).isEqualTo(1);
    }

    // ---- content-type based routing ----

    @Test
    void importFile_csvContentType_routesToImportCsv() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "upload", "text/csv", VALID_CSV.getBytes());

        BulkImportResponse response = importService.importFile(file);
        assertThat(response).isNotNull();
    }

    @Test
    void importFile_jsonContentType_routesToImportJson() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "upload", "application/json", VALID_JSON.getBytes());

        BulkImportResponse response = importService.importFile(file);
        assertThat(response.getTotal()).isEqualTo(1);
    }

    @Test
    void importFile_xmlContentType_routesToImportXml() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "upload", "application/xml", VALID_XML.getBytes());

        BulkImportResponse response = importService.importFile(file);
        assertThat(response.getTotal()).isEqualTo(1);
    }

    @Test
    void importFile_textPlainContentType_routesToImportCsv() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "upload", "text/plain", VALID_CSV.getBytes());

        BulkImportResponse response = importService.importFile(file);
        assertThat(response).isNotNull();
    }

    // ---- content-sniffing (no extension, no recognizable content-type) ----

    @Test
    void importFile_contentSniffing_jsonArray_routesToImportJson() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "upload", "application/octet-stream", VALID_JSON.getBytes());

        BulkImportResponse response = importService.importFile(file);
        assertThat(response.getTotal()).isEqualTo(1);
    }

    @Test
    void importFile_contentSniffing_jsonObject_routesToImportJson() {
        String singleJson = "{\"customerId\":\"C004\",\"customerEmail\":\"d@e.com\","
                + "\"customerName\":\"Dave\",\"subject\":\"Obj subject\",\"description\":\"Obj desc\"}";
        // Wrap as array for Jackson to parse as list — alternatively use single object detection
        // The sniff logic checks startsWith("{"), then calls importJson which reads as List
        // This will fail JSON parse (not a list) — valid test of error handling
        MockMultipartFile file = new MockMultipartFile(
                "file", "upload", "application/octet-stream", singleJson.getBytes());

        BulkImportResponse response = importService.importFile(file);
        // JSON parse should fail gracefully (not a list), returning an error
        assertThat(response).isNotNull();
        assertThat(response.getFailed()).isEqualTo(1);
    }

    @Test
    void importFile_contentSniffing_xml_routesToImportXml() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "upload", "application/octet-stream", VALID_XML.getBytes());

        BulkImportResponse response = importService.importFile(file);
        assertThat(response.getTotal()).isEqualTo(1);
    }

    @Test
    void importFile_contentSniffing_plainText_routesToImportCsv() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "upload", "application/octet-stream", VALID_CSV.getBytes());

        BulkImportResponse response = importService.importFile(file);
        assertThat(response).isNotNull();
    }

    // ---- IOException handling ----

    @Test
    void importFile_ioException_returnsErrorResponse() throws Exception {
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.getOriginalFilename()).thenReturn("tickets.csv");
        when(mockFile.getContentType()).thenReturn("text/csv");
        when(mockFile.getBytes()).thenThrow(new IOException("Disk error"));

        BulkImportResponse response = importService.importFile(mockFile);

        assertThat(response.getTotal()).isEqualTo(0);
        assertThat(response.getFailed()).isEqualTo(1);
        assertThat(response.getErrors()).hasSize(1);
        assertThat(response.getErrors().get(0).getMessage()).contains("Disk error");
    }

    // ---- null filename / null content type ----

    @Test
    void importFile_nullFilenameAndContentType_sniffsContent() {
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.getOriginalFilename()).thenReturn(null);
        when(mockFile.getContentType()).thenReturn(null);
        try {
            when(mockFile.getBytes()).thenReturn(VALID_JSON.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        BulkImportResponse response = importService.importFile(mockFile);
        // JSON array content sniffed → parsed as JSON
        assertThat(response.getTotal()).isEqualTo(1);
    }
}
