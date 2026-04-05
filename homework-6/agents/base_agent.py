import json
import logging
import re
import uuid
from abc import ABC, abstractmethod
from datetime import datetime, timezone
from pathlib import Path


def mask_account(account: str) -> str:
    """Mask account number, keeping only last 4 characters."""
    if len(account) <= 4:
        return f"***{account}"
    return f"***{account[-4:]}"


def mask_accounts_in_data(data: dict) -> dict:
    """Return a copy of data with account fields masked."""
    masked = dict(data)
    for key in ("source_account", "destination_account"):
        if key in masked:
            masked[key] = mask_account(masked[key])
    return masked


class BaseAgent(ABC):
    """Abstract base class for all pipeline agents."""

    def __init__(self, name: str, shared_dir: Path | None = None):
        self.name = name
        self.shared_dir = shared_dir or Path("shared")
        self.logger = logging.getLogger(self.name)

    @abstractmethod
    def process_message(self, message: dict) -> dict:
        """Process a single message and return the enriched message."""
        ...

    def create_message(
        self,
        data: dict,
        target_agent: str,
        source_message: dict | None = None,
    ) -> dict:
        """Wrap data in the standard message envelope."""
        return {
            "message_id": str(uuid.uuid4()),
            "timestamp": datetime.now(timezone.utc).isoformat(),
            "source_agent": self.name,
            "target_agent": target_agent,
            "message_type": "transaction",
            "data": data,
        }

    def log_transaction(self, transaction_id: str, outcome: str) -> None:
        """Log an agent operation with the standard format."""
        self.logger.info("[%s] %s", transaction_id, outcome)

    def write_json(self, directory: Path, filename: str, data: dict) -> Path:
        """Write a dict as JSON to a file in the given directory."""
        directory.mkdir(parents=True, exist_ok=True)
        filepath = directory / filename
        with open(filepath, "w") as f:
            json.dump(data, f, indent=2)
        return filepath

    def read_json(self, filepath: Path) -> dict:
        """Read a JSON file and return as dict."""
        with open(filepath) as f:
            return json.load(f)
