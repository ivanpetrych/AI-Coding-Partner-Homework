"""Integration test — full pipeline end-to-end."""

import json
import pytest
from pathlib import Path

from agents.transaction_validator import TransactionValidator
from agents.fraud_detector import FraudDetector
from agents.reporting_agent import ReportingAgent
from integrator import wrap_transaction, setup_directories, load_transactions, run_pipeline


class TestWrapTransaction:
    def test_wraps_with_envelope(self):
        txn = {"transaction_id": "TXN001", "amount": "100.00"}
        msg = wrap_transaction(txn)
        assert "message_id" in msg
        assert "timestamp" in msg
        assert msg["source_agent"] == "integrator"
        assert msg["target_agent"] == "transaction_validator"
        assert msg["message_type"] == "transaction"
        assert msg["data"] == txn

    def test_unique_message_ids(self):
        txn = {"transaction_id": "TXN001"}
        msg1 = wrap_transaction(txn)
        msg2 = wrap_transaction(txn)
        assert msg1["message_id"] != msg2["message_id"]


class TestLoadTransactions:
    def test_loads_valid_json(self, sample_transactions_file):
        txns = load_transactions(sample_transactions_file)
        assert len(txns) == 4
        assert txns[0]["transaction_id"] == "TXN001"

    def test_file_not_found(self):
        with pytest.raises(FileNotFoundError):
            load_transactions(Path("/nonexistent/file.json"))


class TestFullPipeline:
    def test_pipeline_processes_all_transactions(self, sample_transactions_file, monkeypatch):
        work_dir = sample_transactions_file.parent
        monkeypatch.chdir(work_dir)

        summary = run_pipeline(sample_transactions_file)

        assert summary["total_transactions"] == 4
        assert summary["validated"] == 2  # TXN001, TXN002
        assert summary["rejected"] == 2   # TXN006 (XYZ), TXN007 (negative)

    def test_result_files_created(self, sample_transactions_file, monkeypatch):
        work_dir = sample_transactions_file.parent
        monkeypatch.chdir(work_dir)

        run_pipeline(sample_transactions_file)

        results_dir = work_dir / "shared" / "results"
        assert results_dir.exists()
        result_files = list(results_dir.glob("TXN*.json"))
        assert len(result_files) == 4

    def test_pipeline_summary_file(self, sample_transactions_file, monkeypatch):
        work_dir = sample_transactions_file.parent
        monkeypatch.chdir(work_dir)

        run_pipeline(sample_transactions_file)

        summary_file = work_dir / "shared" / "results" / "pipeline_summary.json"
        assert summary_file.exists()
        summary = json.loads(summary_file.read_text())
        assert "total_transactions" in summary

    def test_shared_directories_created(self, sample_transactions_file, monkeypatch):
        work_dir = sample_transactions_file.parent
        monkeypatch.chdir(work_dir)

        run_pipeline(sample_transactions_file)

        for subdir in ("input", "processing", "output", "results"):
            assert (work_dir / "shared" / subdir).exists()

    def test_intermediate_files_written(self, sample_transactions_file, monkeypatch):
        work_dir = sample_transactions_file.parent
        monkeypatch.chdir(work_dir)

        run_pipeline(sample_transactions_file)

        assert len(list((work_dir / "shared" / "input").glob("*.json"))) == 4
        assert len(list((work_dir / "shared" / "processing").glob("*.json"))) == 4
        assert len(list((work_dir / "shared" / "output").glob("*.json"))) == 4


class TestPipelineAgentFlow:
    """Test the 3-agent flow with a single transaction."""

    def test_valid_transaction_flow(self, shared_dirs, valid_transaction):
        validator = TransactionValidator(shared_dirs)
        fraud = FraudDetector(shared_dirs)
        reporter = ReportingAgent(shared_dirs)

        msg = wrap_transaction(valid_transaction)
        validated = validator.process_message(msg)
        assert validated["data"]["status"] == "validated"

        scored = fraud.process_message(validated)
        assert "fraud_risk_score" in scored["data"]

        reported = reporter.process_message(scored)
        assert reported["data"]["final_status"] == "settled"

        result_file = shared_dirs / "results" / "TXN-TEST-001.json"
        assert result_file.exists()

    def test_rejected_transaction_flow(self, shared_dirs):
        validator = TransactionValidator(shared_dirs)
        fraud = FraudDetector(shared_dirs)
        reporter = ReportingAgent(shared_dirs)

        bad_txn = {
            "transaction_id": "TXN-BAD",
            "amount": "-50.00",
            "currency": "USD",
            "source_account": "ACC-0001",
            "destination_account": "ACC-0002",
            "timestamp": "2026-03-16T09:00:00Z",
            "metadata": {"country": "US"},
        }
        msg = wrap_transaction(bad_txn)
        validated = validator.process_message(msg)
        assert validated["data"]["status"] == "rejected"

        scored = fraud.process_message(validated)
        assert "fraud_risk_score" not in scored["data"]

        reported = reporter.process_message(scored)
        assert reported["data"]["final_status"] == "rejected"
