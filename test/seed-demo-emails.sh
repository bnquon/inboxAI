#!/usr/bin/env bash
# Seed 3 demo emails into the backend (Redis + Kafka pipeline).
# Run from project root. Backend must be up at API_BASE.
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
API_BASE="${API_BASE:-http://localhost:8080}"
JSON="${1:-$SCRIPT_DIR/demo-emails.json}"

echo "Seeding demo emails from $JSON to $API_BASE/api/gmail/seed"
curl -s -X POST "$API_BASE/api/gmail/seed" \
  -H "Content-Type: application/json" \
  -d @"$JSON"
echo ""
