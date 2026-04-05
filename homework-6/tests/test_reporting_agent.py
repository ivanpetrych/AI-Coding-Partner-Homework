"""Tests for the Reporting Agent."""

import json
import pytest
from agents.reporting_agent import ReportingAgent


@pytest.fixture
def reporter(shared_dirs):
    return ReportingAgent(shared_dirs)


class TestSettledTransaction:
    def test_settled_result_file(self, reporter, shared_dirs, scored_settled_message):
        result = reporter.process_message(scored_settled_message)
        result_file = shared_dirs / "results" / "TXN-TEST-SET.json"
        assert result_file.exists()
        data = json.loads(result_file.read_text())
        assert data["final_status"] == "settled"
        assert data["transaction_id"] == "TXN-TEST-SET"

    def test_settled_has_fraud_assessment(self, reporter, shared_dirs, scored_settled_message):
        reporter.process_message(scored_settled_message)
        data = json.loads((shared_dirs / "results" / "TXN-TEST-SET.json").read_text())
        assert data["fraud_assessment"] is not None
        assert data["fraud_assessment"]["risk_score"] == 0
        assert data["fraud_assessment"]["risk_level"] == "LOW"

    def test_settled_has_processed_at(self, reporter, shared_dirs, scored_settled_message):
        reporter.process_message(scored_settled_message)
        data = json.loads((shared_dirs / "results" / "TXN-TEST-SET.json").read_text())
        assert "processed_at" in data

    def test_settled_message_final_status(self, reporter, scored_settled_message):
        result = reporter.process_message(scored_settled_message)
        assert result["data"]["final_status"] == "settled"


class TestFlaggedTransaction:
    def test_high_risk_is_flagged(self, reporter, shared_dirs, scored_flagged_message):
        result = reporter.process_message(scored_flagged_message)
        result_file = shared_dirs / "results" / "TXN-TEST-FLAG.json"
        data = json.loads(result_file.read_text())
        assert data["final_status"] == "flagged"

    def test_flagged_has_risk_factors(self, reporter, shared_dirs, scored_flagged_message):
        reporter.process_message(scored_flagged_message)
        data = json.loads((shared_dirs / "results" / "TXN-TEST-FLAG.json").read_text())
        assert data["fraud_assessment"]["risk_score"] == 7
        assert len(data["fraud_assessment"]["risk_factors"]) > 0


class TestRejectedTransaction:
    def test_rejected_result_file(self, reporter, shared_dirs, rejected_message):
        result = reporter.process_message(rejected_message)
        result_file = shared_dirs / "results" / "TXN-TEST-REJ.json"
        assert result_file.exists()
        data = json.loads(result_file.read_text())
        assert data["final_status"] == "rejected"

    def test_rejected_has_reason(self, reporter, shared_dirs, rejected_message):
        reporter.process_message(rejected_message)
        data = json.loads((shared_dirs / "results" / "TXN-TEST-REJ.json").read_text())
        assert data["validation"]["rejection_reason"] == "INVALID_CURRENCY"

    def test_rejected_no_fraud_assessment(self, reporter, shared_dirs, rejected_message):
        reporter.process_message(rejected_message)
        data = json.loads((shared_dirs / "results" / "TXN-TEST-REJ.json").read_text())
        assert data["fraud_assessment"] is None


class TestAccountMasking:
    def test_accounts_masked_in_result(self, reporter, shared_dirs, scored_settled_message):
        reporter.process_message(scored_settled_message)
        data = json.loads((shared_dirs / "results" / "TXN-TEST-SET.json").read_text())
        txn_data = data["transaction_data"]
        assert txn_data["source_account"].startswith("***")
        assert txn_data["destination_account"].startswith("***")
        assert "ACC-" not in txn_data["source_account"]


class TestPipelineSummary:
    def test_summary_counts(self, reporter, shared_dirs, scored_settled_message, rejected_message, scored_flagged_message):
        reporter.process_message(scored_settled_message)
        reporter.process_message(rejected_message)
        reporter.process_message(scored_flagged_message)
        summary = reporter.generate_summary()
        assert summary["total_transactions"] == 3
        assert summary["validated"] == 2
        assert summary["rejected"] == 1
        assert summary["settled"] == 1
        assert summary["flagged"] == 1

    def test_summary_file_written(self, reporter, shared_dirs, scored_settled_message):
        reporter.process_message(scored_settled_message)
        reporter.generate_summary()
        summary_file = shared_dirs / "results" / "pipeline_summary.json"
        assert summary_file.exists()

    def test_summary_risk_breakdown(self, reporter, shared_dirs, scored_settled_message, scored_flagged_message):
        reporter.process_message(scored_settled_message)
        reporter.process_message(scored_flagged_message)
        summary = reporter.generate_summary()
        assert summary["risk_breakdown"]["LOW"] == 1
        assert summary["risk_breakdown"]["HIGH"] == 1

    def test_summary_rejection_reasons(self, reporter, shared_dirs, rejected_message):
        reporter.process_message(rejected_message)
        summary = reporter.generate_summary()
        assert "INVALID_CURRENCY" in summary["rejection_reasons"]

    def test_summary_with_explicit_results(self, reporter, shared_dirs):
        results = [
            {
                "final_status": "settled",
                "validation": {"status": "validated", "rejection_reason": None},
                "fraud_assessment": {"risk_level": "LOW"},
            },
        ]
        summary = reporter.generate_summary(results)
        assert summary["total_transactions"] == 1
        assert summary["settled"] == 1

    def test_results_property(self, reporter, scored_settled_message):
        assert reporter.results == []
        reporter.process_message(scored_settled_message)
        assert len(reporter.results) == 1


class TestUnknownStatus:
    def test_unknown_status_transaction(self, reporter, shared_dirs):
        msg = {
            "data": {
                "transaction_id": "TXN-UNK",
                "status": "something_else",
            }
        }
        result = reporter.process_message(msg)
        assert result["data"]["final_status"] == "unknown"
