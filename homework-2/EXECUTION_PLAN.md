# Execution Plan: Intelligent Customer Support System (Java/Spring Boot)

**Scope:** Task 1 (Ticket API), Task 2 (Auto-Classification), Task 3 (Test Suite)  
**Excluded:** Task 4 (Documentation), Task 5 (Integration & Performance Tests)  
**Target directory:** `homework-2/support-tickets/`

---

## Phase 1: Project Scaffold

### Step 1.1 — Create Maven project structure
Generate `homework-2/support-tickets/` with standard Maven layout:
```
support-tickets/
├── pom.xml
└── src/
    ├── main/java/com/support/tickets/
    └── test/java/com/support/tickets/
```

### Step 1.2 — Configure `pom.xml`
Dependencies to include:
| Dependency | Purpose |
|---|---|
| `spring-boot-starter-web` | REST API |
| `spring-boot-starter-data-jpa` | JPA/Hibernate ORM |
| `spring-boot-starter-validation` | Bean validation (Jakarta) |
| `com.h2database:h2` | In-memory DB (no external setup needed) |
| `com.opencsv:opencsv` | CSV parsing |
| `com.fasterxml.jackson.dataformat:jackson-dataformat-xml` | XML parsing |
| `org.springframework.boot:spring-boot-starter-test` | JUnit 5, Mockito, MockMvc |
| `org.jacoco:jacoco-maven-plugin` | Code coverage reports |

### Step 1.3 — `application.properties`
Configure H2 in-memory datasource, JPA DDL auto-create, H2 console enabled (for dev inspection), and logging for classification decisions.

---

## Phase 2: Domain Model

### Step 2.1 — Enums
Four enum types, each mapping exactly to the allowed string values from the spec:

- `TicketCategory`: `ACCOUNT_ACCESS`, `TECHNICAL_ISSUE`, `BILLING_QUESTION`, `FEATURE_REQUEST`, `BUG_REPORT`, `OTHER`
- `TicketPriority`: `URGENT`, `HIGH`, `MEDIUM`, `LOW`
- `TicketStatus`: `NEW`, `IN_PROGRESS`, `WAITING_CUSTOMER`, `RESOLVED`, `CLOSED`
- `MetadataSource`: `WEB_FORM`, `EMAIL`, `API`, `CHAT`, `PHONE`
- `DeviceType`: `DESKTOP`, `MOBILE`, `TABLET`

### Step 2.2 — `TicketMetadata` (Embeddable)
JPA `@Embeddable` with fields: `source`, `browser` (nullable), `deviceType` (nullable).  
Serialized as nested JSON object `metadata`.

### Step 2.3 — `Ticket` (JPA Entity)
Full entity with:
- `id` — UUID, generated
- `customerId`, `customerEmail`, `customerName`, `subject`, `description`
- `category` — `TicketCategory` enum (nullable, set by auto-classify)
- `priority` — `TicketPriority` enum (nullable, set by auto-classify)
- `status` — `TicketStatus`, default `NEW`
- `createdAt`, `updatedAt` — auto-managed via `@PrePersist` / `@PreUpdate`
- `resolvedAt` — nullable
- `assignedTo` — nullable string
- `tags` — `@ElementCollection`, stored as separate table
- `metadata` — `@Embedded`
- `classificationConfidence` — `Double`, nullable (stored from auto-classify result)

### Step 2.4 — `ClassificationResult` (Non-entity value object)
Plain Java record/class holding: `category`, `priority`, `confidence` (double 0-1), `reasoning` (String), `keywordsFound` (List<String>).

---

## Phase 3: DTOs

### Step 3.1 — `TicketCreateRequest`
Input DTO for `POST /tickets` with full Jakarta Bean Validation:
- `@NotBlank` on `customerId`, `customerName`, `subject`, `description`
- `@Email` on `customerEmail`
- `@Size(min=1, max=200)` on `subject`
- `@Size(min=10, max=2000)` on `description`
- Optional boolean `autoClassify` (default `false`) — triggers auto-classification on creation

### Step 3.2 — `TicketUpdateRequest`
Partial update DTO — all fields optional (nullable), same validation constraints when present.  
Allows manual override of `category` and `priority`.

### Step 3.3 — `TicketResponse`
Output DTO mapping all entity fields to JSON-serializable form.  
Enums serialized as lowercase strings matching the spec (e.g. `"account_access"` not `"ACCOUNT_ACCESS"`).

