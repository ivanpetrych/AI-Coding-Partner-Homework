# Multi-Agent Banking Transaction Pipeline

> Created by **Ivan Petrych**

## Overview

A multi-agent banking transaction processing pipeline built in Python. The system validates, scores for fraud risk, and reports on banking transactions using file-based JSON message passing through shared directories. Four meta-agents (specification writer, code generator, test runner, documentation generator) produce and maintain the pipeline.

The pipeline processes transactions from `sample-transactions.json` through three cooperating agents: a Transaction Validator that checks required fields, amounts, and ISO 4217 currency codes; a Fraud Detector that scores risk on a 0–10 scale based on amount thresholds, unusual timing, and cross-border indicators; and a Reporting Agent that generates per-transaction result files and a pipeline summary with aggregate statistics.

## Agent Responsibilities

- **Transaction Validator** — validates required fields (transaction_id, amount, currency, source_account, destination_account), ensures amounts are positive Decimals, and checks currency codes against an ISO 4217 whitelist. Rejects invalid transactions with specific reason codes.
- **Fraud Detector** — scores validated transactions for fraud risk (0–10 scale). Triggers: amount >$10k (+3), >$50k (+4 additional), unusual hour 02:00–04:59 UTC (+2), cross-border (+1). Risk levels: LOW (0–2), MEDIUM (3–6), HIGH (7–10).
- **Reporting Agent** — writes a final result JSON file for every transaction (including rejected ones) to `shared/results/`, and generates `pipeline_summary.json` with counts, risk breakdown, and rejection reasons.

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        INTEGRATOR                               │
│  Loads sample-transactions.json, wraps in message envelopes,    │
│  orchestrates agents in sequence, prints summary table          │
└──────────────────────────┬──────────────────────────────────────┘
                           │
                           ▼
              ┌────────────────────────┐
              │  Transaction Validator  │
              │  • Required fields     │
              │  • Positive Decimal    │  ──rejected──┐
              │  • ISO 4217 currency   │              │
              └───────────┬────────────┘              │
                          │ validated                  │
                          ▼                            │
              ┌────────────────────────┐              │
              │    Fraud Detector      │              │
              │  • Amount thresholds   │              │
              │  • Unusual timing      │              │
              │  • Cross-border check  │              │
              └───────────┬────────────┘              │
                          │ scored                    │
                          ▼                            │
              ┌────────────────────────┐              │
              │   Reporting Agent      │ ◄────────────┘
              │  • Result files        │
              │  • Pipeline summary    │
              │  • Account masking     │
              └───────────┬────────────┘
                          │
                          ▼
              ┌────────────────────────┐
              │   shared/results/      │
              │  TXN001.json ...       │
              │  pipeline_summary.json │
              └────────────────────────┘
```

### File-Based Communication

```
shared/
├── input/       ← integrator drops initial messages
├── processing/  ← validator writes validated/rejected messages
├── output/      ← fraud detector writes scored messages
└── results/     ← reporting agent writes final results + summary
```

## Tech Stack

| Component | Technology |
|---|---|
| Language | Python 3.11+ |
| Monetary Arithmetic | `decimal.Decimal` (stdlib) |
| Testing | pytest + pytest-cov |
| MCP Server | FastMCP 3.x |
| Logging | `logging` (stdlib, structured format) |
| Date/Time | `datetime` (stdlib, UTC, ISO 8601) |

## Quick Start

```bash
cd homework-6
pip install -r requirements.txt
python3 integrator.py
```

See [HOWTORUN.md](HOWTORUN.md) for detailed step-by-step instructions.

## Sample Results

| Transaction | Amount | Status | Risk | Details |
|---|---|---|---|---|
| TXN001 | $1,500 USD | settled | LOW (0) | — |
| TXN002 | $25,000 USD | settled | MEDIUM (3) | amount >$10k |
| TXN003 | $9,999.99 USD | settled | LOW (0) | — |
| TXN004 | €500 EUR | settled | MEDIUM (3) | 2:47am + cross-border |
| TXN005 | $75,000 USD | flagged | HIGH (7) | amount >$50k |
| TXN006 | 200 XYZ | rejected | — | INVALID_CURRENCY |
| TXN007 | -£100 GBP | rejected | — | NEGATIVE_AMOUNT |
| TXN008 | $3,200 USD | settled | LOW (0) | — |

## Project Structure

```
homework-6/
├── integrator.py              # Pipeline orchestrator
├── agents/
│   ├── base_agent.py          # Shared base class, PII masking
│   ├── transaction_validator.py
│   ├── fraud_detector.py
│   └── reporting_agent.py
├── tests/                     # pytest suite (94% coverage)
├── mcp/
│   └── server.py              # FastMCP server (query pipeline results)
├── .github/
│   ├── agents/                # Custom Copilot agents
│   ├── prompts/               # Slash commands
│   ├── hooks/                 # Coverage gate hook
│   └── skills/                # Write-spec skill
├── specification.md
├── agents.md
├── research-notes.md
├── sample-transactions.json
└── docs/screenshots/
```
