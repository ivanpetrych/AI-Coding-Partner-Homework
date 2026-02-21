# Test Scripts Documentation

This document describes the validation scripts and end-to-end tests implemented for the Support Tickets System.

## 📂 Scripts Location
All scripts are located in `homework-2/support-tickets/scripts/`.

## 1. End-to-End Tests (`src/test/java/.../integration/TicketEndToEndTest.java`)

A JUnit 5 integration test suite running with `@SpringBootTest` to validate full workflow scenarios.

### Implemented Scenarios:

| Test Method | Description | Steps Verified |
|-------------|-------------|----------------|
| `testCompleteTicketLifecycle` | Full CRUD + Classification lifecycle | 1. Create ticket (`POST /tickets`)<br>2. Verify status is NEW<br>3. Update status/assignee (`PUT`)<br>4. Trigger Auto-classify (`POST .../auto-classify`)<br>5. Resolve ticket<br>6. Verify final state (`GET`) |
| `testBulkImportAndAutoClassification` | CSV Import Validation | 1. Simulate file upload with `auto_classify.csv`<br>2. Verify import count (successful/failed)<br>3. Verify tickets exist in system |
| `testConcurrentOperations` | Performance/Load Test | 1. Spawn **20 concurrent threads**<br>2. Send simultaneous `POST /tickets` requests<br>3. Verify all succeed without data race or loss |
| `testCombinedFiltering` | Search & Filtering | 1. Create mixed dataset (High/Low priority, various categories)<br>2. Query with multiple params: `?priority=HIGH&category=TECHNICAL_ISSUE`<br>3. Assert only matching tickets are returned |

### How to Run
Use the helper script:
```bash
./homework-2/support-tickets/scripts/test_e2e.sh
```

---

## 2. Sample Data Generator (`scripts/generate_sample_data.py`)

A Python script that procedurally generates realistic test data in multiple formats.

### Features
* **Randomized Data**: Generates customer names, emails, and issue details.
* **Volume**:
  * 50 CSV records
  * 20 JSON records
  * 30 XML records
* **Format Handling**: Correctly formats delimiters for CSV, JSON structure, and XML tags.

### Output
Files are saved to `src/test/resources/data/`:
* `sample_tickets.csv`
* `sample_tickets.json`
* `sample_tickets.xml`

### How to Run
```bash
python3 homework-2/support-tickets/scripts/generate_sample_data.py
```

---

## 3. Data Loader Script (`scripts/load_sample_data.sh`)

A Bash utility to populate a running API instance with the generated sample data.

### Usage
1. Start the application (`mvn spring-boot:run`).
2. Run the loader script:
```bash
./homework-2/support-tickets/scripts/load_sample_data.sh
```

### Actions
* Checks if data exists.
* Sends `POST` requests to `http://localhost:8080/tickets/import` for each file type.
* Prints server responses.

---

## 🧪 Testing Workflow for QA

1. **Generate fresh data**:
   `python3 scripts/generate_sample_data.py`
2. **Run automated E2E tests**:
   `./scripts/test_e2e.sh`
3. **Manual Verification**:
   * Start App: `mvn spring-boot:run`
   * Load Data: `./scripts/load_sample_data.sh`
   * Check Data: `curl http://localhost:8080/tickets`
