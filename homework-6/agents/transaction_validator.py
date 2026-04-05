"""Transaction Validator Agent — validates required fields, amounts, and currency codes."""

import argparse
import json
import sys
from decimal import Decimal, InvalidOperation
from pathlib import Path

from agents.base_agent import BaseAgent, mask_account

VALID_CURRENCIES = frozenset({
    "USD", "EUR", "GBP", "JPY", "CHF", "CAD", "AUD", "NZD",
    "CNY", "HKD", "SGD", "SEK", "NOK", "DKK", "INR", "BRL",
    "MXN", "ZAR", "KRW", "PLN",
})

REQUIRED_FIELDS = (
    "transaction_id",
    "amount",
    "currency",
    "source_account",
    "destination_account",
)


class TransactionValidator(BaseAgent):
    """Validates transactions for required fields, positive amounts, and ISO 4217 currency."""

    def __init__(self, shared_dir: Path | None = None):
        super().__init__("transaction_validator", shared_dir)

    def process_message(self, message: dict) -> dict:
        data = dict(message.get("data", {}))

        # Check required fields
        for field in REQUIRED_FIELDS:
            if field not in data or not str(data[field]).strip():
                reason = f"MISSING_FIELD:{field}"
                data["status"] = "rejected"
                data["rejection_reason"] = reason
                self.log_transaction(
                    data.get("transaction_id", "UNKNOWN"),
                    f"REJECTED — {reason}",
                )
                return self.create_message(data, "reporting_agent", message)

        txn_id = data["transaction_id"]

        # Validate amount is a positive Decimal
        try:
            amount = Decimal(str(data["amount"]))
        except (InvalidOperation, ValueError):
            reason = "INVALID_AMOUNT"
            data["status"] = "rejected"
            data["rejection_reason"] = reason
            self.log_transaction(txn_id, f"REJECTED — {reason}")
            return self.create_message(data, "reporting_agent", message)

        if amount <= 0:
            reason = "NEGATIVE_AMOUNT"
            data["status"] = "rejected"
            data["rejection_reason"] = reason
            self.log_transaction(txn_id, f"REJECTED — {reason}")
            return self.create_message(data, "reporting_agent", message)

        # Validate currency
        currency = str(data["currency"]).upper()
        if currency not in VALID_CURRENCIES:
            reason = "INVALID_CURRENCY"
            data["status"] = "rejected"
            data["rejection_reason"] = reason
            self.log_transaction(txn_id, f"REJECTED — {reason}")
            return self.create_message(data, "reporting_agent", message)

        # All checks passed
        data["status"] = "validated"
        data["validated_amount"] = str(amount)
        self.log_transaction(
            txn_id,
            f"VALIDATED — {amount} {currency} from {mask_account(data['source_account'])}",
        )
        return self.create_message(data, "fraud_detector", message)


def dry_run(transactions_path: Path) -> None:
    """Run validation on all transactions without file-based I/O, print results table."""
    with open(transactions_path) as f:
        transactions = json.load(f)

    validator = TransactionValidator()
    results = []

    for txn in transactions:
        msg = {
            "message_id": "dry-run",
            "timestamp": "dry-run",
            "source_agent": "dry_run",
            "target_agent": "transaction_validator",
            "message_type": "transaction",
            "data": txn,
        }
        result = validator.process_message(msg)
        data = result["data"]
        results.append({
            "transaction_id": data.get("transaction_id", "?"),
            "amount": data.get("amount", "?"),
            "currency": data.get("currency", "?"),
            "status": data.get("status", "?"),
            "reason": data.get("rejection_reason", "—"),
        })

    valid = sum(1 for r in results if r["status"] == "validated")
    invalid = sum(1 for r in results if r["status"] == "rejected")

    print(f"\n{'='*70}")
    print(f"  Transaction Validation Report (Dry Run)")
    print(f"{'='*70}")
    print(f"  Total: {len(results)}  |  Valid: {valid}  |  Invalid: {invalid}")
    print(f"{'='*70}")
    print(f"  {'ID':<10} {'Amount':>12} {'Currency':<10} {'Status':<12} {'Reason'}")
    print(f"  {'-'*10} {'-'*12} {'-'*10} {'-'*12} {'-'*20}")
    for r in results:
        print(
            f"  {r['transaction_id']:<10} {r['amount']:>12} {r['currency']:<10} "
            f"{r['status']:<12} {r['reason']}"
        )
    print(f"{'='*70}\n")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Transaction Validator Agent")
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Validate transactions without running the full pipeline",
    )
    parser.add_argument(
        "--input",
        type=str,
        default="sample-transactions.json",
        help="Path to transactions JSON file",
    )
    args = parser.parse_args()

    import logging
    logging.basicConfig(
        level=logging.INFO,
        format="[%(asctime)s] [%(name)s] %(message)s",
        datefmt="%Y-%m-%dT%H:%M:%S",
    )

    if args.dry_run:
        dry_run(Path(args.input))
    else:
        print("Use --dry-run for standalone validation, or run via integrator.py")
        sys.exit(1)
