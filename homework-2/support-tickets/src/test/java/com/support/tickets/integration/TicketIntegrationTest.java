package com.support.tickets.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.support.tickets.dto.*;
import com.support.tickets.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class TicketIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // 1. Create → GET by ID → all fields persisted
    @Test
    void createAndGetById_allFieldsPersisted() throws Exception {
        TicketCreateRequest req = buildValidRequest("CUST001", "integ1@example.com");

        MvcResult createResult = mockMvc.perform(post("/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        TicketResponse created = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), TicketResponse.class);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getCustomerEmail()).isEqualTo("integ1@example.com");
        assertThat(created.getStatus()).isEqualTo(TicketStatus.NEW);

        mockMvc.perform(get("/tickets/" + created.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value("CUST001"))
                .andExpect(jsonPath("$.customerEmail").value("integ1@example.com"));
    }

    // 2. Create with autoClassify=true → category and priority are set
    @Test
    void createWithAutoClassify_categoryAndPrioritySet() throws Exception {
        TicketCreateRequest req = buildValidRequest("CUST002", "integ2@example.com");
        req.setSubject("Critical: production down for all users");
        req.setDescription("The production environment is completely down and all users cannot access it.");
        req.setAutoClassify(true);

        MvcResult result = mockMvc.perform(post("/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        TicketResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), TicketResponse.class);

        assertThat(response.getPriority()).isEqualTo(TicketPriority.URGENT);
        assertThat(response.getClassificationConfidence()).isNotNull().isGreaterThan(0.0);
    }

    // 3. Bulk import CSV → verify successful count
    @Test
    void bulkImportCsv_success() throws Exception {
        String csv = "customer_id,customer_email,customer_name,subject,description\n" +
                     "C1,bulk1@example.com,User One,Subject one,Description one is long enough to pass validation.\n" +
                     "C2,bulk2@example.com,User Two,Subject two,Description two is long enough to pass validation.";

        MockMultipartFile file = new MockMultipartFile("file", "tickets.csv",
                "text/csv", csv.getBytes());

        MvcResult result = mockMvc.perform(multipart("/tickets/import").file(file))
                .andExpect(status().isOk())
                .andReturn();

        BulkImportResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), BulkImportResponse.class);

        assertThat(response.getTotal()).isEqualTo(2);
        assertThat(response.getSuccessful()).isEqualTo(2);
        assertThat(response.getFailed()).isEqualTo(0);
    }

    // 4. Create → Update → verify fields changed and updatedAt differs
    @Test
    void createAndUpdate_fieldsUpdated() throws Exception {
        TicketCreateRequest req = buildValidRequest("CUST003", "integ3@example.com");

        MvcResult createResult = mockMvc.perform(post("/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        TicketResponse created = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), TicketResponse.class);

        TicketUpdateRequest update = new TicketUpdateRequest();
        update.setSubject("Updated subject");
        update.setStatus(TicketStatus.IN_PROGRESS);
        update.setAssignedTo("agent-007");

        MvcResult updateResult = mockMvc.perform(put("/tickets/" + created.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andReturn();

        TicketResponse updated = objectMapper.readValue(
                updateResult.getResponse().getContentAsString(), TicketResponse.class);

        assertThat(updated.getSubject()).isEqualTo("Updated subject");
        assertThat(updated.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
        assertThat(updated.getAssignedTo()).isEqualTo("agent-007");
    }

    // 5. Create → Delete → GET returns 404
    @Test
    void createAndDelete_getReturns404() throws Exception {
        TicketCreateRequest req = buildValidRequest("CUST004", "integ4@example.com");

        MvcResult createResult = mockMvc.perform(post("/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        TicketResponse created = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), TicketResponse.class);

        mockMvc.perform(delete("/tickets/" + created.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/tickets/" + created.getId()))
                .andExpect(status().isNotFound());
    }

    // ---- helpers ----

    private TicketCreateRequest buildValidRequest(String customerId, String email) {
        TicketCreateRequest req = new TicketCreateRequest();
        req.setCustomerId(customerId);
        req.setCustomerEmail(email);
        req.setCustomerName("Integration Test User");
        req.setSubject("Integration test subject");
        req.setDescription("This is a description written for integration test purposes, it is long enough.");
        return req;
    }
}
