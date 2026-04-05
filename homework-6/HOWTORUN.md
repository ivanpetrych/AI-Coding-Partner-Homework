# How to Run — Multi-Agent Banking Pipeline

## Prerequisites

- Python 3.11 or higher
- pip (Python package manager)
- Node.js / npx (for context7 MCP server only)

## 1. Install Dependencies

```bash
cd homework-6
pip install -r requirements.txt
```

This installs: `fastmcp`, `pytest`, `pytest-cov`.

## 2. Run the Pipeline

```bash
python3 integrator.py
```

This will:
1. Create/clear `shared/` directories (input, processing, output, results)
2. Load 8 transactions from `sample-transactions.json`
3. Run each through: Validator → Fraud Detector → Reporting Agent
4. Write result files to `shared/results/`
5. Print a summary table to the terminal

Expected output: 8 transactions processed — 5 settled, 1 flagged, 2 rejected.

## 3. Run Validation Only (Dry Run)

```bash
PYTHONPATH=. python3 agents/transaction_validator.py --dry-run
```

Validates all transactions without running the full pipeline. Shows a table of results.

## 4. Run Tests

```bash
PYTHONPATH=. python3 -m pytest tests/ -v
```

### With Coverage Report

```bash
PYTHONPATH=. python3 -m pytest tests/ --cov=agents --cov=integrator --cov-report=term-missing
```

Coverage target: ≥ 80% (gate), currently at 94%.

## 5. Coverage Gate (Pre-Push Hook)

The git pre-push hook runs tests and blocks push if coverage drops below 80%.

To install the hook manually:
```bash
cp hooks/pre-push ../../.git/hooks/pre-push
chmod +x ../../.git/hooks/pre-push
```

To test the hook script directly:
```bash
bash scripts/check-coverage.sh
```

## 6. MCP Server

### Start the Custom Pipeline-Status Server

```bash
python3 mcp/server.py
```

This starts a FastMCP server (stdio transport) exposing:
- `get_transaction_status(transaction_id)` — query a transaction's status
- `list_pipeline_results()` — summary of all processed transactions
- `pipeline://summary` — human-readable pipeline summary resource

### MCP Configuration

Both MCP servers are configured in `mcp.json`:
- **context7** — library documentation lookup (via npx)
- **pipeline-status** — custom pipeline query server

## 7. Copilot Slash Commands

Available in VS Code Copilot Chat:
- `/run-pipeline` — run the full pipeline end-to-end
- `/validate-transactions` — dry-run validation of transactions
- `/write-spec` — generate a project specification from the template

## File Outputs

After running the pipeline:

```
shared/results/
├── TXN001.json              # Individual transaction results
├── TXN002.json
├── ...
├── TXN008.json
└── pipeline_summary.json    # Aggregate statistics
```
