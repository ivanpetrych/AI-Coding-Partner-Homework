"""Pipeline Integrator — orchestrates the multi-agent banking transaction pipeline."""

import json
import logging
import shutil
import uuid
from datetime import datetime, timezone
from pathlib import Path

from agents.base_agent import mask_account
from agents.transaction_validator import TransactionValidator
from agents.fraud_detector import FraudDetector
from agents.reporting_agent import ReportingAgent

SHARED_DIR = Path("shared")
SUBDIRS = ["input", "processing", "output", "results"]

logging.basicConfig(
    level=logging.INFO,
    format="[%(asctime)s] [%(name)s] %(message)s",
    datefmt="%Y-%m-%dT%H:%M:%S",
)
logger = logging.getLogger("integrator")


def setup_directories() -> None:
    """Create or clear the shared directory structure."""
    for subdir in SUBDIRS:
        path = SHARED_DIR / subdir
        if path.exists():
            shutil.rmtree(path)
        path.mkdir(parents=True, exist_ok=True)
    logger.info("Shared directories ready: %s", ", ".join(SUBDIRS))


def load_transactions(filepath: Path) -> list[dict]:
    """Load raw transactions from a JSON file."""
    with open(filepath) as f:
        transactions = json.load(f)
    logger.info("Loaded %d transactions from %s", len(transactions), filepath)
    return transactions


def wrap_transaction(txn: dict) -> dict:
    """Wrap a raw transaction in the standard message envelope."""
    return {
        "message_id": str(uuid.uuid4()),
        "timestamp": datetime.now(timezone.utc).isoformat(),
        "source_agent": "integrator",
        "target_agent": "transaction_validator",
        "message_type": "transaction",
        "data": txn,
    }


def run_pipeline(transactions_path: Path = Path("sample-transactions.json")) -> dict:
    """Run the full pipeline and return the summary."""
    setup_directories()
    transactions = load_transactions(transactions_path)

    validator = TransactionValidator(SHARED_DIR)
    fraud_detector = FraudDetector(SHARED_DIR)
    reporter = ReportingAgent(SHARED_DIR)

    results = []

    for txn in transactions:
        txn_id = txn.get("transaction_id", "UNKNOWN")
        try:
            # Wrap in message envelope and write to input/
            message = wrap_transaction(txn)
            validator.write_json(
                SHARED_DIR / "input",
                f"{txn_id}.json",
                message,
            )

            # Stage 1: Validation
            validated_msg = validator.process_message(message)
            validator.write_json(
                SHARED_DIR / "processing",
                f"{txn_id}.json",
                validated_msg,
            )

            # Stage 2: Fraud Detection
            scored_msg = fraud_detector.process_message(validated_msg)
            fraud_detector.write_json(
                SHARED_DIR / "output",
                f"{txn_id}.json",
                scored_msg,
            )

            # Stage 3: Reporting
            final_msg = reporter.process_message(scored_msg)
            results.append(final_msg["data"])

        except Exception:
            logger.exception("Error processing %s", txn_id)
            results.append({
                "transaction_id": txn_id,
                "final_status": "error",
            })

    # Generate summary
    summary = reporter.generate_summary()

    # Print results table
    print_summary(results, summary)

    return summary


def print_summary(results: list[dict], summary: dict) -> None:
    """Print a formatted summary table to stdout."""
    print(f"\n{'='*78}")
    print("  Multi-Agent Banking Pipeline — Results")
    print(f"{'='*78}")
    print(
        f"  Total: {summary['total_transactions']}  |  "
        f"Validated: {summary['validated']}  |  "
        f"Rejected: {summary['rejected']}  |  "
        f"Settled: {summary['settled']}  |  "
        f"Flagged: {summary['flagged']}"
    )
    print(f"{'='*78}")
    print(
        f"  {'ID':<10} {'Amount':>12} {'Currency':<6} "
        f"{'Status':<12} {'Risk':<8} {'Score':<6} {'Details'}"
    )
    print(f"  {'-'*10} {'-'*12} {'-'*6} {'-'*12} {'-'*8} {'-'*6} {'-'*20}")

    for r in results:
        txn_id = r.get("transaction_id", "?")
        amount = r.get("amount", "?")
        currency = r.get("currency", "?")
        final_status = r.get("final_status", "?")
        risk_level = r.get("fraud_risk_level", "—")
        risk_score = r.get("fraud_risk_score", "—")
        reason = r.get("rejection_reason", "")
        factors = ", ".join(r.get("fraud_risk_factors", []))
        details = reason or factors or "—"

        print(
            f"  {txn_id:<10} {amount:>12} {currency:<6} "
            f"{final_status:<12} {str(risk_level):<8} {str(risk_score):<6} {details}"
        )

    print(f"{'='*78}")

    if summary.get("rejection_reasons"):
        print("\n  Rejection Reasons:")
        for reason, count in summary["rejection_reasons"].items():
            print(f"    {reason}: {count}")

    risk = summary.get("risk_breakdown", {})
    if any(risk.values()):
        print("\n  Risk Breakdown:")
        for level in ("LOW", "MEDIUM", "HIGH"):
            print(f"    {level}: {risk.get(level, 0)}")

    print()


def main() -> None:
    summary = run_pipeline()
    total = summary["total_transactions"]
    errors = total - summary["validated"] - summary["rejected"]
    exit_code = 1 if errors > 0 else 0
    raise SystemExit(exit_code)


if __name__ == "__main__":
    main()