### Step 3.4 — `BulkImportResponse`
Response for `POST /tickets/import`:
```json
{
  "total": 50,
  "successful": 48,
  "failed": 2,
  "errors": [
    { "row": 3, "field": "customer_email", "message": "Invalid email format" }
  ]
}
```

### Step 3.5 — `ClassificationResponse`
Response for `POST /tickets/:id/auto-classify`:
```json
{
  "ticketId": "uuid",
  "category": "technical_issue",
  "priority": "high",
  "confidence": 0.87,
  "reasoning": "Keywords matched: 'error', 'crash'",
  "keywordsFound": ["error", "crash"]
}
```

---

## Phase 4: Repository

### Step 4.1 — `TicketRepository`
Extends `JpaRepository<Ticket, UUID>`.  
Custom query methods:
- `findByCategory(TicketCategory category)`
- `findByPriority(TicketPriority priority)`
- `findByCategoryAndPriority(TicketCategory, TicketPriority)` — supports combined filtering
- `findByStatus(TicketStatus status)`
- `findByCustomerId(String customerId)`

---

## Phase 5: Services

### Step 5.1 — `TicketService`
Core CRUD orchestration:
- `createTicket(TicketCreateRequest)` — maps DTO → entity, saves, optionally calls `ClassificationService` if `autoClassify=true`
- `getTicketById(UUID)` — throws `TicketNotFoundException` if absent
- `getAllTickets(category, priority, status)` — uses repository query methods for filtering; all filter params are optional
- `updateTicket(UUID, TicketUpdateRequest)` — patch semantics (only update non-null fields)
- `deleteTicket(UUID)` — throws `TicketNotFoundException` if absent
- `autoClassifyTicket(UUID)` — delegates to `ClassificationService`, updates entity, logs result

### Step 5.2 — `ClassificationService`
Keyword-based classification engine:

**Category keyword map** (checked against `subject + description`, case-insensitive):
| Category | Keywords |
|---|---|
| `account_access` | login, password, 2fa, sign in, sign-in, locked out, forgot password, authentication |
| `technical_issue` | error, bug, crash, not working, broken, exception, failure, 500, timeout |
| `billing_question` | payment, invoice, refund, charge, subscription, billing, price, cost |
| `feature_request` | feature, enhancement, suggestion, improve, add support for, would like |
| `bug_report` | reproduce, steps to reproduce, version, expected behavior, actual behavior |

**Priority keyword map** (checked in order, first match wins):
| Priority | Keywords / Phrases |
|---|---|
| `URGENT` | can't access, critical, production down, security, outage, down for all |
| `HIGH` | important, blocking, asap, urgent (if not already matched), high priority |
| `LOW` | minor, cosmetic, suggestion, nice to have, low priority |
| `MEDIUM` | default (no keyword match) |

**Confidence scoring:**
- Count matched category keywords → confidence = matched / total_keywords_in_category_map (capped at 1.0)
- If no keyword matches in any category → `OTHER` with confidence 0.1

**Logging:** Every classification decision is logged at INFO level including ticket ID, resulting category/priority, confidence, and matched keywords.

### Step 5.3 — `ImportService`
Handles `POST /tickets/import` — detects format from `Content-Type` or file extension:

- **CSV** — use OpenCSV `CsvToBean`; map columns by header name matching snake_case field names; parse `tags` as semicolon-separated list
- **JSON** — use Jackson `ObjectMapper`; accept root array `[{...}, {...}]`
- **XML** — use Jackson `XmlMapper`; accept `<tickets><ticket>...</ticket></tickets>` root structure

For each record:
1. Run Bean Validation manually via `jakarta.validation.Validator`
2. If valid → call `TicketService.createTicket()`
3. If invalid → add `ImportError` with row number, field name, and message

Returns `BulkImportResponse` with counts and error list.

---

## Phase 6: Controller

### Step 6.1 — `TicketController`
Single `@RestController` at `/tickets`:

| Method | Path | Handler | Status |
|---|---|---|---|
| `POST` | `/tickets` | `createTicket` | `201 Created` |
| `POST` | `/tickets/import` | `bulkImport` | `200 OK` |
| `GET` | `/tickets` | `getAllTickets` | `200 OK` |
| `GET` | `/tickets/{id}` | `getTicketById` | `200 OK` |
| `PUT` | `/tickets/{id}` | `updateTicket` | `200 OK` |
| `DELETE` | `/tickets/{id}` | `deleteTicket` | `204 No Content` |
| `POST` | `/tickets/{id}/auto-classify` | `autoClassify` | `200 OK` |

