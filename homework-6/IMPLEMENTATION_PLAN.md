# Implementation Plan — Homework 6: AI-Powered Multi-Agent Banking Pipeline

## Tech Stack Decision

| Component | Choice | Rationale |
|---|---|---|
| Language | **Python 3.11+** | Best match for FastMCP, `decimal.Decimal`, and examples in TASKS.md |
| Testing | **pytest + pytest-cov** | Standard Python testing, easy coverage gating |
| MCP Server | **FastMCP (Python)** | Required by Task 4 |
| Package manager | **pip + requirements.txt** | Simple, no extra tooling needed |
| Hook mechanism | **Git pre-push hook** | Blocks push if coverage < 80% |

## Project Structure (Target)

```
homework-6/
├── agents.md
├── specification.md
├── research-notes.md
├── README.md
├── HOWTORUN.md
├── requirements.txt
├── mcp.json
├── sample-transactions.json          # already exists
├── specification-TEMPLATE-hint.md    # already exists
├── TASKS.md                          # already exists
├── integrator.py                     # orchestrator — runs the full pipeline
├── agents/
│   ├── __init__.py
│   ├── base_agent.py                 # shared base class for message handling
│   ├── transaction_validator.py      # Agent 1: validates fields, amounts, currency
│   ├── fraud_detector.py            # Agent 2: scores fraud risk
│   └── reporting_agent.py            # Agent 3: generates reports and pipeline summary
├── shared/                           # file-based communication (created at runtime)
│   ├── input/
│   ├── processing/
│   ├── output/
│   └── results/
├── mcp/
│   └── server.py                     # FastMCP server with tools + resource
├── tests/
│   ├── __init__.py
│   ├── conftest.py                   # shared fixtures (tmp_path-based shared dirs)
│   ├── test_transaction_validator.py
│   ├── test_fraud_detector.py
│   ├── test_settlement_processor.py
│   └── test_integration.py           # end-to-end pipeline test
├── .github/
│   └── copilot/
│       └── commands/
│           ├── write-spec.md         # Skill: generate specification from template
│           ├── run-pipeline.md       # Skill: run pipeline end-to-end
│           └── validate-transactions.md  # Skill: dry-run validation
├── .git/
│   └── hooks/
│       └── pre-push                  # coverage gate hook (blocks push if < 80%)
└── docs/
    └── screenshots/                  # 5 screenshots (manual capture)
        ├── pipeline-run.png
        ├── test-coverage.png
        ├── skill-run-pipeline.png
        ├── hook-trigger.png
        └── mcp-interaction.png
```

---

## Step-by-Step Plan

### Step 1 — Specification & Agents (Task 1 / Agent 1)

**Deliverables:** `specification.md`, `agents.md`, `.github/copilot/commands/write-spec.md`

1. **Create `specification.md`** following the template structure:
   - High-Level Objective: one-sentence pipeline description
   - Mid-Level Objectives: 5 testable requirements (invalid currency rejection, high-value fraud flagging, settlement processing, audit logging, result file generation)
   - Implementation Notes: Decimal arithmetic, ISO 4217, PII masking, logging format
   - Context: beginning state (sample-transactions.json, empty workspace) → ending state (shared/results/ populated, ≥90% coverage)
   - Low-Level Tasks: one entry per agent with exact prompts, files, functions, and details

2. **Create `agents.md`** with project-specific agent guidelines:
   - Project identity, tech stack, domain rules
   - Mandatory rules: Decimal for money, PII masking, audit logging, JSON message format

3. **Create `.github/copilot/commands/write-spec.md`** — slash command that generates a spec from the template

---

### Step 2 — Pipeline Code (Task 2 / Agent 2)

**Deliverables:** `integrator.py`, `agents/*.py`, `requirements.txt`

1. **Create `requirements.txt`** — dependencies (fastmcp, pytest, pytest-cov)

2. **Create `agents/base_agent.py`** — base class with:
   - `process_message(message: dict) -> dict` abstract method
   - File I/O helpers: read from input dir, write to output dir
   - Logging with ISO 8601 timestamps, agent name, transaction ID
   - PII masking for account numbers in logs

3. **Create `agents/transaction_validator.py`**:
   - Validates required fields: transaction_id, amount, currency, source_account, destination_account
   - Validates amount is positive Decimal (rejects negative like TXN007)
   - Validates currency against ISO 4217 whitelist (rejects XYZ like TXN006)
   - Returns status "validated" or "rejected" with reason

4. **Create `agents/fraud_detector.py`**:
   - Scoring: amount > $10,000 (+3), amount > $50,000 (+4), unusual hour 2am-5am (+2), cross-border (+1)
   - Risk levels: LOW (0-2), MEDIUM (3-6), HIGH (7-10)
   - Only processes validated transactions (skips rejected)

5. **Create `agents/reporting_agent.py`**:
   - Generates final report for each transaction (summary of validation + fraud assessment)
   - Writes final result JSON to shared/results/
   - Produces pipeline summary report with statistics (total, validated, rejected, risk breakdown)

6. **Create `integrator.py`**:
   - Creates shared/ directory structure
   - Loads sample-transactions.json, wraps each in standard message format (uuid, timestamp, source_agent, target_agent, message_type, data)
   - Runs agents in sequence: validator → fraud_detector → settlement_processor
   - Monitors and reports results

