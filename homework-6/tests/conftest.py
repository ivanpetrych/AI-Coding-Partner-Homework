"""Shared test fixtures for the banking pipeline test suite."""

import json
import pytest
from pathlib import Path


@pytest.fixture
def shared_dirs(tmp_path):
    """Create isolated shared/ directory structure for testing."""
    for subdir in ("input", "processing", "output", "results"):
        (tmp_path / subdir).mkdir()
    return tmp_path


@pytest.fixture
def valid_transaction():
    """A standard valid transaction."""
    return {
        "transaction_id": "TXN-TEST-001",
        "amount": "1500.00",
        "currency": "USD",
        "source_account": "ACC-1001",
        "destination_account": "ACC-2001",
        "timestamp": "2026-03-16T09:00:00Z",
        "transaction_type": "transfer",
        "description": "Test payment",
        "metadata": {"channel": "online", "country": "US"},
    }


@pytest.fixture
def valid_message(valid_transaction):
    """A valid transaction wrapped in the standard message envelope."""
    return {
        "message_id": "test-uuid-001",
        "timestamp": "2026-03-16T09:00:00Z",
        "source_agent": "test",
        "target_agent": "transaction_validator",
        "message_type": "transaction",
        "data": valid_transaction,
    }


@pytest.fixture
def rejected_message():
    """A message with rejected status (invalid currency)."""
    return {
        "message_id": "test-uuid-rej",
        "timestamp": "2026-03-16T10:05:00Z",
        "source_agent": "transaction_validator",
        "target_agent": "fraud_detector",
        "message_type": "transaction",
        "data": {
            "transaction_id": "TXN-TEST-REJ",
            "amount": "200.00",
            "currency": "XYZ",
            "source_account": "ACC-1006",
            "destination_account": "ACC-7700",
            "status": "rejected",
            "rejection_reason": "INVALID_CURRENCY",
            "timestamp": "2026-03-16T10:05:00Z",
            "metadata": {"channel": "online", "country": "US"},
        },
    }


@pytest.fixture
def validated_message():
    """A message that has passed validation."""
    return {
        "message_id": "test-uuid-val",
        "timestamp": "2026-03-16T09:00:00Z",
        "source_agent": "transaction_validator",
        "target_agent": "fraud_detector",
        "message_type": "transaction",
        "data": {
            "transaction_id": "TXN-TEST-VAL",
            "amount": "1500.00",
            "currency": "USD",
            "source_account": "ACC-1001",
            "destination_account": "ACC-2001",
            "status": "validated",
            "validated_amount": "1500.00",
            "timestamp": "2026-03-16T09:00:00Z",
            "metadata": {"channel": "online", "country": "US"},
        },
    }


@pytest.fixture
def high_value_message():
    """A validated message with amount > $50,000 (triggers HIGH risk)."""
    return {
        "message_id": "test-uuid-high",
        "timestamp": "2026-03-16T10:00:00Z",
        "source_agent": "transaction_validator",
        "target_agent": "fraud_detector",
        "message_type": "transaction",
        "data": {
            "transaction_id": "TXN-TEST-HIGH",
            "amount": "75000.00",
            "currency": "USD",
            "source_account": "ACC-1005",
            "destination_account": "ACC-6600",
            "status": "validated",
            "validated_amount": "75000.00",
            "timestamp": "2026-03-16T10:00:00Z",
            "metadata": {"channel": "branch", "country": "US"},
        },
    }


@pytest.fixture
def scored_settled_message():
    """A fully scored message that should settle (LOW risk)."""
    return {
        "message_id": "test-uuid-scored",
        "timestamp": "2026-03-16T09:00:00Z",
        "source_agent": "fraud_detector",
        "target_agent": "reporting_agent",
        "message_type": "transaction",
        "data": {
            "transaction_id": "TXN-TEST-SET",
            "amount": "1500.00",
            "currency": "USD",
            "source_account": "ACC-1001",
            "destination_account": "ACC-2001",
            "status": "validated",
            "fraud_risk_score": 0,
            "fraud_risk_level": "LOW",
            "fraud_risk_factors": [],
            "timestamp": "2026-03-16T09:00:00Z",
            "metadata": {"channel": "online", "country": "US"},
        },
    }


@pytest.fixture
def scored_flagged_message():
    """A fully scored message that should be flagged (HIGH risk)."""
    return {
        "message_id": "test-uuid-flagged",
        "timestamp": "2026-03-16T10:00:00Z",
        "source_agent": "fraud_detector",
        "target_agent": "reporting_agent",
        "message_type": "transaction",
        "data": {
            "transaction_id": "TXN-TEST-FLAG",
            "amount": "75000.00",
            "currency": "USD",
            "source_account": "ACC-1005",
            "destination_account": "ACC-6600",
            "status": "validated",
            "fraud_risk_score": 7,
            "fraud_risk_level": "HIGH",
            "fraud_risk_factors": ["amount_above_50000 (+7)"],
            "timestamp": "2026-03-16T10:00:00Z",
            "metadata": {"channel": "branch", "country": "US"},
        },
    }


@pytest.fixture
def sample_transactions_file(tmp_path):
    """Create a sample-transactions.json in tmp_path."""
    transactions = [
        {
            "transaction_id": "TXN001",
            "timestamp": "2026-03-16T09:00:00Z",
            "source_account": "ACC-1001",
            "destination_account": "ACC-2001",
            "amount": "1500.00",
            "currency": "USD",
            "transaction_type": "transfer",
            "description": "Monthly rent payment",
            "metadata": {"channel": "online", "country": "US"},
        },
        {
            "transaction_id": "TXN002",
            "timestamp": "2026-03-16T09:15:00Z",
            "source_account": "ACC-1002",
            "destination_account": "ACC-3001",
            "amount": "25000.00",
            "currency": "USD",
            "transaction_type": "wire_transfer",
            "description": "Equipment purchase",
            "metadata": {"channel": "branch", "country": "US"},
        },
        {
            "transaction_id": "TXN006",
            "timestamp": "2026-03-16T10:05:00Z",
            "source_account": "ACC-1006",
            "destination_account": "ACC-7700",
            "amount": "200.00",
            "currency": "XYZ",
            "transaction_type": "transfer",
            "description": "Test payment",
            "metadata": {"channel": "online", "country": "US"},
        },
        {
            "transaction_id": "TXN007",
            "timestamp": "2026-03-16T10:10:00Z",
            "source_account": "ACC-1007",
            "destination_account": "ACC-8800",
            "amount": "-100.00",
            "currency": "GBP",
            "transaction_type": "refund",
            "description": "Refund for order #8821",
            "metadata": {"channel": "online", "country": "GB"},
        },
    ]
    filepath = tmp_path / "sample-transactions.json"
    with open(filepath, "w") as f:
        json.dump(transactions, f)
    return filepath
