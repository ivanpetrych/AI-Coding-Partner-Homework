"""Custom FastMCP server — makes the banking pipeline queryable."""

import json
from pathlib import Path

from fastmcp import FastMCP

RESULTS_DIR = Path(__file__).resolve().parent.parent / "shared" / "results"

mcp = FastMCP("pipeline-status")


@mcp.tool()
def get_transaction_status(transaction_id: str) -> str:
    """Get the current status of a transaction by its ID.

    Args:
        transaction_id: The transaction ID to look up (e.g. 'TXN001').

    Returns:
        JSON string with the transaction's current status and details.
    """
    result_file = RESULTS_DIR / f"{transaction_id}.json"
    if not result_file.exists():
        return json.dumps({
            "error": f"Transaction {transaction_id} not found",
            "available": _list_available_ids(),
        })

    with open(result_file) as f:
        data = json.load(f)

    return json.dumps({
        "transaction_id": data.get("transaction_id"),
        "final_status": data.get("final_status"),
        "validation_status": data.get("validation", {}).get("status"),
        "rejection_reason": data.get("validation", {}).get("rejection_reason"),
        "fraud_assessment": data.get("fraud_assessment"),
        "processed_at": data.get("processed_at"),
    }, indent=2)


@mcp.tool()
def list_pipeline_results() -> str:
    """List a summary of all processed transactions from the latest pipeline run.

    Returns:
        JSON string with summary statistics and per-transaction status.
    """
    summary_file = RESULTS_DIR / "pipeline_summary.json"
    if not summary_file.exists():
        return json.dumps({"error": "No pipeline results found. Run the pipeline first."})

    with open(summary_file) as f:
        summary = json.load(f)

    # Also gather individual statuses
    transactions = []
    for file in sorted(RESULTS_DIR.glob("TXN*.json")):
        with open(file) as f:
            data = json.load(f)
        transactions.append({
            "transaction_id": data.get("transaction_id"),
            "final_status": data.get("final_status"),
            "risk_level": (data.get("fraud_assessment") or {}).get("risk_level"),
        })

    return json.dumps({
        "summary": summary,
        "transactions": transactions,
    }, indent=2)


@mcp.resource("pipeline://summary")
def pipeline_summary() -> str:
    """Returns the latest pipeline run summary as human-readable text."""
    summary_file = RESULTS_DIR / "pipeline_summary.json"
    if not summary_file.exists():
        return "No pipeline results found. Run the pipeline first with: python3 integrator.py"

    with open(summary_file) as f:
        summary = json.load(f)

    lines = [
        "=== Pipeline Run Summary ===",
        f"Run at: {summary.get('pipeline_run_at', 'unknown')}",
        "",
        f"Total transactions: {summary.get('total_transactions', 0)}",
        f"  Validated: {summary.get('validated', 0)}",
        f"  Rejected:  {summary.get('rejected', 0)}",
        f"  Settled:   {summary.get('settled', 0)}",
        f"  Flagged:   {summary.get('flagged', 0)}",
        "",
        "Risk Breakdown:",
    ]

    for level, count in summary.get("risk_breakdown", {}).items():
        lines.append(f"  {level}: {count}")

    reasons = summary.get("rejection_reasons", {})
    if reasons:
        lines.append("")
        lines.append("Rejection Reasons:")
        for reason, count in reasons.items():
            lines.append(f"  {reason}: {count}")

    return "\n".join(lines)


def _list_available_ids() -> list[str]:
    """List available transaction IDs in results."""
    if not RESULTS_DIR.exists():
        return []
    return sorted(
        f.stem for f in RESULTS_DIR.glob("TXN*.json")
    )


if __name__ == "__main__":
    mcp.run()
