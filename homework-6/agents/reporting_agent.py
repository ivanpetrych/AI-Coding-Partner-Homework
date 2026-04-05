"""Reporting Agent — generates final result files and pipeline summary."""

import json
from datetime import datetime, timezone
from pathlib import Path

from agents.base_agent import BaseAgent, mask_accounts_in_data


class ReportingAgent(BaseAgent):
    """Writes a result file for every transaction and generates pipeline summary."""

    def __init__(self, shared_dir: Path | None = None):
        super().__init__("reporting_agent", shared_dir)
        self._results: list[dict] = []

    @property
    def results(self) -> list[dict]:
        return list(self._results)

    def process_message(self, message: dict) -> dict:
        data = dict(message.get("data", {}))
        txn_id = data.get("transaction_id", "UNKNOWN")

        # Determine final status
        status = data.get("status", "unknown")
        if status == "rejected":
            final_status = "rejected"
        elif data.get("fraud_risk_level") == "HIGH":
            final_status = "flagged"
        elif status == "validated":
            final_status = "settled"
        else:
            final_status = "unknown"

        # Build result record
        result = {
            "transaction_id": txn_id,
            "final_status": final_status,
            "processed_at": datetime.now(timezone.utc).isoformat(),
            "validation": {
                "status": status,
                "rejection_reason": data.get("rejection_reason"),
            },
            "fraud_assessment": None,
            "transaction_data": mask_accounts_in_data(data),
        }

        if data.get("fraud_risk_score") is not None:
            result["fraud_assessment"] = {
                "risk_score": data["fraud_risk_score"],
                "risk_level": data["fraud_risk_level"],
                "risk_factors": data.get("fraud_risk_factors", []),
            }

        # Write individual result file
        results_dir = self.shared_dir / "results"
        self.write_json(results_dir, f"{txn_id}.json", result)
        self._results.append(result)

        self.log_transaction(txn_id, f"REPORTED — final_status={final_status}")

        data["final_status"] = final_status
        return self.create_message(data, "done", message)

    def generate_summary(self, results: list[dict] | None = None) -> dict:
        """Generate pipeline summary from collected results."""
        results = results if results is not None else self._results

        total = len(results)
        validated = sum(1 for r in results if r["validation"]["status"] == "validated")
        rejected = sum(1 for r in results if r["validation"]["status"] == "rejected")
        settled = sum(1 for r in results if r["final_status"] == "settled")
        flagged = sum(1 for r in results if r["final_status"] == "flagged")

        risk_breakdown = {"LOW": 0, "MEDIUM": 0, "HIGH": 0}
        for r in results:
            fa = r.get("fraud_assessment")
            if fa and fa.get("risk_level"):
                risk_breakdown[fa["risk_level"]] += 1

        rejection_reasons: dict[str, int] = {}
        for r in results:
            reason = r["validation"].get("rejection_reason")
            if reason:
                rejection_reasons[reason] = rejection_reasons.get(reason, 0) + 1

        summary = {
            "pipeline_run_at": datetime.now(timezone.utc).isoformat(),
            "total_transactions": total,
            "validated": validated,
            "rejected": rejected,
            "settled": settled,
            "flagged": flagged,
            "risk_breakdown": risk_breakdown,
            "rejection_reasons": rejection_reasons,
        }

        results_dir = self.shared_dir / "results"
        self.write_json(results_dir, "pipeline_summary.json", summary)
        self.logger.info(
            "SUMMARY — total=%d validated=%d rejected=%d settled=%d flagged=%d",
            total, validated, rejected, settled, flagged,
        )
        return summary
