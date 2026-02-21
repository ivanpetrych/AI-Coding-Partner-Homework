package com.support.tickets.model;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TicketModelTest {

    private static Validator validator;

    @BeforeAll
    static void setup() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    // 1. Valid ticket passes all constraints
    @Test
    void validTicket_noViolations() {
        Ticket ticket = buildValidTicket();
        Set<ConstraintViolation<Ticket>> violations = validator.validate(ticket);
        assertThat(violations).isEmpty();
    }

    // 2. Subject too long (>200) fails
    @Test
    void subject_tooLong_fails() {
        Ticket ticket = buildValidTicket();
        ticket.setSubject("A".repeat(201));
        Set<ConstraintViolation<Ticket>> violations = validator.validate(ticket);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("subject"));
    }

    // 3. Subject empty fails
    @Test
    void subject_empty_fails() {
        Ticket ticket = buildValidTicket();
        ticket.setSubject("");
        Set<ConstraintViolation<Ticket>> violations = validator.validate(ticket);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("subject"));
    }

    // 4. Description too short (<10) fails
    @Test
    void description_tooShort_fails() {
        Ticket ticket = buildValidTicket();
        ticket.setDescription("Short");
        Set<ConstraintViolation<Ticket>> violations = validator.validate(ticket);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("description"));
    }

    // 5. Description too long (>2000) fails
    @Test
    void description_tooLong_fails() {
        Ticket ticket = buildValidTicket();
        ticket.setDescription("A".repeat(2001));
        Set<ConstraintViolation<Ticket>> violations = validator.validate(ticket);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("description"));
    }

    // 6. Invalid email format fails
    @Test
    void customerEmail_invalid_fails() {
        Ticket ticket = buildValidTicket();
        ticket.setCustomerEmail("not-an-email");
        Set<ConstraintViolation<Ticket>> violations = validator.validate(ticket);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("customerEmail"));
    }

    // 7. Null customerId fails
    @Test
    void customerId_null_fails() {
        Ticket ticket = buildValidTicket();
        ticket.setCustomerId(null);
        Set<ConstraintViolation<Ticket>> violations = validator.validate(ticket);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("customerId"));
    }

    // 8. Default status is NEW
    @Test
    void defaultStatus_isNew() {
        Ticket ticket = new Ticket();
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.NEW);
    }

    // 9. Tags default to empty list (not null)
    @Test
    void tags_defaultToEmptyList() {
        Ticket ticket = new Ticket();
        assertThat(ticket.getTags()).isNotNull().isEmpty();
    }

    // ---- helper ----

    private Ticket buildValidTicket() {
        Ticket ticket = new Ticket();
        ticket.setCustomerId("CUST001");
        ticket.setCustomerEmail("user@example.com");
        ticket.setCustomerName("Test User");
        ticket.setSubject("Valid subject");
        ticket.setDescription("This description is long enough to pass validation.");
        return ticket;
    }
}
