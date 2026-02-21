package com.support.tickets.dto;

public class ImportError {
    private int row;
    private String field;
    private String message;

    public ImportError() {}

    public ImportError(int row, String field, String message) {
        this.row = row;
        this.field = field;
        this.message = message;
    }

    public int getRow() { return row; }
    public void setRow(int row) { this.row = row; }

    public String getField() { return field; }
    public void setField(String field) { this.field = field; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
