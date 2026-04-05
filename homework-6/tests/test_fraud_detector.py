"""Tests for the Fraud Detector agent."""

import pytest
from agents.fraud_detector import FraudDetector


@pytest.fixture
def detector(shared_dirs):
    return FraudDetector(shared_dirs)


class TestSkipRejected:
    def test_skips_rejected_transaction(self, detector, rejected_message):
        result = detector.process_message(rejected_message)
        assert result["data"]["status"] == "rejected"
        assert "fraud_risk_score" not in result["data"]

    def test_skips_unknown_status(self, detector):
        msg = {
            "data": {
                "transaction_id": "TXN-UNK",
                "status": "error",
                "amount": "100.00",
            }
        }
        result = detector.process_message(msg)
        assert "fraud_risk_score" not in result["data"]


class TestLowRiskScoring:
    def test_small_domestic_daytime(self, detector, validated_message):
        result = detector.process_message(validated_message)
        assert result["data"]["fraud_risk_score"] == 0
        assert result["data"]["fraud_risk_level"] == "LOW"
        assert result["data"]["fraud_risk_factors"] == []

    def test_boundary_10000_not_triggered(self, detector, validated_message):
        validated_message["data"]["amount"] = "10000.00"
        result = detector.process_message(validated_message)
        assert result["data"]["fraud_risk_score"] == 0
        assert result["data"]["fraud_risk_level"] == "LOW"


class TestMediumRiskScoring:
    def test_above_10000(self, detector, validated_message):
        validated_message["data"]["amount"] = "10000.01"
        result = detector.process_message(validated_message)
        assert result["data"]["fraud_risk_score"] == 3
        assert result["data"]["fraud_risk_level"] == "MEDIUM"
        assert any("amount_above_10000" in f for f in result["data"]["fraud_risk_factors"])

    def test_25000_medium_risk(self, detector, validated_message):
        validated_message["data"]["amount"] = "25000.00"
        result = detector.process_message(validated_message)
        assert result["data"]["fraud_risk_score"] == 3
        assert result["data"]["fraud_risk_level"] == "MEDIUM"

    def test_unusual_hour_2am(self, detector, validated_message):
        validated_message["data"]["timestamp"] = "2026-03-16T02:30:00Z"
        result = detector.process_message(validated_message)
        assert result["data"]["fraud_risk_score"] == 2
        assert result["data"]["fraud_risk_level"] == "LOW"

    def test_unusual_hour_4am(self, detector, validated_message):
        validated_message["data"]["timestamp"] = "2026-03-16T04:59:00Z"
        result = detector.process_message(validated_message)
        assert result["data"]["fraud_risk_score"] == 2

    def test_cross_border(self, detector, validated_message):
        validated_message["data"]["metadata"] = {"country": "DE"}
        result = detector.process_message(validated_message)
        assert result["data"]["fraud_risk_score"] == 1
        assert any("cross_border" in f for f in result["data"]["fraud_risk_factors"])

    def test_unusual_hour_plus_cross_border(self, detector, validated_message):
        validated_message["data"]["timestamp"] = "2026-03-16T02:47:00Z"
        validated_message["data"]["metadata"] = {"country": "DE"}
        result = detector.process_message(validated_message)
        assert result["data"]["fraud_risk_score"] == 3
        assert result["data"]["fraud_risk_level"] == "MEDIUM"

    def test_boundary_50000_not_very_high(self, detector, validated_message):
        validated_message["data"]["amount"] = "50000.00"
        result = detector.process_message(validated_message)
        assert result["data"]["fraud_risk_score"] == 3
        assert result["data"]["fraud_risk_level"] == "MEDIUM"


class TestHighRiskScoring:
    def test_above_50000(self, detector, high_value_message):
        result = detector.process_message(high_value_message)
        assert result["data"]["fraud_risk_score"] == 7
        assert result["data"]["fraud_risk_level"] == "HIGH"
        assert any("amount_above_50000" in f for f in result["data"]["fraud_risk_factors"])

    def test_above_50000_at_3am_cross_border(self, detector, validated_message):
        validated_message["data"]["amount"] = "75000.00"
        validated_message["data"]["timestamp"] = "2026-03-16T03:00:00Z"
        validated_message["data"]["metadata"] = {"country": "GB"}
        result = detector.process_message(validated_message)
        # 7 (>50k) + 2 (unusual hour) + 1 (cross-border) = 10
        assert result["data"]["fraud_risk_score"] == 10
        assert result["data"]["fraud_risk_level"] == "HIGH"

    def test_score_capped_at_10(self, detector, validated_message):
        validated_message["data"]["amount"] = "100000.00"
        validated_message["data"]["timestamp"] = "2026-03-16T02:00:00Z"
        validated_message["data"]["metadata"] = {"country": "JP"}
        result = detector.process_message(validated_message)
        assert result["data"]["fraud_risk_score"] <= 10


class TestEdgeCases:
    def test_missing_timestamp(self, detector, validated_message):
        del validated_message["data"]["timestamp"]
        result = detector.process_message(validated_message)
        assert result["data"]["fraud_risk_score"] >= 0

    def test_missing_metadata(self, detector, validated_message):
        del validated_message["data"]["metadata"]
        result = detector.process_message(validated_message)
        assert result["data"]["fraud_risk_level"] in ("LOW", "MEDIUM", "HIGH")

    def test_invalid_amount_defaults_zero(self, detector, validated_message):
        validated_message["data"]["amount"] = "not-a-number"
        result = detector.process_message(validated_message)
        assert result["data"]["fraud_risk_score"] == 0

    def test_hour_5am_not_unusual(self, detector, validated_message):
        validated_message["data"]["timestamp"] = "2026-03-16T05:00:00Z"
        result = detector.process_message(validated_message)
        assert not any("unusual_hour" in f for f in result["data"]["fraud_risk_factors"])

    def test_hour_1am_not_unusual(self, detector, validated_message):
        validated_message["data"]["timestamp"] = "2026-03-16T01:59:00Z"
        result = detector.process_message(validated_message)
        assert not any("unusual_hour" in f for f in result["data"]["fraud_risk_factors"])


class TestMessageEnvelope:
    def test_source_agent_is_fraud_detector(self, detector, validated_message):
        result = detector.process_message(validated_message)
        assert result["source_agent"] == "fraud_detector"

    def test_target_is_reporting_agent(self, detector, validated_message):
        result = detector.process_message(validated_message)
        assert result["target_agent"] == "reporting_agent"
