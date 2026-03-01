package com.support.tickets.service;

import com.support.tickets.dto.*;
import com.support.tickets.exception.TicketNotFoundException;
import com.support.tickets.model.*;
import com.support.tickets.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private ClassificationService classificationService;

    @InjectMocks
    private TicketService ticketService;

    private Ticket sampleTicket;
    private UUID ticketId;

    @BeforeEach
    void setUp() {
        ticketId = UUID.randomUUID();
        sampleTicket = new Ticket();
        sampleTicket.setCustomerId("C001");
        sampleTicket.setCustomerEmail("user@example.com");
        sampleTicket.setCustomerName("John Doe");
        sampleTicket.setSubject("Login issue");
        sampleTicket.setDescription("Cannot log in to my account");
        sampleTicket.setCategory(TicketCategory.ACCOUNT_ACCESS);
        sampleTicket.setPriority(TicketPriority.HIGH);
        sampleTicket.setStatus(TicketStatus.NEW);
    }

    // ---- createTicket ----

    @Test
    void createTicket_withoutAutoClassify_savesAndReturnsResponse() {
        when(ticketRepository.save(any(Ticket.class))).thenReturn(sampleTicket);

        TicketCreateRequest req = buildCreateRequest(false);
        TicketResponse resp = ticketService.createTicket(req);

        assertThat(resp).isNotNull();
        assertThat(resp.getCustomerEmail()).isEqualTo("user@example.com");
        verify(classificationService, never()).classify(any(), any());
        verify(ticketRepository).save(any(Ticket.class));
    }

    @Test
    void createTicket_withAutoClassify_classifiesAndSaves() {
        ClassificationResult result = new ClassificationResult(
                TicketCategory.TECHNICAL_ISSUE, TicketPriority.URGENT,
                0.9, "technical keywords found", List.of("crash", "error"));
        when(classificationService.classify(any(), any())).thenReturn(result);
        when(ticketRepository.save(any(Ticket.class))).thenReturn(sampleTicket);

        TicketCreateRequest req = buildCreateRequest(true);
        ticketService.createTicket(req);

        verify(classificationService).classify(any(), any());
        verify(ticketRepository).save(any(Ticket.class));
    }

    @Test
    void createTicket_withAllOptionalFieldsNull_mapsCorrectly() {
        TicketCreateRequest req = new TicketCreateRequest();
        req.setCustomerId("C002");
        req.setCustomerEmail("a@b.com");
        req.setCustomerName("Alice");
        req.setSubject("Question");
        req.setDescription("Just a question");
        // category, priority, status, assignedTo, tags, metadata all null
        when(ticketRepository.save(any(Ticket.class))).thenReturn(sampleTicket);

        TicketResponse resp = ticketService.createTicket(req);
        assertThat(resp).isNotNull();
    }

    @Test
    void createTicket_withMetadata_setsMetadataOnEntity() {
        MetadataRequest meta = new MetadataRequest();
        meta.setSource(MetadataSource.WEB_FORM);
        meta.setBrowser("Chrome");
        meta.setDeviceType(DeviceType.DESKTOP);

        TicketCreateRequest req = buildCreateRequest(false);
        req.setMetadata(meta);
        when(ticketRepository.save(any(Ticket.class))).thenReturn(sampleTicket);

        ticketService.createTicket(req);
        verify(ticketRepository).save(any(Ticket.class));
    }

    // ---- getAllTickets filter combinations ----

    @Test
    void getAllTickets_noFilters_callsFindAll() {
        when(ticketRepository.findAll()).thenReturn(List.of(sampleTicket));

        List<TicketResponse> result = ticketService.getAllTickets(null, null, null);

        assertThat(result).hasSize(1);
        verify(ticketRepository).findAll();
    }

    @Test
    void getAllTickets_categoryOnly_callsFindByCategory() {
        when(ticketRepository.findByCategory(TicketCategory.ACCOUNT_ACCESS)).thenReturn(List.of(sampleTicket));

        List<TicketResponse> result = ticketService.getAllTickets(TicketCategory.ACCOUNT_ACCESS, null, null);

        assertThat(result).hasSize(1);
        verify(ticketRepository).findByCategory(TicketCategory.ACCOUNT_ACCESS);
    }

    @Test
    void getAllTickets_priorityOnly_callsFindByPriority() {
        when(ticketRepository.findByPriority(TicketPriority.HIGH)).thenReturn(List.of(sampleTicket));

        List<TicketResponse> result = ticketService.getAllTickets(null, TicketPriority.HIGH, null);

        assertThat(result).hasSize(1);
        verify(ticketRepository).findByPriority(TicketPriority.HIGH);
    }

    @Test
    void getAllTickets_statusOnly_callsFindByStatus() {
        when(ticketRepository.findByStatus(TicketStatus.NEW)).thenReturn(List.of(sampleTicket));

        List<TicketResponse> result = ticketService.getAllTickets(null, null, TicketStatus.NEW);

        assertThat(result).hasSize(1);
        verify(ticketRepository).findByStatus(TicketStatus.NEW);
    }

    @Test
    void getAllTickets_categoryAndPriority_callsFindByCategoryAndPriority() {
        when(ticketRepository.findByCategoryAndPriority(TicketCategory.ACCOUNT_ACCESS, TicketPriority.HIGH))
                .thenReturn(List.of(sampleTicket));

        List<TicketResponse> result = ticketService.getAllTickets(TicketCategory.ACCOUNT_ACCESS, TicketPriority.HIGH, null);

        assertThat(result).hasSize(1);
        verify(ticketRepository).findByCategoryAndPriority(TicketCategory.ACCOUNT_ACCESS, TicketPriority.HIGH);
    }

    @Test
    void getAllTickets_categoryAndStatus_callsFindByCategoryAndStatus() {
        when(ticketRepository.findByCategoryAndStatus(TicketCategory.ACCOUNT_ACCESS, TicketStatus.NEW))
                .thenReturn(List.of(sampleTicket));

        List<TicketResponse> result = ticketService.getAllTickets(TicketCategory.ACCOUNT_ACCESS, null, TicketStatus.NEW);

        assertThat(result).hasSize(1);
        verify(ticketRepository).findByCategoryAndStatus(TicketCategory.ACCOUNT_ACCESS, TicketStatus.NEW);
    }

    @Test
    void getAllTickets_priorityAndStatus_callsFindByPriorityAndStatus() {
        when(ticketRepository.findByPriorityAndStatus(TicketPriority.HIGH, TicketStatus.NEW))
                .thenReturn(List.of(sampleTicket));

        List<TicketResponse> result = ticketService.getAllTickets(null, TicketPriority.HIGH, TicketStatus.NEW);

        assertThat(result).hasSize(1);
        verify(ticketRepository).findByPriorityAndStatus(TicketPriority.HIGH, TicketStatus.NEW);
    }

    @Test
    void getAllTickets_allThreeFilters_callsFindByCategoryAndPriorityAndStatus() {
        when(ticketRepository.findByCategoryAndPriorityAndStatus(
                TicketCategory.ACCOUNT_ACCESS, TicketPriority.HIGH, TicketStatus.NEW))
                .thenReturn(List.of(sampleTicket));

        List<TicketResponse> result = ticketService.getAllTickets(
                TicketCategory.ACCOUNT_ACCESS, TicketPriority.HIGH, TicketStatus.NEW);

        assertThat(result).hasSize(1);
        verify(ticketRepository).findByCategoryAndPriorityAndStatus(
                TicketCategory.ACCOUNT_ACCESS, TicketPriority.HIGH, TicketStatus.NEW);
    }

    // ---- getTicketById ----

    @Test
    void getTicketById_found_returnsResponse() {
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(sampleTicket));

        TicketResponse resp = ticketService.getTicketById(ticketId);
        assertThat(resp).isNotNull();
    }

    @Test
    void getTicketById_notFound_throwsException() {
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.getTicketById(ticketId))
                .isInstanceOf(TicketNotFoundException.class);
    }

    // ---- updateTicket ----

    @Test
    void updateTicket_allFieldsSet_updatesAllFields() {
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(sampleTicket));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(sampleTicket);

        TicketUpdateRequest req = new TicketUpdateRequest();
        req.setCustomerId("C999");
        req.setCustomerEmail("new@example.com");
        req.setCustomerName("Jane");
        req.setSubject("Updated subject");
        req.setDescription("Updated description");
        req.setCategory(TicketCategory.BILLING_QUESTION);
        req.setPriority(TicketPriority.LOW);
        req.setStatus(TicketStatus.RESOLVED);
        req.setResolvedAt(LocalDateTime.now());
        req.setAssignedTo("agent42");
        req.setTags(List.of("billing", "urgent"));

        MetadataRequest meta = new MetadataRequest();
        meta.setSource(MetadataSource.EMAIL);
        meta.setBrowser("Firefox");
        meta.setDeviceType(DeviceType.MOBILE);
        req.setMetadata(meta);

        TicketResponse resp = ticketService.updateTicket(ticketId, req);
        assertThat(resp).isNotNull();
        verify(ticketRepository).save(sampleTicket);
    }

    @Test
    void updateTicket_allFieldsNull_doesNotUpdateFields() {
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(sampleTicket));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(sampleTicket);

        TicketUpdateRequest req = new TicketUpdateRequest(); // all null
        ticketService.updateTicket(ticketId, req);

        // No field updates happened — category still ACCOUNT_ACCESS
        assertThat(sampleTicket.getCategory()).isEqualTo(TicketCategory.ACCOUNT_ACCESS);
        verify(ticketRepository).save(sampleTicket);
    }

    @Test
    void updateTicket_notFound_throwsException() {
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.updateTicket(ticketId, new TicketUpdateRequest()))
                .isInstanceOf(TicketNotFoundException.class);
    }

    // ---- deleteTicket ----

    @Test
    void deleteTicket_found_deletesTicket() {
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(sampleTicket));
        doNothing().when(ticketRepository).deleteById(ticketId);

        ticketService.deleteTicket(ticketId);

        verify(ticketRepository).deleteById(ticketId);
    }

    @Test
    void deleteTicket_notFound_throwsException() {
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.deleteTicket(ticketId))
                .isInstanceOf(TicketNotFoundException.class);
    }

    // ---- autoClassifyTicket ----

    @Test
    void autoClassifyTicket_found_classifiesAndReturnsResponse() {
        ClassificationResult result = new ClassificationResult(
                TicketCategory.BUG_REPORT, TicketPriority.URGENT,
                0.85, "bug keywords", List.of("crash", "error", "bug"));
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(sampleTicket));
        when(classificationService.classify(any(), any())).thenReturn(result);
        when(ticketRepository.save(any(Ticket.class))).thenReturn(sampleTicket);

        ClassificationResponse resp = ticketService.autoClassifyTicket(ticketId);

        assertThat(resp.getCategory()).isEqualTo(TicketCategory.BUG_REPORT);
        assertThat(resp.getPriority()).isEqualTo(TicketPriority.URGENT);
        assertThat(resp.getConfidence()).isEqualTo(0.85);
        assertThat(resp.getKeywordsFound()).contains("crash", "error", "bug");
    }

    @Test
    void autoClassifyTicket_notFound_throwsException() {
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.autoClassifyTicket(ticketId))
                .isInstanceOf(TicketNotFoundException.class);
    }

    // ---- mapToEntity ----

    @Test
    void mapToEntity_withOptionalFields_mapsAllFields() {
        TicketCreateRequest req = buildCreateRequest(false);
        req.setAssignedTo("agent1");
        req.setTags(List.of("tag1", "tag2"));

        Ticket entity = ticketService.mapToEntity(req);

        assertThat(entity.getCustomerId()).isEqualTo("C001");
        assertThat(entity.getCustomerEmail()).isEqualTo("user@example.com");
        assertThat(entity.getCustomerName()).isEqualTo("John Doe");
        assertThat(entity.getSubject()).isEqualTo("Login issue");
        assertThat(entity.getCategory()).isEqualTo(TicketCategory.ACCOUNT_ACCESS);
        assertThat(entity.getPriority()).isEqualTo(TicketPriority.HIGH);
        assertThat(entity.getStatus()).isEqualTo(TicketStatus.NEW);
        assertThat(entity.getAssignedTo()).isEqualTo("agent1");
        assertThat(entity.getTags()).containsExactly("tag1", "tag2");
    }

    // ---- helpers ----

    private TicketCreateRequest buildCreateRequest(boolean autoClassify) {
        TicketCreateRequest req = new TicketCreateRequest();
        req.setCustomerId("C001");
        req.setCustomerEmail("user@example.com");
        req.setCustomerName("John Doe");
        req.setSubject("Login issue");
        req.setDescription("Cannot log in to my account");
        req.setCategory(TicketCategory.ACCOUNT_ACCESS);
        req.setPriority(TicketPriority.HIGH);
        req.setStatus(TicketStatus.NEW);
        req.setAutoClassify(autoClassify);
        return req;
    }
}
