package com.support.tickets.model;

import com.support.tickets.dto.MetadataRequest;
import com.support.tickets.dto.MetadataResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Covers enum fromValue() methods, MetadataRequest/Response, TicketMetadata,
 * and other model helpers that need additional instruction coverage.
 */
class EnumAndModelCoverageTest {

    // ---- TicketCategory ----

    @Test
    void ticketCategory_fromValue_allValues() {
        assertThat(TicketCategory.fromValue("account_access")).isEqualTo(TicketCategory.ACCOUNT_ACCESS);
        assertThat(TicketCategory.fromValue("technical_issue")).isEqualTo(TicketCategory.TECHNICAL_ISSUE);
        assertThat(TicketCategory.fromValue("billing_question")).isEqualTo(TicketCategory.BILLING_QUESTION);
        assertThat(TicketCategory.fromValue("feature_request")).isEqualTo(TicketCategory.FEATURE_REQUEST);
        assertThat(TicketCategory.fromValue("bug_report")).isEqualTo(TicketCategory.BUG_REPORT);
        assertThat(TicketCategory.fromValue("other")).isEqualTo(TicketCategory.OTHER);
        assertThat(TicketCategory.fromValue("ACCOUNT_ACCESS")).isEqualTo(TicketCategory.ACCOUNT_ACCESS);
    }

