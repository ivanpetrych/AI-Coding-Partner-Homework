"""Tests for the Transaction Validator agent."""

import pytest
from agents.transaction_validator import TransactionValidator, VALID_CURRENCIES


@pytest.fixture
def validator(shared_dirs):
    return TransactionValidator(shared_dirs)


class TestValidTransaction:
    def test_valid_usd_transaction(self, validator, valid_message):
        result = validator.process_message(valid_message)
        assert result["data"]["status"] == "validated"
        assert result["data"]["validated_amount"] == "1500.00"
        assert result["target_agent"] == "fraud_detector"

    def test_valid_eur_transaction(self, validator, valid_message):
        valid_message["data"]["currency"] = "EUR"
        result = validator.process_message(valid_message)
        assert result["data"]["status"] == "validated"

    def test_valid_gbp_transaction(self, validator, valid_message):
        valid_message["data"]["currency"] = "GBP"
        result = validator.process_message(valid_message)
        assert result["data"]["status"] == "validated"

    def test_valid_large_amount(self, validator, valid_message):
        valid_message["data"]["amount"] = "999999.99"
        result = validator.process_message(valid_message)
        assert result["data"]["status"] == "validated"
        assert result["data"]["validated_amount"] == "999999.99"

    def test_valid_small_amount(self, validator, valid_message):
        valid_message["data"]["amount"] = "0.01"
        result = validator.process_message(valid_message)
        assert result["data"]["status"] == "validated"


class TestMissingFields:
    def test_missing_transaction_id(self, validator):
        msg = {
            "data": {
                "amount": "100.00",
                "currency": "USD",
                "source_account": "ACC-1001",
                "destination_account": "ACC-2001",
            }
        }
        result = validator.process_message(msg)
        assert result["data"]["status"] == "rejected"
        assert result["data"]["rejection_reason"] == "MISSING_FIELD:transaction_id"

    def test_missing_amount(self, validator, valid_message):
        del valid_message["data"]["amount"]
        result = validator.process_message(valid_message)
        assert result["data"]["status"] == "rejected"
        assert result["data"]["rejection_reason"] == "MISSING_FIELD:amount"

    def test_missing_currency(self, validator, valid_message):
        del valid_message["data"]["currency"]
        result = validator.process_message(valid_message)
        assert result["data"]["status"] == "rejected"
        assert result["data"]["rejection_reason"] == "MISSING_FIELD:currency"

    def test_missing_source_account(self, validator, valid_message):
        del valid_message["data"]["source_account"]
        result = validator.process_message(valid_message)
        assert result["data"]["status"] == "rejected"
        assert result["data"]["rejection_reason"] == "MISSING_FIELD:source_account"

    def test_missing_destination_account(self, validator, valid_message):
        del valid_message["data"]["destination_account"]
        result = validator.process_message(valid_message)
        assert result["data"]["status"] == "rejected"
        assert result["data"]["rejection_reason"] == "MISSING_FIELD:destination_account"

    def test_empty_string_field(self, validator, valid_message):
        valid_message["data"]["transaction_id"] = "   "
        result = validator.process_message(valid_message)
        assert result["data"]["status"] == "rejected"
        assert "MISSING_FIELD" in result["data"]["rejection_reason"]

    def test_empty_data(self, validator):
        result = validator.process_message({"data": {}})
        assert result["data"]["status"] == "rejected"


class TestAmountValidation:
    def test_negative_amount(self, validator, valid_message):
        valid_message["data"]["amount"] = "-100.00"
        result = validator.process_message(valid_message)
        assert result["data"]["status"] == "rejected"
        assert result["data"]["rejection_reason"] == "NEGATIVE_AMOUNT"

    def test_zero_amount(self, validator, valid_message):
        valid_message["data"]["amount"] = "0.00"
        result = validator.process_message(valid_message)
        assert result["data"]["status"] == "rejected"
        assert result["data"]["rejection_reason"] == "NEGATIVE_AMOUNT"

    def test_invalid_amount_string(self, validator, valid_message):
        valid_message["data"]["amount"] = "not-a-number"
        result = validator.process_message(valid_message)
        assert result["data"]["status"] == "rejected"
        assert result["data"]["rejection_reason"] == "INVALID_AMOUNT"


class TestCurrencyValidation:
    def test_invalid_currency_xyz(self, validator, valid_message):
        valid_message["data"]["currency"] = "XYZ"
        result = validator.process_message(valid_message)
        assert result["data"]["status"] == "rejected"
        assert result["data"]["rejection_reason"] == "INVALID_CURRENCY"

    def test_invalid_currency_empty(self, validator, valid_message):
        valid_message["data"]["currency"] = ""
        result = validator.process_message(valid_message)
        assert result["data"]["status"] == "rejected"

    def test_currency_case_insensitive(self, validator, valid_message):
        valid_message["data"]["currency"] = "usd"
        result = validator.process_message(valid_message)
        assert result["data"]["status"] == "validated"


class TestMessageEnvelope:
    def test_message_has_required_fields(self, validator, valid_message):
        result = validator.process_message(valid_message)
        assert "message_id" in result
        assert "timestamp" in result
        assert "source_agent" in result
        assert result["source_agent"] == "transaction_validator"
        assert "target_agent" in result
        assert "message_type" in result
        assert result["message_type"] == "transaction"

    def test_rejected_targets_reporting_agent(self, validator, valid_message):
        valid_message["data"]["currency"] = "XYZ"
        result = validator.process_message(valid_message)
        assert result["target_agent"] == "reporting_agent"

    def test_validated_targets_fraud_detector(self, validator, valid_message):
        result = validator.process_message(valid_message)
        assert result["target_agent"] == "fraud_detector"

    def test_no_data_key(self, validator):
        result = validator.process_message({})
        assert result["data"]["status"] == "rejected"
