#!/usr/bin/env bash
# run_tests.sh — run all tests for the Training App
#
# JVM unit tests (no device needed):
#   ./run_tests.sh
#
# JVM + instrumented tests (emulator/device must be running):
#   ./run_tests.sh --all

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

echo "========================================"
echo " Training App — Test Runner"
echo "========================================"

if [[ "$1" == "--all" ]]; then
    echo ""
    echo ">>> Running JVM unit tests..."
    ./gradlew :app:test --tests "com.trainingapp.*"

    echo ""
    echo ">>> Running instrumented UI tests (device/emulator required)..."
    ./gradlew :app:connectedAndroidTest

    echo ""
    echo "========================================"
    echo " All tests complete."
    echo " JVM report:          app/build/reports/tests/testDebugUnitTest/index.html"
    echo " Instrumented report: app/build/reports/androidTests/connected/index.html"
    echo "========================================"
else
    echo ""
    echo ">>> Running JVM unit tests..."
    ./gradlew :app:test --tests "com.trainingapp.*"

    echo ""
    echo "========================================"
    echo " JVM tests complete."
    echo " Report: app/build/reports/tests/testDebugUnitTest/index.html"
    echo ""
    echo " To also run instrumented UI tests (needs emulator/device):"
    echo "   ./run_tests.sh --all"
    echo "========================================"
fi
