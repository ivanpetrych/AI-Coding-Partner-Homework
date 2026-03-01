package com.support.tickets.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import com.support.tickets.dto.*;
import com.support.tickets.model.*;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.*;

@Service
public class ImportService {

    private static final Logger log = LoggerFactory.getLogger(ImportService.class);

    private final TicketService ticketService;
    private final Validator validator;
    private final ObjectMapper jsonMapper;
    private final XmlMapper xmlMapper;

    public ImportService(TicketService ticketService, Validator validator, ObjectMapper jsonMapper) {
        this.ticketService = ticketService;
        this.validator = validator;
        this.jsonMapper = jsonMapper;
        this.xmlMapper = new XmlMapper();
    }

    public BulkImportResponse importFile(MultipartFile file) {
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        String contentType = file.getContentType() != null ? file.getContentType().toLowerCase() : "";

        try {
            if (filename.endsWith(".csv") || contentType.contains("csv") || contentType.contains("text/plain")) {
                return importCsv(file.getBytes(), file.getInputStream());
            } else if (filename.endsWith(".json") || contentType.contains("json")) {
                return importJson(file.getInputStream());
            } else if (filename.endsWith(".xml") || contentType.contains("xml")) {
                return importXml(file.getInputStream());
            } else {
                // Try to detect from content
                byte[] bytes = file.getBytes();
                String content = new String(bytes).stripLeading();
                if (content.startsWith("[") || content.startsWith("{")) {
                    return importJson(new ByteArrayInputStream(bytes));
                } else if (content.startsWith("<")) {
                    return importXml(new ByteArrayInputStream(bytes));
                } else {
                    return importCsv(bytes, new ByteArrayInputStream(bytes));
                }
            }
        } catch (IOException e) {
            log.error("Failed to read import file", e);
            ImportError err = new ImportError(0, "file", "Failed to read file: " + e.getMessage());
            return new BulkImportResponse(0, 0, 1, List.of(err));
        }
    }

    // ---- CSV ----

