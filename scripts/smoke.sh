#!/bin/bash
set -euo pipefail

# Smoke test script: posts suites, runs them, and compares results
BASE_URL="http://localhost:8080"

echo "Posting suites..."
for suite_file in suites/*.yaml; do
  echo "Posting $suite_file"
  curl -s -X POST "${BASE_URL}/suites" \
    -H 'Content-Type: text/yaml' \
    --data-binary "@${suite_file}" \
    > /dev/null || echo "Warning: Failed to post $(basename "$suite_file")"
done

echo "Running suites with repeat=3 concurrency=32..."
declare -A run_ids

for suite_file in suites/*.yaml; do
  suite_name=$(basename "$suite_file" .yaml)
  echo "Running $suite_name..."

  # Run first time
  run_id_1=$(curl -s -X POST "${BASE_URL}/runs" \
    -H 'Content-Type: application/json' \
    -d "{\"suite_id\":\"${suite_name}\",\"repeat\":3,\"concurrency\":32}" \
    | python3 -c "import sys, json; print(json.load(sys.stdin).get('id', 'unknown'))" || echo "unknown")

  echo "  Run 1 ID: $run_id_1"
  run_ids["${suite_name}_1"]="$run_id_1"

  # Run second time (for diff comparison)
  run_id_2=$(curl -s -X POST "${BASE_URL}/runs" \
    -H 'Content-Type: application/json' \
    -d "{\"suite_id\":\"${suite_name}\",\"repeat\":3,\"concurrency\":32}" \
    | python3 -c "import sys, json; print(json.load(sys.stdin).get('id', 'unknown'))" || echo "unknown")

  echo "  Run 2 ID: $run_id_2"
  run_ids["${suite_name}_2"]="$run_id_2"
done

echo ""
echo "Getting diff between first two booking-agent runs..."
if [[ -n "${run_ids[booking-agent_1]:-}" ]] && [[ -n "${run_ids[booking-agent_2]:-}" ]]; then
  curl -s -X GET "${BASE_URL}/runs/${run_ids[booking-agent_1]}/diff/${run_ids[booking-agent_2]}" | python3 -m json.tool || echo "Warning: Could not get diff"
else
  echo "Warning: booking-agent run IDs not available"
fi

echo ""
echo "Smoke test complete"
