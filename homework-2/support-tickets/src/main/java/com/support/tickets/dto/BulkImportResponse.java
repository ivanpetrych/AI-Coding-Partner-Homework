package com.support.tickets.dto;

import java.util.ArrayList;
import java.util.List;

public class BulkImportResponse {
    private int total;
    private int successful;
    private int failed;
    private List<ImportError> errors = new ArrayList<>();

    public BulkImportResponse() {}

    public BulkImportResponse(int total, int successful, int failed, List<ImportError> errors) {
        this.total = total;
        this.successful = successful;
        this.failed = failed;
        this.errors = errors;
    }

    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }

    public int getSuccessful() { return successful; }
    public void setSuccessful(int successful) { this.successful = successful; }

    public int getFailed() { return failed; }
    public void setFailed(int failed) { this.failed = failed; }

    public List<ImportError> getErrors() { return errors; }
    public void setErrors(List<ImportError> errors) { this.errors = errors; }
}
