package com.support.tickets.repository;

import com.support.tickets.model.Ticket;
import com.support.tickets.model.TicketCategory;
import com.support.tickets.model.TicketPriority;
import com.support.tickets.model.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID> {
    List<Ticket> findByCategory(TicketCategory category);
    List<Ticket> findByPriority(TicketPriority priority);
    List<Ticket> findByStatus(TicketStatus status);
    List<Ticket> findByCategoryAndPriority(TicketCategory category, TicketPriority priority);
    List<Ticket> findByCategoryAndStatus(TicketCategory category, TicketStatus status);
    List<Ticket> findByPriorityAndStatus(TicketPriority priority, TicketStatus status);
    List<Ticket> findByCategoryAndPriorityAndStatus(TicketCategory category, TicketPriority priority, TicketStatus status);
    List<Ticket> findByCustomerId(String customerId);
}
