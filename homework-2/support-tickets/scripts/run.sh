#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$SCRIPT_DIR/.."

echo "==> Building and starting Support Tickets API on http://localhost:8080"
cd "$PROJECT_DIR"
mvn spring-boot:run