    public BulkImportResponse importCsv(byte[] bytes, InputStream stream) {
        List<ImportError> errors = new ArrayList<>();
        int successful = 0;
        int row = 1;

        try (CSVReader reader = new CSVReader(new InputStreamReader(new ByteArrayInputStream(bytes)))) {
            List<String[]> all;
            try {
                all = reader.readAll();
            } catch (CsvException e) {
                return new BulkImportResponse(0, 0, 1,
                        List.of(new ImportError(0, "file", "Malformed CSV: " + e.getMessage())));
            }

            if (all.isEmpty()) {
                return new BulkImportResponse(0, 0, 0, errors);
            }

            String[] headers = all.get(0);
            Map<String, Integer> headerIndex = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                headerIndex.put(headers[i].trim().toLowerCase(), i);
            }

            for (int i = 1; i < all.size(); i++) {
                row = i + 1;
                String[] cols = all.get(i);
                try {
                    TicketCreateRequest req = mapCsvRow(cols, headerIndex);
                    Set<ConstraintViolation<TicketCreateRequest>> violations = validator.validate(req);
                    if (!violations.isEmpty()) {
                        for (ConstraintViolation<TicketCreateRequest> v : violations) {
                            errors.add(new ImportError(row, v.getPropertyPath().toString(), v.getMessage()));
                        }
                    } else {
                        ticketService.createTicket(req);
                        successful++;
                    }
                } catch (Exception e) {
                    errors.add(new ImportError(row, "row", e.getMessage()));
                }
            }

            int total = all.size() - 1;
            return new BulkImportResponse(total, successful, total - successful, errors);

        } catch (IOException e) {
            return new BulkImportResponse(0, 0, 1,
                    List.of(new ImportError(0, "file", "Cannot read CSV: " + e.getMessage())));
        }
    }

    private TicketCreateRequest mapCsvRow(String[] cols, Map<String, Integer> idx) {
        TicketCreateRequest req = new TicketCreateRequest();
        req.setCustomerId(get(cols, idx, "customer_id"));
        req.setCustomerEmail(get(cols, idx, "customer_email"));
        req.setCustomerName(get(cols, idx, "customer_name"));
        req.setSubject(get(cols, idx, "subject"));
        req.setDescription(get(cols, idx, "description"));

        String cat = get(cols, idx, "category");
        if (cat != null && !cat.isEmpty()) req.setCategory(TicketCategory.fromValue(cat));

        String pri = get(cols, idx, "priority");
        if (pri != null && !pri.isEmpty()) req.setPriority(TicketPriority.fromValue(pri));

        String sta = get(cols, idx, "status");
        if (sta != null && !sta.isEmpty()) req.setStatus(TicketStatus.fromValue(sta));

        req.setAssignedTo(get(cols, idx, "assigned_to"));

        String tagsRaw = get(cols, idx, "tags");
        if (tagsRaw != null && !tagsRaw.isEmpty()) {
            req.setTags(Arrays.asList(tagsRaw.split(";")));
        }
        return req;
    }

    private String get(String[] cols, Map<String, Integer> idx, String key) {
        Integer i = idx.get(key);
        if (i == null || i >= cols.length) return null;
        String val = cols[i].trim();
        return val.isEmpty() ? null : val;
    }

    // ---- JSON ----

    public BulkImportResponse importJson(InputStream stream) {
        List<ImportError> errors = new ArrayList<>();
        int successful = 0;
        List<TicketCreateRequest> requests;

        try {
            requests = jsonMapper.readValue(stream, new TypeReference<List<TicketCreateRequest>>() {});
        } catch (IOException e) {
            return new BulkImportResponse(0, 0, 1,
                    List.of(new ImportError(0, "file", "Malformed JSON: " + e.getMessage())));
        }

        for (int i = 0; i < requests.size(); i++) {
            TicketCreateRequest req = requests.get(i);
            Set<ConstraintViolation<TicketCreateRequest>> violations = validator.validate(req);
            if (!violations.isEmpty()) {
                for (ConstraintViolation<TicketCreateRequest> v : violations) {
                    errors.add(new ImportError(i + 1, v.getPropertyPath().toString(), v.getMessage()));
                }
            } else {
                try {
                    ticketService.createTicket(req);
                    successful++;
                } catch (Exception e) {
                    errors.add(new ImportError(i + 1, "row", e.getMessage()));
                }
            }
        }

        return new BulkImportResponse(requests.size(), successful, requests.size() - successful, errors);
    }

    // ---- XML ----

    public BulkImportResponse importXml(InputStream stream) {
        List<ImportError> errors = new ArrayList<>();
        int successful = 0;
        TicketsXmlWrapper wrapper;

        try {
            wrapper = xmlMapper.readValue(stream, TicketsXmlWrapper.class);
        } catch (IOException e) {
            return new BulkImportResponse(0, 0, 1,
                    List.of(new ImportError(0, "file", "Malformed XML: " + e.getMessage())));
        }

        List<TicketCreateRequest> requests = wrapper.getTickets();
        if (requests == null) requests = Collections.emptyList();

        for (int i = 0; i < requests.size(); i++) {
            TicketCreateRequest req = requests.get(i);
            Set<ConstraintViolation<TicketCreateRequest>> violations = validator.validate(req);
            if (!violations.isEmpty()) {
                for (ConstraintViolation<TicketCreateRequest> v : violations) {
                    errors.add(new ImportError(i + 1, v.getPropertyPath().toString(), v.getMessage()));
                }
            } else {
                try {
                    ticketService.createTicket(req);
                    successful++;
                } catch (Exception e) {
                    errors.add(new ImportError(i + 1, "row", e.getMessage()));
                }
            }
        }

        return new BulkImportResponse(requests.size(), successful, requests.size() - successful, errors);
    }

    // ---- XML wrapper ----

    @JacksonXmlRootElement(localName = "TicketsXmlWrapper")
    public static class TicketsXmlWrapper {
        @JacksonXmlElementWrapper(useWrapping = false)
        private List<TicketCreateRequest> tickets;

        public List<TicketCreateRequest> getTickets() { return tickets; }
        public void setTickets(List<TicketCreateRequest> tickets) { this.tickets = tickets; }
    }
}
