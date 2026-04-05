# Specification: Multi-Agent Banking Transaction Pipeline

> Ingest the information from this file, implement the Low-Level Tasks, and generate the code that will satisfy the High and Mid-Level Objectives.

---

## 1. High-Level Objective

Build a 3-agent Python pipeline that validates, scores for fraud risk, and reports on banking transactions using file-based JSON message passing through shared directories.

---

## 2. Mid-Level Objectives

1. **Validation**: Transactions with missing required fields, negative amounts, or invalid ISO 4217 currency codes are rejected with a machine-readable reason (e.g. `INVALID_CURRENCY`, `NEGATIVE_AMOUNT`, `MISSING_FIELD:<name>`).
2. **Fraud scoring**: Transactions above $10,000 receive +3 risk points; above $50,000 receive an additional +4; timestamps between 02:00–04:59 UTC add +2; cross-border (source country ≠ destination country) adds +1. Risk levels: LOW (0–2), MEDIUM (3–6), HIGH (7–10).
3. **Reporting**: Every transaction (validated or rejected) produces a final result JSON file in `shared/results/` containing the full processing history, risk assessment, and final status.
4. **Audit logging**: All agent operations are logged to stdout with ISO 8601 timestamps, agent name, transaction ID, and outcome. Account numbers are masked to last 4 characters in all log output.
5. **Pipeline completeness**: Running `python integrator.py` processes all 8 sample transactions and writes exactly 8 result files to `shared/results/`, plus a `pipeline_summary.json` with aggregate statistics.

---

## 3. Implementation Notes

### Monetary Values
- Use `decimal.Decimal` for all monetary calculations — **never `float`**.
- Parse amounts from string representations in the source JSON to preserve precision.
- Comparisons use `Decimal` thresholds (e.g. `Decimal("10000")` not `10000.0`).

### Currency Validation
- ISO 4217 whitelist: `USD`, `EUR`, `GBP`, `JPY`, `CHF`, `CAD`, `AUD`, `NZD`, `CNY`, `HKD`, `SGD`, `SEK`, `NOK`, `DKK`, `INR`, `BRL`, `MXN`, `ZAR`, `KRW`, `PLN`.
- Any currency code not in the whitelist triggers rejection with reason `INVALID_CURRENCY`.

### Logging & PII
- All log lines: `[ISO8601] [AGENT_NAME] [TXN_ID] message`
- Account numbers are masked: `ACC-1001` → `***1001` in logs.
- Transaction amounts and currency codes may be logged.

### Message Format
Standard inter-agent message:
```json
{
  "message_id": "uuid4-string",
  "timestamp": "ISO8601",
  "source_agent": "agent_name",
  "target_agent": "next_agent_name",
  "message_type": "transaction",
  "data": { "...original + enriched fields..." }
}
```

### Error Handling
- Agents must not crash on malformed input — log the error, mark the transaction as `"status": "error"`, and continue processing the remaining transactions.
- The integrator catches and logs exceptions from individual transaction processing without halting the pipeline.

---

## 4. Context

### Beginning State
- `sample-transactions.json` exists with 8 raw transaction records.
- No agents, integrator, shared directories, tests, or documentation exist yet.
- Python 3.11+ is available.

### Ending State
- `integrator.py` orchestrates the pipeline end-to-end.
- Three agent modules in `agents/`: `transaction_validator.py`, `fraud_detector.py`, `reporting_agent.py`.
- Shared directories (`shared/input/`, `shared/processing/`, `shared/output/`, `shared/results/`) are created at runtime.
- All 8 transactions processed: 6 validated+scored+reported, 2 rejected+reported.
- `shared/results/` contains 8 individual `TXN*.json` files + 1 `pipeline_summary.json`.
- Test suite in `tests/` with coverage ≥ 90%.
- `README.md` (with author name), `HOWTORUN.md`, and `research-notes.md` complete.
- MCP server in `mcp/server.py` exposes pipeline query tools.

---

## 5. Low-Level Tasks

### Task: Transaction Validator

**Prompt**:
```
Context: Python 3.11+ project. File-based agent pipeline. Messages are dicts with "data" containing transaction fields.
Task: Build a Transaction Validator agent that checks each transaction for required fields, valid positive Decimal amount, and ISO 4217 currency code.
Rules:
  - Use decimal.Decimal for all amount handling, never float.
  - Required fields: transaction_id, amount, currency, source_account, destination_account.
  - Reject with specific reason codes: MISSING_FIELD:<name>, NEGATIVE_AMOUNT, INVALID_CURRENCY.
  - Mask account numbers in all log output (ACC-1001 → ***1001).
  - Support --dry-run CLI flag for standalone validation without file I/O.
Output: agents/transaction_validator.py with a TransactionValidator class extending BaseAgent, implementing process_message(message: dict) -> dict.
```
**File to CREATE**: `agents/transaction_validator.py`
**Function to CREATE**: `TransactionValidator.process_message(message: dict) -> dict`
**Details**:
- Check required fields: `transaction_id`, `amount`, `currency`, `source_account`, `destination_account`
- Validate amount is a positive `Decimal` (reject zero and negative)
- Validate currency against ISO 4217 whitelist
- Return message with `data.status` = `"validated"` or `"rejected"` + `data.rejection_reason`
- Support `--dry-run` CLI mode: read `sample-transactions.json`, validate all, print table, exit

