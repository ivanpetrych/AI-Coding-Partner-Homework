#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$SCRIPT_DIR/.."

echo "==> Running End-to-End Tests ONLY"
cd "$PROJECT_DIR"

mvn test -Dtest=TicketEndToEndTest
