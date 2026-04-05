# Research Notes — context7 Queries

Documented context7 queries made during pipeline development (Task 2 / Agent 2).

---

## Query 1: Python decimal module — monetary arithmetic

- **Search**: "Python decimal module"
- **context7 library ID**: `/python/cpython` (Python standard library docs)
- **Key insight**: The `decimal` module provides `Decimal` type for exact decimal arithmetic, avoiding the floating-point representation errors inherent in `float`. For banking/monetary calculations:
  - Always construct `Decimal` from strings, never from floats: `Decimal("10.50")` not `Decimal(10.50)`
  - Use comparison operators directly: `Decimal("75000") > Decimal("50000")`
  - The `InvalidOperation` exception is raised when constructing from invalid strings
- **Applied**: All monetary comparisons in `agents/transaction_validator.py` and `agents/fraud_detector.py` use `Decimal` constructed from string values. Threshold constants are defined as `Decimal("10000")` and `Decimal("50000")`. Amount parsing wraps in try/except `InvalidOperation` for validation.

```python
# Pattern applied from context7 research
from decimal import Decimal, InvalidOperation

THRESHOLD_HIGH_VALUE = Decimal("10000")

try:
    amount = Decimal(str(data["amount"]))
except (InvalidOperation, ValueError):
    # reject transaction with INVALID_AMOUNT
    ...
```

---

## Query 2: FastMCP — building MCP servers in Python

- **Search**: "FastMCP Python MCP server tools resources"
- **context7 library ID**: `/jlowin/fastmcp`
- **Key insight**: FastMCP v2+ provides a simple decorator-based API for creating MCP servers:
  - `@mcp.tool()` decorator registers a function as an MCP tool (callable by AI agents)
  - `@mcp.resource("uri://path")` decorator exposes a read-only resource
  - Tools should return strings (JSON-serialized for structured data)
  - `mcp.run()` starts the server on stdio transport (standard for VS Code integration)
  - The `FastMCP("name")` constructor takes a server name shown to clients
- **Applied**: Built `mcp/server.py` with two tools (`get_transaction_status`, `list_pipeline_results`) and one resource (`pipeline://summary`). Tools return JSON strings with `json.dumps()`, resource returns plain text. Server uses stdio transport via `mcp.run()`.

```python
# Pattern applied from context7 research
from fastmcp import FastMCP

mcp = FastMCP("pipeline-status")

@mcp.tool()
def get_transaction_status(transaction_id: str) -> str:
    """Get status of a transaction."""
    ...
    return json.dumps(result, indent=2)

@mcp.resource("pipeline://summary")
def pipeline_summary() -> str:
    """Human-readable pipeline summary."""
    ...
    return "\n".join(lines)
```

---

## Query 3: Python pathlib — file system operations (bonus)

- **Search**: "Python pathlib Path glob mkdir"
- **context7 library ID**: `/python/cpython`
- **Key insight**: `pathlib.Path` provides an object-oriented interface for file operations:
  - `Path.glob("TXN*.json")` for pattern matching in directories
  - `Path.mkdir(parents=True, exist_ok=True)` for safe directory creation
  - `Path.resolve()` for absolute paths relative to script location
  - `Path(__file__).resolve().parent` to get the script's directory
- **Applied**: Used throughout `agents/base_agent.py` for `write_json`/`read_json` methods, in `integrator.py` for shared directory management, and in `mcp/server.py` for locating result files relative to the server script.
