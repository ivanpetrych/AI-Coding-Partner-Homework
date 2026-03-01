package com.support.tickets.service;

import com.support.tickets.dto.*;
import com.support.tickets.exception.TicketNotFoundException;
import com.support.tickets.model.*;
import com.support.tickets.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class TicketService {

    private final TicketRepository ticketRepository;
    private final ClassificationService classificationService;

    public TicketService(TicketRepository ticketRepository, ClassificationService classificationService) {
        this.ticketRepository = ticketRepository;
        this.classificationService = classificationService;
    }

    public TicketResponse createTicket(TicketCreateRequest request) {
        Ticket ticket = mapToEntity(request);
        if (request.isAutoClassify()) {
            ClassificationResult result = classificationService.classify(ticket.getSubject(), ticket.getDescription());
            ticket.setCategory(result.getCategory());
            ticket.setPriority(result.getPriority());
            ticket.setClassificationConfidence(result.getConfidence());
        }
        Ticket saved = ticketRepository.save(ticket);
        return TicketResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public TicketResponse getTicketById(UUID id) {
        return TicketResponse.from(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> getAllTickets(TicketCategory category, TicketPriority priority, TicketStatus status) {
        List<Ticket> tickets;
        if (category != null && priority != null && status != null) {
            tickets = ticketRepository.findByCategoryAndPriorityAndStatus(category, priority, status);
        } else if (category != null && priority != null) {
            tickets = ticketRepository.findByCategoryAndPriority(category, priority);
        } else if (category != null && status != null) {
            tickets = ticketRepository.findByCategoryAndStatus(category, status);
        } else if (priority != null && status != null) {
            tickets = ticketRepository.findByPriorityAndStatus(priority, status);
        } else if (category != null) {
            tickets = ticketRepository.findByCategory(category);
        } else if (priority != null) {
            tickets = ticketRepository.findByPriority(priority);
        } else if (status != null) {
            tickets = ticketRepository.findByStatus(status);
        } else {
            tickets = ticketRepository.findAll();
        }
        return tickets.stream().map(TicketResponse::from).collect(Collectors.toList());
    }

    public TicketResponse updateTicket(UUID id, TicketUpdateRequest request) {
        Ticket ticket = findOrThrow(id);
        if (request.getCustomerId() != null)    ticket.setCustomerId(request.getCustomerId());
        if (request.getCustomerEmail() != null) ticket.setCustomerEmail(request.getCustomerEmail());
        if (request.getCustomerName() != null)  ticket.setCustomerName(request.getCustomerName());
        if (request.getSubject() != null)       ticket.setSubject(request.getSubject());
        if (request.getDescription() != null)   ticket.setDescription(request.getDescription());
        if (request.getCategory() != null)      ticket.setCategory(request.getCategory());
        if (request.getPriority() != null)      ticket.setPriority(request.getPriority());
        if (request.getStatus() != null)        ticket.setStatus(request.getStatus());
        if (request.getResolvedAt() != null)    ticket.setResolvedAt(request.getResolvedAt());
        if (request.getAssignedTo() != null)    ticket.setAssignedTo(request.getAssignedTo());
        if (request.getTags() != null)          ticket.setTags(request.getTags());
        if (request.getMetadata() != null)      ticket.setMetadata(mapMetadata(request.getMetadata()));
        return TicketResponse.from(ticketRepository.save(ticket));
    }

    public void deleteTicket(UUID id) {
        findOrThrow(id);
        ticketRepository.deleteById(id);
    }

    public ClassificationResponse autoClassifyTicket(UUID id) {
        Ticket ticket = findOrThrow(id);
        ClassificationResult result = classificationService.classify(ticket.getSubject(), ticket.getDescription());
        ticket.setCategory(result.getCategory());
        ticket.setPriority(result.getPriority());
        ticket.setClassificationConfidence(result.getConfidence());
        ticketRepository.save(ticket);
        return new ClassificationResponse(
                ticket.getId(),
                result.getCategory(),
                result.getPriority(),
                result.getConfidence(),
                result.getReasoning(),
                result.getKeywordsFound()
        );
    }

    // ---- helper methods ----

    private Ticket findOrThrow(UUID id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException("Ticket not found: " + id));
    }

    Ticket mapToEntity(TicketCreateRequest request) {
        Ticket ticket = new Ticket();
        ticket.setCustomerId(request.getCustomerId());
        ticket.setCustomerEmail(request.getCustomerEmail());
        ticket.setCustomerName(request.getCustomerName());
        ticket.setSubject(request.getSubject());
        ticket.setDescription(request.getDescription());
        if (request.getCategory() != null) ticket.setCategory(request.getCategory());
        if (request.getPriority() != null) ticket.setPriority(request.getPriority());
        if (request.getStatus() != null)   ticket.setStatus(request.getStatus());
        if (request.getAssignedTo() != null) ticket.setAssignedTo(request.getAssignedTo());
        if (request.getTags() != null)     ticket.setTags(request.getTags());
        if (request.getMetadata() != null) ticket.setMetadata(mapMetadata(request.getMetadata()));
        return ticket;
    }

    private TicketMetadata mapMetadata(MetadataRequest m) {
        return new TicketMetadata(m.getSource(), m.getBrowser(), m.getDeviceType());
    }
}
