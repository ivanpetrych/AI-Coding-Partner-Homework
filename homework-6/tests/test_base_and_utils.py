"""Tests for base_agent utilities and transaction_validator dry-run."""

import json
import pytest
from pathlib import Path

from agents.base_agent import mask_account, mask_accounts_in_data, BaseAgent
from agents.transaction_validator import dry_run


class TestMaskAccount:
    def test_normal_account(self):
        assert mask_account("ACC-1001") == "***1001"

    def test_short_account(self):
        assert mask_account("AB") == "***AB"

    def test_four_char_account(self):
        assert mask_account("ABCD") == "***ABCD"

    def test_long_account(self):
        assert mask_account("ACC-123456789") == "***6789"


class TestMaskAccountsInData:
    def test_masks_both_accounts(self):
        data = {
            "source_account": "ACC-1001",
            "destination_account": "ACC-2001",
            "amount": "100.00",
        }
        masked = mask_accounts_in_data(data)
        assert masked["source_account"] == "***1001"
        assert masked["destination_account"] == "***2001"
        assert masked["amount"] == "100.00"
        # Original not mutated
        assert data["source_account"] == "ACC-1001"

    def test_no_accounts(self):
        data = {"amount": "100.00"}
        masked = mask_accounts_in_data(data)
        assert masked == {"amount": "100.00"}


class TestBaseAgentWriteRead:
    def test_write_and_read_json(self, shared_dirs):
        class TestAgent(BaseAgent):
            def process_message(self, message):
                return message

        agent = TestAgent("test_agent", shared_dirs)
        data = {"key": "value", "num": 42}
        filepath = agent.write_json(shared_dirs / "output", "test.json", data)
        assert filepath.exists()

        loaded = agent.read_json(filepath)
        assert loaded == data

    def test_write_creates_directory(self, tmp_path):
        class TestAgent(BaseAgent):
            def process_message(self, message):
                return message

        agent = TestAgent("test_agent", tmp_path)
        new_dir = tmp_path / "new" / "nested"
        agent.write_json(new_dir, "test.json", {"a": 1})
        assert (new_dir / "test.json").exists()

    def test_create_message(self, shared_dirs):
        class TestAgent(BaseAgent):
            def process_message(self, message):
                return message

        agent = TestAgent("my_agent", shared_dirs)
        msg = agent.create_message({"foo": "bar"}, "next_agent")
        assert msg["source_agent"] == "my_agent"
        assert msg["target_agent"] == "next_agent"
        assert msg["data"] == {"foo": "bar"}
        assert "message_id" in msg
        assert "timestamp" in msg

    def test_log_transaction(self, shared_dirs, caplog):
        import logging

        class TestAgent(BaseAgent):
            def process_message(self, message):
                return message

        agent = TestAgent("test_agent", shared_dirs)
        with caplog.at_level(logging.INFO, logger="test_agent"):
            agent.log_transaction("TXN001", "test outcome")
        assert "TXN001" in caplog.text
        assert "test outcome" in caplog.text


class TestDryRun:
    def test_dry_run_output(self, capsys, sample_transactions_file):
        dry_run(sample_transactions_file)
        captured = capsys.readouterr()
        assert "Transaction Validation Report" in captured.out
        assert "Total: 4" in captured.out
        assert "Valid: 2" in captured.out
        assert "Invalid: 2" in captured.out
        assert "INVALID_CURRENCY" in captured.out
        assert "NEGATIVE_AMOUNT" in captured.out
