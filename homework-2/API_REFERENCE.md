# API Reference – Support Tickets

Base URL: `http://localhost:8080`

## Quick Start with Sample Data

To quickly populate the API with test data, ensure the server is running and execute:

```bash
# Generates 100 random tickets in CSV/JSON/XML
python3 scripts/generate_sample_data.py

# Uploads them to localhost:8080
./scripts/load_sample_data.sh
```

---

## Ticket Resource

All responses are JSON.

Ticket representation (simplified):

```json
{
  "id": "UUID",
  "customerId": "string",
  "customerEmail": "email",
  "customerName": "string",
  "subject": "string",
  "description": "string",
  "category": "account_access | technical_issue | billing_question | feature_request | bug_report | other",
  "priority": "urgent | high | medium | low",
  "status": "new | in_progress | waiting_customer | resolved | closed",
  "createdAt": "ISO-8601 datetime",
  "updatedAt": "ISO-8601 datetime",
  "resolvedAt": "ISO-8601 datetime or null",
  "assignedTo": "string or null",
  "tags": ["string", "..."]
}
```

### POST /tickets – Create Ticket

- **Request body**: JSON `TicketCreateRequest`
- **Status codes**: `201 Created`, `400 Bad Request`

Example request:

```bash
curl -X POST http://localhost:8080/tickets \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST001",
    "customerEmail": "user@example.com",
    "customerName": "User Name",
    "subject": "Cannot access account",
    "description": "I am locked out after enabling 2FA.",
    "autoClassify": true
  }'
```

Example success response (`201`):

```json
{
  "id": "...",
  "customerId": "CUST001",
  "customerEmail": "user@example.com",
  "status": "new",
  "category": "account_access",
  "priority": "urgent",
  "classificationConfidence": 0.8
}
```

### POST /tickets/import – Bulk Import

- **Content type**: `multipart/form-data`
- **Field**: `file` (CSV, JSON, or XML)
- **Status codes**: `200 OK`, `400 Bad Request`

Example (CSV):

```bash
curl -X POST http://localhost:8080/tickets/import \
  -F "file=@sample_tickets.csv"
```

Response (`BulkImportResponse`):

```json
{
  "total": 50,
  "successful": 45,
  "failed": 5,
  "errors": [
    { "row": 3, "field": "customerEmail", "message": "must be a well-formed email address" }
  ]
}
```

### GET /tickets – List Tickets

- **Query params (optional)**:
  - `category`: enum value (e.g. `technical_issue`)
  - `priority`: enum value (e.g. `high`)
  - `status`: enum value (e.g. `new`)
- **Status codes**: `200 OK`

Example:

```bash
curl "http://localhost:8080/tickets?category=technical_issue&priority=high"
```

Response:

```json
[
  { "id": "...", "subject": "...", "category": "technical_issue", "priority": "high" }
]
```

### GET /tickets/{id} – Get Ticket by ID

- **Path param**: `id` (UUID)
- **Status codes**: `200 OK`, `404 Not Found`

```bash
curl http://localhost:8080/tickets/{id}
```

### PUT /tickets/{id} – Update Ticket

- **Path param**: `id` (UUID)
- **Request body**: partial `TicketUpdateRequest`
- **Status codes**: `200 OK`, `400 Bad Request`, `404 Not Found`

```bash
curl -X PUT http://localhost:8080/tickets/{id} \
  -H "Content-Type: application/json" \
  -d '{ "subject": "Updated subject", "status": "in_progress" }'
```

### DELETE /tickets/{id} – Delete Ticket

- **Path param**: `id` (UUID)
- **Status codes**: `204 No Content`, `404 Not Found`

```bash
curl -X DELETE http://localhost:8080/tickets/{id}
```

### POST /tickets/{id}/auto-classify – Auto-Classification

- **Path param**: `id` (UUID)
- **Status codes**: `200 OK`, `404 Not Found`

```bash
curl -X POST http://localhost:8080/tickets/{id}/auto-classify
```

Response (`ClassificationResponse`):

```json
{
  "ticketId": "...",
  "category": "technical_issue",
  "priority": "high",
  "confidence": 0.75,
  "reasoning": "Category 'technical_issue' matched by keywords: [\"error\"]. Priority 'high' matched by keywords: [\"high priority\"].",
  "keywordsFound": ["error"]
}
```

---

## Error Responses

### Validation Errors (`400 Bad Request`)

```json
{
  "timestamp": "2024-01-01T10:00:00",
  "status": 400,
  "error": "Validation Failed",
  "fieldErrors": [
    { "field": "customerEmail", "message": "must be a well-formed email address" }
  ]
}
```

### Generic Errors

```json
{
  "timestamp": "2024-01-01T10:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Ticket not found: {id}"
}
```