`GET /tickets` accepts optional query params: `category`, `priority`, `status` (all nullable enums).  
`POST /tickets/import` accepts `multipart/form-data` with file field named `file`.

### Step 6.2 — `GlobalExceptionHandler`
`@RestControllerAdvice` handling:
- `TicketNotFoundException` → `404 Not Found`
- `MethodArgumentNotValidException` → `400 Bad Request` with field-level errors
- `HttpMessageNotReadableException` → `400 Bad Request` (malformed JSON/XML body)
- `IllegalArgumentException` → `400 Bad Request` (e.g. unknown enum value)
- Generic `Exception` → `500 Internal Server Error`

---

## Phase 7: Test Suite (Task 3)

All test classes live under `src/test/java/com/support/tickets/`.  
Fixture files at `src/test/resources/fixtures/`.

### Step 7.1 — `TicketControllerTest` (11 tests, MockMvc slice)
Using `@WebMvcTest` + `@MockBean` for services:
1. `POST /tickets` — valid request → 201 + body
2. `POST /tickets` — missing required field → 400 + error message
3. `POST /tickets` — invalid email format → 400
4. `GET /tickets` — returns list
5. `GET /tickets?category=technical_issue` — filtered list
6. `GET /tickets/{id}` — existing ticket → 200
7. `GET /tickets/{id}` — non-existent → 404
8. `PUT /tickets/{id}` — valid update → 200
9. `PUT /tickets/{id}` — non-existent → 404
10. `DELETE /tickets/{id}` — existing → 204
11. `POST /tickets/{id}/auto-classify` — valid → 200 + classification response

### Step 7.2 — `TicketModelTest` (9 tests, unit)
Using plain JUnit 5 + Jakarta Validator:
1. Valid ticket entity passes all constraints
2. Subject too long (>200) fails
3. Subject empty fails
4. Description too short (<10) fails
5. Description too long (>2000) fails
6. Invalid email format fails
7. Null required field (`customerId`) fails
8. `createdAt` is auto-set on persist
9. `updatedAt` is auto-updated on update

### Step 7.3 — `ImportCsvTest` (6 tests, unit)
Using `@ExtendWith(MockitoExtension.class)`:
1. Valid CSV with all fields → all records imported successfully
2. CSV with one invalid email row → partial success, error recorded
3. CSV with missing required column header → 400 / import error
4. Completely malformed CSV (binary data) → graceful error, no crash
5. CSV with empty file → 0 records, no error
6. CSV with tags as semicolon-separated → tags parsed as list correctly

### Step 7.4 — `ImportJsonTest` (5 tests, unit)
1. Valid JSON array → all records imported
2. JSON array with one invalid record → partial success
3. Malformed JSON (syntax error) → returns meaningful error
4. Empty JSON array `[]` → 0 records
5. JSON object instead of array → returns parse error

### Step 7.5 — `ImportXmlTest` (5 tests, unit)
1. Valid XML `<tickets>` root → all records imported
2. XML with one invalid record → partial success
3. Malformed XML (unclosed tag) → returns meaningful error
4. Empty `<tickets/>` → 0 records
5. XML with namespaces / extra attributes → handled gracefully

### Step 7.6 — `ClassificationServiceTest` (10 tests, unit)
Pure unit tests — no Spring context:
1. Subject contains "can't access" → URGENT + ACCOUNT_ACCESS
2. "production down" in description → URGENT + TECHNICAL_ISSUE
3. "invoice" and "refund" keywords → BILLING_QUESTION, confidence > 0.5
4. "feature" and "enhancement" → FEATURE_REQUEST
5. "reproduce" and "expected behavior" → BUG_REPORT
6. "cosmetic change" → LOW priority
7. "blocking" in subject → HIGH priority
8. No matching keywords → OTHER, confidence ≤ 0.2
9. Multiple categories with more hits → highest-count category wins
10. Classification result is logged (verify via logger capture / spy)

### Step 7.7 — `TicketIntegrationTest` (5 tests, `@SpringBootTest` full context)
Uses H2 in-memory DB, real HTTP via `TestRestTemplate` or `MockMvc`:
1. Create ticket → get by ID → verify all fields persisted correctly
2. Create ticket with `autoClassify=true` → category and priority are set in response
3. Bulk import CSV fixture file → verify `BulkImportResponse.successful` count
4. Create → update → verify updated fields, `updatedAt` changed
5. Create → delete → GET returns 404

