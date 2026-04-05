---
description: "Validate all transactions without running the full pipeline. Use when: checking transactions, dry-run validation, verifying input data, pre-flight check."
---
Validate all transactions in sample-transactions.json without processing them through the full pipeline.

## Steps

1. **Run the validator in dry-run mode** from the `homework-6/` directory:
   ```bash
   cd homework-6 && PYTHONPATH=. python3 agents/transaction_validator.py --dry-run
   ```

2. **Report summary**: Show totals — total count, valid count, invalid count.

3. **Show results table** with columns: Transaction ID, Amount, Currency, Status, Reason for rejection (if any).

4. **Highlight issues**: If any transactions were rejected, explain:
   - `INVALID_CURRENCY` — currency code not in ISO 4217 whitelist
   - `NEGATIVE_AMOUNT` — amount is zero or negative
   - `MISSING_FIELD:<name>` — a required field is missing
   - `INVALID_AMOUNT` — amount cannot be parsed as a number
