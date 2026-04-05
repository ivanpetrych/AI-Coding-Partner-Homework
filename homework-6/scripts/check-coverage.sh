#!/bin/bash
# Coverage gate hook — blocks push if test coverage is below 80%
# Location: homework-6/scripts/check-coverage.sh
# Used by: git pre-push hook, VS Code Copilot hook

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

cd "$PROJECT_DIR"

echo "🔍 Running coverage gate check..."
echo "   Project: $PROJECT_DIR"
echo ""

# Detect Python: prefer venv, fallback to system python3
if [ -f "$PROJECT_DIR/../.venv/bin/python" ]; then
    PYTHON="$PROJECT_DIR/../.venv/bin/python"
elif [ -f "$PROJECT_DIR/.venv/bin/python" ]; then
    PYTHON="$PROJECT_DIR/.venv/bin/python"
else
    PYTHON="python3"
fi

# Run tests with coverage, fail if below 80%
PYTHONPATH=. "$PYTHON" -m pytest tests/ \
    --cov=agents \
    --cov=integrator \
    --cov-report=term-missing \
    --cov-fail-under=80 \
    -q

EXIT_CODE=$?

if [ $EXIT_CODE -eq 0 ]; then
    echo ""
    echo "✅ Coverage gate PASSED (≥ 80%)"
    echo ""
else
    echo ""
    echo "❌ Coverage gate FAILED — coverage is below 80%"
    echo "   Push blocked. Increase test coverage before pushing."
    echo ""
    exit 1
fi
