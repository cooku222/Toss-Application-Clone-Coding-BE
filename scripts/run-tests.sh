#!/bin/bash

# Toss Microservices Test Runner Script

set -e

echo "🧪 Running Toss Microservices Tests"
echo "==================================="

# Check if services are running
if ! curl -s http://localhost:8080/actuator/health > /dev/null; then
    echo "❌ Services are not running. Please start the development environment first."
    echo "Run: ./scripts/start-dev.sh"
    exit 1
fi

# Create test results directory
mkdir -p test-results

echo "🔐 Running Security Tests..."
if [ -f "tests/security/security-test.sh" ]; then
    chmod +x tests/security/security-test.sh
    ./tests/security/security-test.sh
    echo "✅ Security tests completed"
else
    echo "⚠️ Security test script not found"
fi

echo ""
echo "⚡ Running Load Tests..."

# Check if k6 is installed
if ! command -v k6 &> /dev/null; then
    echo "❌ k6 is not installed. Please install k6 to run load tests."
    echo "Installation guide: https://k6.io/docs/getting-started/installation/"
    exit 1
fi

echo "Running authentication load test..."
k6 run tests/load/auth-load-test.js --out json=test-results/auth-load-test-results.json

echo "Running transaction load test..."
k6 run tests/load/transaction-load-test.js --out json=test-results/transaction-load-test-results.json

echo ""
echo "📊 Test Results Summary:"
echo "========================"

if [ -f "test-results/auth-load-test-results.json" ]; then
    echo "✅ Authentication load test completed"
fi

if [ -f "test-results/transaction-load-test-results.json" ]; then
    echo "✅ Transaction load test completed"
fi

if [ -f "security-reports/security-test-results-*.log" ]; then
    echo "✅ Security tests completed"
fi

echo ""
echo "📁 Test results saved to:"
echo "- Load test results: test-results/"
echo "- Security test results: security-reports/"
echo ""
echo "🎉 All tests completed!"
