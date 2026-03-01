#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$SCRIPT_DIR/.."

echo "==> Running all tests with JaCoCo coverage report"
cd "$PROJECT_DIR"

# Run tests; skip the coverage enforcement check so report is always generated
mvn test jacoco:report -Djacoco.skip=false

REPORT="$PROJECT_DIR/target/site/jacoco/index.html"
if [ -f "$REPORT" ]; then
    echo ""
    echo "==> Coverage report generated at:"
    echo "    $REPORT"
    echo ""
    echo "To open in browser (macOS):"
    echo "    open \"$REPORT\""
else
    echo "==> Coverage report not found. Check for test failures above."
fi
