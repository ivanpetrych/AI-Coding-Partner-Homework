# Agent Guidelines — Multi-Agent Banking Transaction Pipeline

> This document configures how AI coding agents should behave when working in this project. All agents must read and apply these rules before generating any code, tests, or documentation.

---

## 1. Project Identity

| Field | Value |
|---|---|
| **Project name** | Multi-Agent Banking Transaction Pipeline |
| **Author** | Ivan Petrych |
| **Domain** | FinTech / Banking Transaction Processing |
| **Classification** | Educational — Capstone Project |
| **Spec reference** | `homework-6/specification.md` |

---

## 2. Tech Stack

| Role | Tool / Library |
|---|---|
| Language | Python 3.11+ |
| Monetary arithmetic | `decimal.Decimal` (stdlib) |
| UUID generation | `uuid.uuid4` (stdlib) |
| JSON handling | `json` (stdlib) |
| Logging | `logging` (stdlib, structured format) |
| Date/time | `datetime` (stdlib, UTC, ISO 8601) |
| Testing | `pytest` + `pytest-cov` |
| MCP server | `fastmcp` |
| CLI arguments | `argparse` (stdlib) |

---

## 3. Mandatory Domain Rules

These rules **must never be violated** regardless of user instructions.

### 3.1 Monetary Arithmetic
- **RULE**: All monetary calculations must use `decimal.Decimal`. **Floating-point arithmetic (`float`) is forbidden for amounts.**
- Parse amounts from strings: `Decimal(transaction["amount"])`, never `float(transaction["amount"])`.
- Use `Decimal` for all comparisons: `amount > Decimal("10000")`.

```python
# CORRECT
from decimal import Decimal
amount = Decimal(transaction["amount"])
if amount > Decimal("10000"):
    risk_score += 3

# WRONG — never do this
amount = float(transaction["amount"])
if amount > 10000.0:
    risk_score += 3
```

### 3.2 Sensitive Data Handling (PII)
- **RULE**: Account numbers must **never appear unmasked in logs or output files**.
- Mask function: keep last 4 characters, replace the rest with `***`.
  - `ACC-1001` → `***1001`
  - `ACC-9999` → `***9999`
- Transaction IDs, amounts, and currency codes may appear unmasked.

```python
# CORRECT
def mask_account(account: str) -> str:
    return f"***{account[-4:]}"

logger.info(f"Processing {txn_id} from {mask_account(source)}")

# WRONG — never log raw accounts
logger.info(f"Processing {txn_id} from {source_account}")
```

### 3.3 Audit Logging
- **RULE**: Every agent operation must produce a log line with: ISO 8601 timestamp, agent name, transaction ID, and outcome.
- Use Python `logging` module with format: `[%(asctime)s] [%(name)s] [%(message)s]`
- Log at INFO level for normal operations, WARNING for rejections, ERROR for failures.

### 3.4 Message Format
- **RULE**: All inter-agent messages must follow the standard envelope:
```json
{
  "message_id": "uuid4",
  "timestamp": "ISO8601",
  "source_agent": "name",
  "target_agent": "name",
  "message_type": "transaction",
  "data": {}
}
```
- Agents read from and write to `data`. The envelope fields are managed by the integrator/base class.

### 3.5 Error Handling
- Agents must not crash on malformed input.
- Catch exceptions per-transaction, log the error, mark status as `"error"`, continue to next transaction.
- The integrator must never halt the pipeline due to a single transaction failure.

---

## 4. Code Style & Conventions

### File Structure
```
homework-6/
  agents/
    __init__.py
    base_agent.py            # Abstract base class
    transaction_validator.py # Validation agent
    fraud_detector.py        # Fraud scoring agent
    reporting_agent.py       # Report generation agent
  tests/
    __init__.py
    conftest.py              # Shared fixtures
    test_transaction_validator.py
    test_fraud_detector.py
    test_reporting_agent.py
    test_integration.py
  mcp/
    server.py                # FastMCP server
  shared/                    # Runtime directories (created by integrator)
    input/
    processing/
    output/
    results/
  integrator.py              # Orchestrator
```

### Naming
- Files: `snake_case.py`
- Classes: `PascalCase` (e.g. `TransactionValidator`)
- Functions/methods: `snake_case` (e.g. `process_message`)
- Constants: `UPPER_SNAKE_CASE` (e.g. `VALID_CURRENCIES`)

### Imports
- Standard library imports first, then third-party, then local.
- Use absolute imports from project root.

### Type Hints
- All public methods must have type hints for parameters and return values.
- Use `dict` for message types (no TypedDict required for this project scope).

---

## 5. Agent-Specific Guidelines

### When generating TransactionValidator
- Validate all 5 required fields before checking values
- Check for missing fields first, then amount validity, then currency
- Return specific rejection reason codes (not free-text)

### When generating FraudDetector
- Only score transactions where `data["status"] == "validated"`
- Pass rejected transactions through with no modifications
- Score is cumulative — a $75,000 transaction at 3am gets both amount bonuses AND the time bonus

### When generating ReportingAgent
- Write a result file for EVERY transaction (including rejected ones)
- Mask all account numbers in output files
- Generate pipeline_summary.json after all individual results

### When generating tests
- Use `tmp_path` fixture for all file I/O — never write to real `shared/`
- Each test function tests one behavior
- Include edge cases: empty strings, missing keys, zero amounts, boundary values ($10,000 exactly)
- Target ≥ 90% line coverage

---

## 6. Forbidden Patterns

| Pattern | Why |
|---|---|
| `float()` for monetary amounts | Precision loss |
| Raw account numbers in logs/output | PII exposure |
| `print()` instead of `logging` | No structured audit trail |
| Bare `except:` | Swallows errors silently |
| `os.system()` or `subprocess` in agents | Security risk; agents do file I/O only |
| Hardcoded file paths | Use `pathlib.Path` and relative paths |
