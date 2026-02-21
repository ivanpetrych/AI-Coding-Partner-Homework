package com.support.tickets.controller;

import com.support.tickets.dto.*;
import com.support.tickets.model.*;
import com.support.tickets.service.ImportService;
import com.support.tickets.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/tickets")
public class TicketController {

    private final TicketService ticketService;
    private final ImportService importService;

    public TicketController(TicketService ticketService, ImportService importService) {
        this.ticketService = ticketService;
        this.importService = importService;
    }

    @PostMapping
    public ResponseEntity<TicketResponse> createTicket(@Valid @RequestBody TicketCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketService.createTicket(request));
    }

    @PostMapping("/import")
    public ResponseEntity<BulkImportResponse> bulkImport(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(importService.importFile(file));
    }

    @GetMapping
    public ResponseEntity<List<TicketResponse>> getAllTickets(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String status) {

        TicketCategory cat = category != null ? TicketCategory.fromValue(category) : null;
        TicketPriority pri = priority != null ? TicketPriority.fromValue(priority) : null;
        TicketStatus    sta = status   != null ? TicketStatus.fromValue(status)     : null;

        return ResponseEntity.ok(ticketService.getAllTickets(cat, pri, sta));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketResponse> getTicketById(@PathVariable UUID id) {
        return ResponseEntity.ok(ticketService.getTicketById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TicketResponse> updateTicket(
            @PathVariable UUID id,
            @Valid @RequestBody TicketUpdateRequest request) {
        return ResponseEntity.ok(ticketService.updateTicket(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTicket(@PathVariable UUID id) {
        ticketService.deleteTicket(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/auto-classify")
    public ResponseEntity<ClassificationResponse> autoClassify(@PathVariable UUID id) {
        return ResponseEntity.ok(ticketService.autoClassifyTicket(id));
    }
}
