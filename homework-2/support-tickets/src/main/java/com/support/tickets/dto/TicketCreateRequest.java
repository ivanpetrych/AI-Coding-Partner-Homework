package com.support.tickets.dto;

import com.support.tickets.model.*;
import jakarta.validation.constraints.*;

import java.util.List;

public class TicketCreateRequest {

    @NotBlank(message = "customer_id is required")
    private String customerId;

    @NotBlank(message = "customer_email is required")
    @Email(message = "customer_email must be a valid email address")
    private String customerEmail;

    @NotBlank(message = "customer_name is required")
    private String customerName;

    @NotBlank(message = "subject is required")
    @Size(min = 1, max = 200, message = "subject must be between 1 and 200 characters")
    private String subject;

    @NotBlank(message = "description is required")
    @Size(min = 10, max = 2000, message = "description must be between 10 and 2000 characters")
    private String description;

    private TicketCategory category;
    private TicketPriority priority;
    private TicketStatus status;
    private String assignedTo;
    private List<String> tags;
    private MetadataRequest metadata;
    private boolean autoClassify = false;

    // Getters and Setters

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public TicketCategory getCategory() { return category; }
    public void setCategory(TicketCategory category) { this.category = category; }

    public TicketPriority getPriority() { return priority; }
    public void setPriority(TicketPriority priority) { this.priority = priority; }

    public TicketStatus getStatus() { return status; }
    public void setStatus(TicketStatus status) { this.status = status; }

    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public MetadataRequest getMetadata() { return metadata; }
    public void setMetadata(MetadataRequest metadata) { this.metadata = metadata; }

    public boolean isAutoClassify() { return autoClassify; }
    public void setAutoClassify(boolean autoClassify) { this.autoClassify = autoClassify; }
}