### Step 7.8 — Sample Fixture Files (under `src/test/resources/fixtures/`)
| File | Contents |
|---|---|
| `valid_tickets.csv` | 10 valid tickets, all fields populated |
| `invalid_tickets.csv` | 5 rows: 2 invalid emails, 1 empty subject, 2 valid |
| `valid_tickets.json` | 5 valid ticket JSON array |
| `invalid_tickets.json` | Malformed JSON (missing closing bracket) |
| `valid_tickets.xml` | 5 valid tickets in XML |
| `invalid_tickets.xml` | XML with unclosed tag |

---

## Phase 8: Launch & Test Scripts

### Step 8.1 — `scripts/run.sh`
```bash
#!/usr/bin/env bash
# Builds and starts the application on port 8080
cd "$(dirname "$0")/.."
mvn spring-boot:run
```

### Step 8.2 — `scripts/test.sh`
```bash
#!/usr/bin/env bash
# Runs all tests and generates JaCoCo coverage report
cd "$(dirname "$0")/.."
mvn test jacoco:report
echo "Coverage report: target/site/jacoco/index.html"
```

---

## Full Directory Layout (End State)

```
homework-2/
├── EXECUTION_PLAN.md          ← this file
└── support-tickets/
    ├── pom.xml
    ├── scripts/
    │   ├── run.sh
    │   └── test.sh
    └── src/
        ├── main/
        │   ├── java/com/support/tickets/
        │   │   ├── SupportTicketsApplication.java
        │   │   ├── model/
        │   │   │   ├── Ticket.java
        │   │   │   ├── TicketCategory.java
        │   │   │   ├── TicketPriority.java
        │   │   │   ├── TicketStatus.java
        │   │   │   ├── MetadataSource.java
        │   │   │   ├── DeviceType.java
        │   │   │   ├── TicketMetadata.java
        │   │   │   └── ClassificationResult.java
        │   │   ├── dto/
        │   │   │   ├── TicketCreateRequest.java
        │   │   │   ├── TicketUpdateRequest.java
        │   │   │   ├── TicketResponse.java
        │   │   │   ├── BulkImportResponse.java
        │   │   │   ├── ImportError.java
        │   │   │   └── ClassificationResponse.java
        │   │   ├── repository/
        │   │   │   └── TicketRepository.java
        │   │   ├── service/
        │   │   │   ├── TicketService.java
        │   │   │   ├── ImportService.java
        │   │   │   └── ClassificationService.java
        │   │   ├── controller/
        │   │   │   └── TicketController.java
        │   │   └── exception/
        │   │       ├── TicketNotFoundException.java
        │   │       └── GlobalExceptionHandler.java
        │   └── resources/
        │       └── application.properties
        └── test/
            ├── java/com/support/tickets/
            │   ├── controller/
            │   │   └── TicketControllerTest.java
            │   ├── model/
            │   │   └── TicketModelTest.java
            │   ├── service/
            │   │   ├── ImportCsvTest.java
            │   │   ├── ImportJsonTest.java
            │   │   ├── ImportXmlTest.java
            │   │   └── ClassificationServiceTest.java
            │   └── integration/
            │       └── TicketIntegrationTest.java
            └── resources/
                ├── application-test.properties
                └── fixtures/
                    ├── valid_tickets.csv
                    ├── invalid_tickets.csv
                    ├── valid_tickets.json
                    ├── invalid_tickets.json
                    ├── valid_tickets.xml
                    └── invalid_tickets.xml
```

---

## Implementation Order

| # | Step | Depends On |
|---|---|---|
| 1 | Project scaffold + pom.xml | — |
| 2 | Enums + TicketMetadata + Ticket entity | 1 |
| 3 | ClassificationResult value object | 2 |
| 4 | All DTOs | 2 |
| 5 | TicketRepository | 2 |
| 6 | ClassificationService | 2, 3 |
| 7 | ImportService | 4, 5 |
| 8 | TicketService | 4, 5, 6 |
| 9 | TicketController + GlobalExceptionHandler | 4, 8 |
| 10 | application.properties | 1 |
| 11 | Fixture files | — |
| 12 | ClassificationServiceTest | 6, 11 |
| 13 | TicketModelTest | 2, 11 |
| 14 | ImportCsvTest / ImportJsonTest / ImportXmlTest | 7, 11 |
| 15 | TicketControllerTest | 9, 11 |
| 16 | TicketIntegrationTest | 9, 11 |
| 17 | scripts/run.sh + scripts/test.sh | 1 |
| 18 | Verify: `mvn test` passes + coverage >85% | all |