---

### Task: Fraud Detector

**Prompt**:
```
Context: Python 3.11+ project. Agent receives validated transaction messages (dict). Rejected transactions should be passed through unchanged.
Task: Build a Fraud Detector agent that scores each validated transaction for fraud risk on a 0–10 scale.
Rules:
  - Use decimal.Decimal for amount comparisons.
  - Scoring: amount > $10,000 → +3 pts; amount > $50,000 → +4 additional pts; timestamp hour 02:00–04:59 UTC → +2 pts; cross-border (metadata.country != "US" heuristic or differing countries) → +1 pt.
  - Risk levels: LOW (0–2), MEDIUM (3–6), HIGH (7–10).
  - Skip scoring for rejected transactions — pass them through with existing status.
  - Log each score decision with agent name and transaction ID.
Output: agents/fraud_detector.py with a FraudDetector class extending BaseAgent, implementing process_message(message: dict) -> dict.
```
**File to CREATE**: `agents/fraud_detector.py`
**Function to CREATE**: `FraudDetector.process_message(message: dict) -> dict`
**Details**:
- Only score transactions with `data.status == "validated"`
- Scoring rules (cumulative):
  - `amount > 10000` → +3 points
  - `amount > 50000` → +4 additional points (total +7 for >$50k)
  - Timestamp hour 02–04 UTC → +2 points
  - `metadata.country != "US"` → +1 point (cross-border heuristic)
- Enrich `data` with: `fraud_risk_score` (int), `fraud_risk_level` (LOW/MEDIUM/HIGH), `fraud_risk_factors` (list of triggered rules)
- Pass rejected transactions through unchanged

---

### Task: Reporting Agent

**Prompt**:
```
Context: Python 3.11+ project. Agent receives fully processed transaction messages (validated+scored or rejected). Must write final result files.
Task: Build a Reporting Agent that generates a final result JSON for every transaction and writes a pipeline summary.
Rules:
  - Write one JSON file per transaction to shared/results/ named {transaction_id}.json.
  - Each result includes: transaction_id, original data, validation status, fraud assessment (if applicable), processing timestamps, and final_status.
  - After all transactions, write pipeline_summary.json with: total count, validated count, rejected count, risk level breakdown, processing duration.
  - Mask account numbers in result files.
  - Log each report written.
Output: agents/reporting_agent.py with a ReportingAgent class extending BaseAgent, implementing process_message(message: dict) -> dict and generate_summary(results: list) -> dict.
```
**File to CREATE**: `agents/reporting_agent.py`
**Function to CREATE**: `ReportingAgent.process_message(message: dict) -> dict` and `ReportingAgent.generate_summary(results: list) -> dict`
**Details**:
- Write `{transaction_id}.json` to `shared/results/` for every transaction (including rejected)
- Result file contents: `transaction_id`, `final_status` ("settled" | "rejected" | "flagged"), `validation` block, `fraud_assessment` block (if scored), `processed_at` timestamp
- Mask account numbers in output files: `ACC-1001` → `***1001`
- `generate_summary()` produces `pipeline_summary.json` with counts and risk breakdown
- Transactions with `fraud_risk_level == "HIGH"` get `final_status: "flagged"`

---

### Task: Integrator / Orchestrator

**Prompt**:
```
Context: Python 3.11+ project with 3 agents (TransactionValidator, FraudDetector, ReportingAgent) in agents/ directory. Messages are passed as dicts through shared/ directories.
Task: Build the orchestrator that loads sample-transactions.json, wraps each record in the standard message format, runs them through all 3 agents in sequence, and reports results.
Rules:
  - Create shared/ directory structure (input/, processing/, output/, results/) at startup; clear any existing contents.
  - Wrap each raw transaction in the standard message envelope (message_id, timestamp, source_agent, target_agent, message_type, data).
  - Pipeline order: TransactionValidator → FraudDetector → ReportingAgent.
  - Write intermediate JSON files to shared/processing/ and shared/output/ as agents process.
  - Print a final summary table to stdout showing each transaction's outcome.
  - Handle agent exceptions gracefully — log and continue.
Output: integrator.py with main() function.
```
**File to CREATE**: `integrator.py`
**Function to CREATE**: `main()`
**Details**:
- Create/clear `shared/{input,processing,output,results}/` directories
- Load `sample-transactions.json`
- For each transaction: wrap in message envelope → validator → fraud_detector → reporting_agent
- Write JSON files at each stage to appropriate shared/ subdirectory
- Print summary table at end
- Exit code 0 on success, 1 if any transaction errored