**Expected behavior with sample data:**
| TXN | Validator | Fraud Detector | Settlement |
|-----|-----------|----------------|------------|
| TXN001 ($1,500 USD) | ✅ validated | LOW risk (0) | ✅ reported |
| TXN002 ($25,000 USD) | ✅ validated | MEDIUM risk (3) | ✅ reported |
| TXN003 ($9,999.99 USD) | ✅ validated | LOW risk (0) | ✅ reported |
| TXN004 ($500 EUR, 2:47am, DE) | ✅ validated | MEDIUM risk (3) — unusual hour + cross-border | ✅ reported |
| TXN005 ($75,000 USD) | ✅ validated | HIGH risk (7) — >$10k + >$50k | ✅ reported (flagged) |
| TXN006 ($200 XYZ) | ❌ rejected (INVALID_CURRENCY) | skipped | ✅ reported (rejected) |
| TXN007 (-$100 GBP) | ❌ rejected (NEGATIVE_AMOUNT) | skipped | ✅ reported (rejected) |
| TXN008 ($3,200 USD) | ✅ validated | LOW risk (0) | ✅ reported |

---

### Step 3 — Skills & Hooks (Task 3 / Agent 3)

**Deliverables:** 2 command files, pre-push hook

1. **Create `.github/copilot/commands/run-pipeline.md`** — slash command instructions to:
   - Check sample-transactions.json exists
   - Clear shared/ directories
   - Run `python integrator.py`
   - Summarize results and rejected transactions

2. **Create `.github/copilot/commands/validate-transactions.md`** — slash command to:
   - Run validator in dry-run mode
   - Report total/valid/invalid counts with reasons table

3. **Add `--dry-run` flag to `agents/transaction_validator.py`** so it can be invoked standalone

4. **Create `hooks/pre-push`** — shell script (to be manually copied to `.git/hooks/`):
   - Runs `pytest --cov=agents --cov=integrator --cov-fail-under=80`
   - Exits non-zero (blocking push) if coverage < 80%
   - Instructions in HOWTORUN.md on how to install it

---

### Step 4 — MCP Integration (Task 4)

**Deliverables:** `mcp.json`, `mcp/server.py`, `research-notes.md`

1. **Create `mcp.json`** with both servers configured:
   - `context7`: `npx -y @upstash/context7-mcp@latest`
   - `pipeline-status`: `python mcp/server.py`

2. **Create `mcp/server.py`** using FastMCP:
   - Tool `get_transaction_status(transaction_id: str)` — reads from shared/results/, returns status
   - Tool `list_pipeline_results()` — returns summary of all processed transactions
   - Resource `pipeline://summary` — returns latest pipeline run summary as text

3. **Create `research-notes.md`** with 2+ context7 queries:
   - Query 1: Python decimal module / monetary arithmetic
   - Query 2: FastMCP / MCP server implementation patterns
   - (We will make actual context7 queries during Step 2 implementation)

---

### Step 5 — Tests & Documentation (Task 5 / Agent 4)

**Deliverables:** `tests/`, `README.md`, `HOWTORUN.md`

1. **Create `tests/conftest.py`** — shared fixtures:
   - `tmp_shared_dirs` fixture creating isolated shared/ structure in tmp_path
   - Sample message fixtures

2. **Create `tests/test_transaction_validator.py`**:
   - Valid transaction passes
   - Missing required field → rejected
   - Negative amount → rejected
   - Invalid currency → rejected
   - Edge cases (zero amount, boundary values)

3. **Create `tests/test_fraud_detector.py`**:
   - Low risk transaction (small amount, business hours, domestic)
   - Medium risk (>$10k or unusual hour or cross-border)
   - High risk (multiple triggers, >$50k)
   - Already-rejected transactions are skipped

3. **Create `tests/test_reporting_agent.py`**:
   - Normal reporting succeeds
   - Report contains all required fields (transaction summary, risk info)
   - Rejected transactions are included in report with rejection reason
   - Pipeline summary statistics are correct

5. **Create `tests/test_integration.py`**:
   - Full pipeline end-to-end with sample-transactions.json
   - All 8 transactions processed, 6 settled, 2 rejected
   - Results written to shared/results/

6. **Create `README.md`**:
   - Student name (will ask you for this)
   - System description
   - Agent responsibilities (bullet per agent)
   - ASCII architecture diagram
   - Tech stack table

7. **Create `HOWTORUN.md`**:
   - Prerequisites (Python 3.11+)
   - Install dependencies
   - Run pipeline
   - Run tests
   - Run MCP server

---

### Step 6 — Screenshots & Final Review

**Manual steps (you do these):**

| Screenshot | How to capture |
|---|---|
| `pipeline-run.png` | Run `python integrator.py` and screenshot terminal |
| `test-coverage.png` | Run `pytest --cov` and screenshot coverage report |
| `skill-run-pipeline.png` | Invoke `/run-pipeline` in Copilot Chat and screenshot |
| `hook-trigger.png` | Attempt `git push` with failing/passing coverage and screenshot |
| `mcp-interaction.png` | Use context7 query or custom MCP tool and screenshot |

---

## Implementation Order & Dependencies

```
Step 1 (specification.md, agents.md, write-spec skill)
   ↓
Step 2 (pipeline code — integrator + 3 agents)
   ↓
Step 3 (skills + hooks — depends on working pipeline)
   ↓
Step 4 (MCP server — depends on shared/results/ existing)
   ↓
Step 5 (tests + docs — depends on all code being written)
   ↓
Step 6 (screenshots — manual, after everything works)
```

---

## Resolved Questions

1. **Name**: Ivan Petrych
2. **Third agent**: Reporting Agent
3. **Git**: Initialized, but no git operations — just create hook script files, skills, and agents
