#!/bin/bash
# Refresh openapi.json.
#
# Generated from the controller signatures by OpenApiExportTest, so it cannot drift from the code
# the way a hand-written spec does. Needs Docker, because the test starts the application context
# against Testcontainers.
#
#   ./extract-openapi-json.sh
#
# Pass --check to verify the committed copy is current without overwriting it. This is what CI runs:
# it fails when someone changes a request or response type and does not regenerate.
#
#   ./extract-openapi-json.sh --check
set -euo pipefail

mvn -B -q test -Dtest=OpenApiExportTest

if [[ "${1:-}" == "--check" ]]; then
    if diff -u openapi.json target/openapi.json; then
        echo "openapi.json is up to date"
        exit 0
    fi
    echo
    echo "openapi.json is stale — run ./extract-openapi-json.sh and commit the result" >&2
    exit 1
fi

cp target/openapi.json openapi.json
echo "openapi.json generated from the controller signatures"
