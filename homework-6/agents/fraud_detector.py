"""Fraud Detector Agent — scores transactions for fraud risk on a 0-10 scale."""

from datetime import datetime
from decimal import Decimal, InvalidOperation
from pathlib import Path

from agents.base_agent import BaseAgent, mask_account

THRESHOLD_HIGH_VALUE = Decimal("10000")
THRESHOLD_VERY_HIGH_VALUE = Decimal("50000")
UNUSUAL_HOUR_START = 2
UNUSUAL_HOUR_END = 4  # inclusive: 02:00–04:59
DOMESTIC_COUNTRY = "US"


class FraudDetector(BaseAgent):
    """Scores validated transactions for fraud risk."""

    def __init__(self, shared_dir: Path | None = None):
        super().__init__("fraud_detector", shared_dir)

    def process_message(self, message: dict) -> dict:
        data = dict(message.get("data", {}))
        txn_id = data.get("transaction_id", "UNKNOWN")

        # Skip rejected transactions — pass through unchanged
        if data.get("status") != "validated":
            self.log_transaction(txn_id, "SKIPPED — not validated")
            return self.create_message(data, "reporting_agent", message)

        score = 0
        factors = []

        # Rule 1: High-value transaction
        try:
            amount = Decimal(str(data.get("amount", "0")))
        except (InvalidOperation, ValueError):
            amount = Decimal("0")

        if amount > THRESHOLD_VERY_HIGH_VALUE:
            score += 7  # +3 for >10k and +4 for >50k
            factors.append(f"amount_above_50000 (+7)")
        elif amount > THRESHOLD_HIGH_VALUE:
            score += 3
            factors.append(f"amount_above_10000 (+3)")

        # Rule 2: Unusual hour (02:00–04:59 UTC)
        timestamp_str = data.get("timestamp", "")
        try:
            ts = datetime.fromisoformat(timestamp_str.replace("Z", "+00:00"))
            if UNUSUAL_HOUR_START <= ts.hour <= UNUSUAL_HOUR_END:
                score += 2
                factors.append(f"unusual_hour_{ts.hour:02d}:00 (+2)")
        except (ValueError, AttributeError):
            pass

        # Rule 3: Cross-border
        metadata = data.get("metadata", {})
        country = metadata.get("country", DOMESTIC_COUNTRY)
        if country != DOMESTIC_COUNTRY:
            score += 1
            factors.append(f"cross_border_{country} (+1)")

        # Cap score at 10
        score = min(score, 10)

        # Determine risk level
        if score <= 2:
            risk_level = "LOW"
        elif score <= 6:
            risk_level = "MEDIUM"
        else:
            risk_level = "HIGH"

        data["fraud_risk_score"] = score
        data["fraud_risk_level"] = risk_level
        data["fraud_risk_factors"] = factors

        self.log_transaction(
            txn_id,
            f"SCORED — risk={score} level={risk_level} factors={factors}",
        )
        return self.create_message(data, "reporting_agent", message)