    @Test
    void ticketCategory_fromValue_invalid_throwsException() {
        assertThatThrownBy(() -> TicketCategory.fromValue("nonexistent"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown category");
    }

    // ---- TicketPriority ----

    @Test
    void ticketPriority_fromValue_allValues() {
        assertThat(TicketPriority.fromValue("urgent")).isEqualTo(TicketPriority.URGENT);
        assertThat(TicketPriority.fromValue("high")).isEqualTo(TicketPriority.HIGH);
        assertThat(TicketPriority.fromValue("medium")).isEqualTo(TicketPriority.MEDIUM);
        assertThat(TicketPriority.fromValue("low")).isEqualTo(TicketPriority.LOW);
        assertThat(TicketPriority.fromValue("URGENT")).isEqualTo(TicketPriority.URGENT);
    }

    @Test
    void ticketPriority_fromValue_invalid_throwsException() {
        assertThatThrownBy(() -> TicketPriority.fromValue("critical"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void ticketPriority_getValue() {
        assertThat(TicketPriority.URGENT.getValue()).isEqualTo("urgent");
        assertThat(TicketPriority.HIGH.getValue()).isEqualTo("high");
        assertThat(TicketPriority.MEDIUM.getValue()).isEqualTo("medium");
        assertThat(TicketPriority.LOW.getValue()).isEqualTo("low");
    }

    // ---- TicketStatus ----

    @Test
    void ticketStatus_fromValue_allValues() {
        assertThat(TicketStatus.fromValue("new")).isEqualTo(TicketStatus.NEW);
        assertThat(TicketStatus.fromValue("in_progress")).isEqualTo(TicketStatus.IN_PROGRESS);
        assertThat(TicketStatus.fromValue("waiting_customer")).isEqualTo(TicketStatus.WAITING_CUSTOMER);
        assertThat(TicketStatus.fromValue("resolved")).isEqualTo(TicketStatus.RESOLVED);
        assertThat(TicketStatus.fromValue("closed")).isEqualTo(TicketStatus.CLOSED);
        assertThat(TicketStatus.fromValue("NEW")).isEqualTo(TicketStatus.NEW);
    }

    @Test
    void ticketStatus_fromValue_invalid_throwsException() {
        assertThatThrownBy(() -> TicketStatus.fromValue("deleted"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void ticketStatus_getValue() {
        assertThat(TicketStatus.NEW.getValue()).isEqualTo("new");
        assertThat(TicketStatus.IN_PROGRESS.getValue()).isEqualTo("in_progress");
        assertThat(TicketStatus.RESOLVED.getValue()).isEqualTo("resolved");
    }

    // ---- MetadataSource ----

    @Test
    void metadataSource_fromValue_allValues() {
        assertThat(MetadataSource.fromValue("web_form")).isEqualTo(MetadataSource.WEB_FORM);
        assertThat(MetadataSource.fromValue("email")).isEqualTo(MetadataSource.EMAIL);
        assertThat(MetadataSource.fromValue("api")).isEqualTo(MetadataSource.API);
        assertThat(MetadataSource.fromValue("chat")).isEqualTo(MetadataSource.CHAT);
        assertThat(MetadataSource.fromValue("phone")).isEqualTo(MetadataSource.PHONE);
        assertThat(MetadataSource.fromValue("WEB_FORM")).isEqualTo(MetadataSource.WEB_FORM);
    }

    @Test
    void metadataSource_fromValue_invalid_throwsException() {
        assertThatThrownBy(() -> MetadataSource.fromValue("fax"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void metadataSource_getValue() {
        assertThat(MetadataSource.WEB_FORM.getValue()).isEqualTo("web_form");
        assertThat(MetadataSource.EMAIL.getValue()).isEqualTo("email");
    }

    // ---- DeviceType ----

    @Test
    void deviceType_fromValue_allValues() {
        assertThat(DeviceType.fromValue("desktop")).isEqualTo(DeviceType.DESKTOP);
        assertThat(DeviceType.fromValue("mobile")).isEqualTo(DeviceType.MOBILE);
        assertThat(DeviceType.fromValue("tablet")).isEqualTo(DeviceType.TABLET);
        assertThat(DeviceType.fromValue("DESKTOP")).isEqualTo(DeviceType.DESKTOP);
    }

    @Test
    void deviceType_fromValue_invalid_throwsException() {
        assertThatThrownBy(() -> DeviceType.fromValue("watch"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deviceType_getValue() {
        assertThat(DeviceType.DESKTOP.getValue()).isEqualTo("desktop");
        assertThat(DeviceType.MOBILE.getValue()).isEqualTo("mobile");
        assertThat(DeviceType.TABLET.getValue()).isEqualTo("tablet");
    }

    // ---- TicketMetadata ----

    @Test
    void ticketMetadata_defaultConstructor() {
        TicketMetadata meta = new TicketMetadata();
        assertThat(meta.getSource()).isNull();
        assertThat(meta.getBrowser()).isNull();
        assertThat(meta.getDeviceType()).isNull();
    }

    @Test
    void ticketMetadata_allArgsConstructor_andGetters() {
        TicketMetadata meta = new TicketMetadata(MetadataSource.WEB_FORM, "Chrome", DeviceType.DESKTOP);
        assertThat(meta.getSource()).isEqualTo(MetadataSource.WEB_FORM);
        assertThat(meta.getBrowser()).isEqualTo("Chrome");
        assertThat(meta.getDeviceType()).isEqualTo(DeviceType.DESKTOP);
    }

    @Test
    void ticketMetadata_setters() {
        TicketMetadata meta = new TicketMetadata();
        meta.setSource(MetadataSource.API);
        meta.setBrowser("Firefox");
        meta.setDeviceType(DeviceType.MOBILE);

        assertThat(meta.getSource()).isEqualTo(MetadataSource.API);
        assertThat(meta.getBrowser()).isEqualTo("Firefox");
        assertThat(meta.getDeviceType()).isEqualTo(DeviceType.MOBILE);
    }

    // ---- MetadataRequest ----

    @Test
    void metadataRequest_settersAndGetters() {
        MetadataRequest req = new MetadataRequest();
        req.setSource(MetadataSource.CHAT);
        req.setBrowser("Safari");
        req.setDeviceType(DeviceType.TABLET);

        assertThat(req.getSource()).isEqualTo(MetadataSource.CHAT);
        assertThat(req.getBrowser()).isEqualTo("Safari");
        assertThat(req.getDeviceType()).isEqualTo(DeviceType.TABLET);
    }

    // ---- MetadataResponse ----

    @Test
    void metadataResponse_settersAndGetters() {
        MetadataResponse resp = new MetadataResponse();
        resp.setSource(MetadataSource.PHONE);
        resp.setBrowser("Edge");
        resp.setDeviceType(DeviceType.DESKTOP);

        assertThat(resp.getSource()).isEqualTo(MetadataSource.PHONE);
        assertThat(resp.getBrowser()).isEqualTo("Edge");
        assertThat(resp.getDeviceType()).isEqualTo(DeviceType.DESKTOP);
    }

    // ---- ClassificationResult ----

    @Test
    void classificationResult_getReasoning() {
        ClassificationResult result = new ClassificationResult(
                TicketCategory.BUG_REPORT, TicketPriority.HIGH,
                0.75, "Bug-related reasoning text", List.of("crash"));

        assertThat(result.getReasoning()).isEqualTo("Bug-related reasoning text");
    }
}
