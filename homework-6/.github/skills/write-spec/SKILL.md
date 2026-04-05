---
name: write-spec
description: 'Generate a project specification from template. Use when: writing a spec, creating specification.md, generating specification from template for banking pipeline.'
argument-hint: 'Optionally describe the project to generate a specification for'
---

# Write Specification Skill

## When to Use
- Creating a new project specification from the template
- Regenerating or updating `specification.md`
- Starting a new banking transaction pipeline project

## Procedure

1. **Read the template**: Load [specification-TEMPLATE-hint.md](../../specification-TEMPLATE-hint.md) for the required structure
2. **Analyze input data**: Read [sample-transactions.json](../../sample-transactions.json) to understand:
   - 8 transactions with varying amounts, currencies, timestamps, and countries
   - Edge cases: TXN006 has invalid currency "XYZ", TXN007 has negative amount
   - Cross-border: TXN004 from DE (Germany)
   - Unusual timing: TXN004 at 02:47 UTC
   - High-value: TXN002 ($25,000), TXN005 ($75,000)

3. **Generate `specification.md`** with these 5 sections:

### Section 1: High-Level Objective
One sentence: what the pipeline does, how many agents, what tech.

### Section 2: Mid-Level Objectives (5 items)
Each must be **testable**:
- Validation rules (rejected transactions get reason codes)
- Fraud scoring thresholds and risk levels
- Reporting: every transaction gets a result file in shared/results/
- Audit logging with ISO 8601, agent name, transaction ID
- Pipeline completeness: all 8 transactions processed

### Section 3: Implementation Notes
Include these mandatory constraints:
- `decimal.Decimal` for amounts — never `float`
- ISO 4217 currency whitelist (USD, EUR, GBP, JPY, CHF, CAD, AUD, NZD, CNY, HKD, SGD, SEK, NOK, DKK, INR, BRL, MXN, ZAR, KRW, PLN)
- PII masking: `ACC-1001` → `***1001` in all logs and output
- Logging format: `[ISO8601] [AGENT_NAME] [TXN_ID] message`
- Standard JSON message envelope with: message_id, timestamp, source_agent, target_agent, message_type, data
- Error handling: agents don't crash on malformed input

### Section 4: Context
- **Beginning**: `sample-transactions.json` with 8 records, empty workspace
- **Ending**: 8 result files + pipeline_summary.json in shared/results/, tests with ≥90% coverage, README and HOWTORUN

### Section 5: Low-Level Tasks
One entry per component. Each must have:
```
Task: [Name]
Prompt: "[Exact AI prompt]"
File to CREATE: path/to/file.py
Function to CREATE: ClassName.method_name(args) -> return_type
Details: [What it checks/transforms/decides]
```

Components:
- Transaction Validator (validates fields, amounts, currency)
- Fraud Detector (scores risk 0-10, assigns LOW/MEDIUM/HIGH)
- Reporting Agent (writes result files, generates summary)
- Integrator (orchestrates pipeline, manages shared/ directories)

4. **Write the file** to `specification.md` in the homework-6 project root

## Reference: Fraud Scoring Rules
| Trigger | Points |
|---------|--------|
| Amount > $10,000 | +3 |
| Amount > $50,000 | +4 (additional, total +7) |
| Timestamp 02:00–04:59 UTC | +2 |
| Cross-border (country ≠ US) | +1 |

Risk levels: LOW (0–2), MEDIUM (3–6), HIGH (7–10)
