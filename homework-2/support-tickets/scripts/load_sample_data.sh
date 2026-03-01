#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$SCRIPT_DIR/.."
DATA_DIR="$PROJECT_DIR/src/test/resources/data"
API_URL="http://localhost:8080/tickets/import"

echo "==> Loading sample data into Support Tickets API at $API_URL"

if [ ! -d "$DATA_DIR" ]; then
    echo "Error: Data directory not found at $DATA_DIR"
    exit 1
fi

echo "Uploading sample_tickets.csv..."
curl -X POST "$API_URL" -F "file=@$DATA_DIR/sample_tickets.csv"
echo -e "\n"

echo "Uploading sample_tickets.json..."
curl -X POST "$API_URL" -F "file=@$DATA_DIR/sample_tickets.json"
echo -e "\n"

echo "Uploading sample_tickets.xml..."
curl -X POST "$API_URL" -F "file=@$DATA_DIR/sample_tickets.xml"
echo -e "\n"

echo "==> Sample data loading complete."
