package com.support.tickets.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.support.tickets.dto.*;
import com.support.tickets.exception.TicketNotFoundException;
import com.support.tickets.model.*;
import com.support.tickets.service.ImportService;
import com.support.tickets.service.TicketService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TicketController.class)
class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TicketService ticketService;

    @MockBean
    private ImportService importService;

    // 1. POST /tickets - valid request → 201
    @Test
    void createTicket_valid_returns201() throws Exception {
        TicketResponse response = buildTicketResponse(UUID.randomUUID());
        when(ticketService.createTicket(any())).thenReturn(response);

        mockMvc.perform(post("/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildValidCreateRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.customerEmail").value("test@example.com"));
    }

    // 2. POST /tickets - missing required field → 400
    @Test
    void createTicket_missingRequiredField_returns400() throws Exception {
        TicketCreateRequest request = buildValidCreateRequest();
        request.setCustomerId(null);

        mockMvc.perform(post("/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    // 3. POST /tickets - invalid email → 400
    @Test
    void createTicket_invalidEmail_returns400() throws Exception {
        TicketCreateRequest request = buildValidCreateRequest();
        request.setCustomerEmail("not-an-email");

        mockMvc.perform(post("/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // 4. GET /tickets - returns list
    @Test
    void getAllTickets_returnsList() throws Exception {
        when(ticketService.getAllTickets(null, null, null))
                .thenReturn(List.of(buildTicketResponse(UUID.randomUUID())));

        mockMvc.perform(get("/tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));
    }

    // 5. GET /tickets?category=technical_issue - filtered
    @Test
    void getAllTickets_withFilter_callsServiceWithFilter() throws Exception {
        when(ticketService.getAllTickets(eq(TicketCategory.TECHNICAL_ISSUE), isNull(), isNull()))
                .thenReturn(List.of());

        mockMvc.perform(get("/tickets").param("category", "technical_issue"))
                .andExpect(status().isOk());

        verify(ticketService).getAllTickets(TicketCategory.TECHNICAL_ISSUE, null, null);
    }

    // 6. GET /tickets/{id} - existing → 200
    @Test
    void getTicketById_exists_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(ticketService.getTicketById(id)).thenReturn(buildTicketResponse(id));

        mockMvc.perform(get("/tickets/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    // 7. GET /tickets/{id} - non-existent → 404
    @Test
    void getTicketById_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(ticketService.getTicketById(id)).thenThrow(new TicketNotFoundException("Ticket not found: " + id));

        mockMvc.perform(get("/tickets/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // 8. PUT /tickets/{id} - valid update → 200
    @Test
    void updateTicket_valid_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        TicketUpdateRequest req = new TicketUpdateRequest();
        req.setSubject("Updated subject");

        when(ticketService.updateTicket(eq(id), any())).thenReturn(buildTicketResponse(id));

        mockMvc.perform(put("/tickets/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    // 9. PUT /tickets/{id} - not found → 404
    @Test
    void updateTicket_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(ticketService.updateTicket(eq(id), any()))
                .thenThrow(new TicketNotFoundException("Ticket not found: " + id));

        mockMvc.perform(put("/tickets/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new TicketUpdateRequest())))
                .andExpect(status().isNotFound());
    }

    // 10. DELETE /tickets/{id} - existing → 204
    @Test
    void deleteTicket_exists_returns204() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(ticketService).deleteTicket(id);

        mockMvc.perform(delete("/tickets/" + id))
                .andExpect(status().isNoContent());
    }

    // 11. POST /tickets/{id}/auto-classify → 200
    @Test
    void autoClassify_valid_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        ClassificationResponse cr = new ClassificationResponse(id, TicketCategory.TECHNICAL_ISSUE,
                TicketPriority.HIGH, 0.75, "Matched error keywords", List.of("error"));
        when(ticketService.autoClassifyTicket(id)).thenReturn(cr);

        mockMvc.perform(post("/tickets/" + id + "/auto-classify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confidence").value(0.75))
                .andExpect(jsonPath("$.category").value("technical_issue"));
    }

    // ---- helpers ----

    private TicketCreateRequest buildValidCreateRequest() {
        TicketCreateRequest req = new TicketCreateRequest();
        req.setCustomerId("CUST001");
        req.setCustomerEmail("test@example.com");
        req.setCustomerName("Test User");
        req.setSubject("Test subject");
        req.setDescription("This is a test description that is long enough.");
        return req;
    }

    private TicketResponse buildTicketResponse(UUID id) {
        TicketResponse r = new TicketResponse();
        r.setId(id);
        r.setCustomerId("CUST001");
        r.setCustomerEmail("test@example.com");
        r.setCustomerName("Test User");
        r.setSubject("Test subject");
        r.setDescription("This is a test description that is long enough.");
        r.setStatus(TicketStatus.NEW);
        r.setCreatedAt(LocalDateTime.now());
        r.setUpdatedAt(LocalDateTime.now());
        return r;
    }
}
