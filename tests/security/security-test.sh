#!/bin/bash

# Security Testing Script for Toss Microservices
# This script performs various security tests

set -e

BASE_URL="http://localhost:8080"
REPORT_DIR="security-reports"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")

# Create report directory
mkdir -p $REPORT_DIR

echo "🔒 Starting Security Tests for Toss Microservices"
echo "=================================================="

# Function to log test results
log_result() {
    local test_name="$1"
    local status="$2"
    local details="$3"
    echo "[$status] $test_name: $details"
    echo "[$status] $test_name: $details" >> "$REPORT_DIR/security-test-results-$TIMESTAMP.log"
}

# Test 1: Authentication Bypass Tests
echo "🔐 Testing Authentication Bypass..."
test_auth_bypass() {
    # Test accessing protected endpoints without token
    local endpoints=("/accounts" "/transactions" "/auth/me")
    
    for endpoint in "${endpoints[@]}"; do
        response=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL$endpoint")
        if [ "$response" = "401" ]; then
            log_result "Auth Bypass Test - $endpoint" "PASS" "Properly returns 401 Unauthorized"
        else
            log_result "Auth Bypass Test - $endpoint" "FAIL" "Returns $response instead of 401"
        fi
    done
}

# Test 2: SQL Injection Tests
echo "💉 Testing SQL Injection..."
test_sql_injection() {
    local malicious_inputs=(
        "'; DROP TABLE users; --"
        "1' OR '1'='1"
        "admin'--"
        "1' UNION SELECT * FROM users--"
    )
    
    for input in "${malicious_inputs[@]}"; do
        # Test login endpoint
        response=$(curl -s -X POST "$BASE_URL/auth/login" \
            -H "Content-Type: application/json" \
            -d "{\"email\":\"$input\",\"password\":\"test\"}" \
            -w "%{http_code}")
        
        if [[ "$response" == *"400"* ]] || [[ "$response" == *"401"* ]]; then
            log_result "SQL Injection Test - Login" "PASS" "Properly rejects malicious input: $input"
        else
            log_result "SQL Injection Test - Login" "FAIL" "Accepts malicious input: $input"
        fi
    done
}

# Test 3: XSS Tests
echo "🌐 Testing Cross-Site Scripting (XSS)..."
test_xss() {
    local xss_payloads=(
        "<script>alert('XSS')</script>"
        "javascript:alert('XSS')"
        "<img src=x onerror=alert('XSS')>"
        "';alert('XSS');//"
    )
    
    for payload in "${xss_payloads[@]}"; do
        # Test registration endpoint
        response=$(curl -s -X POST "$BASE_URL/auth/register" \
            -H "Content-Type: application/json" \
            -d "{\"email\":\"test@test.com\",\"password\":\"password123\",\"name\":\"$payload\",\"phoneNumber\":\"+8210123456789\"}" \
            -w "%{http_code}")
        
        if [[ "$response" == *"400"* ]]; then
            log_result "XSS Test - Registration" "PASS" "Properly rejects XSS payload: $payload"
        else
            log_result "XSS Test - Registration" "FAIL" "Accepts XSS payload: $payload"
        fi
    done
}

# Test 4: Rate Limiting Tests
echo "⏱️ Testing Rate Limiting..."
test_rate_limiting() {
    local endpoint="/auth/login"
    local success_count=0
    local total_requests=20
    
    for i in $(seq 1 $total_requests); do
        response=$(curl -s -X POST "$BASE_URL$endpoint" \
            -H "Content-Type: application/json" \
            -d "{\"email\":\"test@test.com\",\"password\":\"wrongpassword\"}" \
            -w "%{http_code}")
        
        if [[ "$response" == *"429"* ]]; then
            success_count=$((success_count + 1))
        fi
        sleep 0.1
    done
    
    if [ $success_count -gt 0 ]; then
        log_result "Rate Limiting Test" "PASS" "Rate limiting triggered after $success_count requests"
    else
        log_result "Rate Limiting Test" "FAIL" "No rate limiting detected after $total_requests requests"
    fi
}

# Test 5: JWT Token Security Tests
echo "🎫 Testing JWT Token Security..."
test_jwt_security() {
    # Test with invalid token
    response=$(curl -s -o /dev/null -w "%{http_code}" \
        -H "Authorization: Bearer invalid_token" \
        "$BASE_URL/auth/me")
    
    if [ "$response" = "401" ]; then
        log_result "JWT Security Test - Invalid Token" "PASS" "Properly rejects invalid token"
    else
        log_result "JWT Security Test - Invalid Token" "FAIL" "Accepts invalid token"
    fi
    
    # Test with expired token (if we had one)
    # This would require generating an expired token for testing
    log_result "JWT Security Test - Expired Token" "SKIP" "Requires expired token generation"
}

# Test 6: Input Validation Tests
echo "✅ Testing Input Validation..."
test_input_validation() {
    # Test email validation
    local invalid_emails=("invalid-email" "test@" "@test.com" "test..test@test.com")
    
    for email in "${invalid_emails[@]}"; do
        response=$(curl -s -X POST "$BASE_URL/auth/register" \
            -H "Content-Type: application/json" \
            -d "{\"email\":\"$email\",\"password\":\"password123\",\"name\":\"Test User\",\"phoneNumber\":\"+8210123456789\"}" \
            -w "%{http_code}")
        
        if [[ "$response" == *"400"* ]]; then
            log_result "Input Validation Test - Email" "PASS" "Properly rejects invalid email: $email"
        else
            log_result "Input Validation Test - Email" "FAIL" "Accepts invalid email: $email"
        fi
    done
    
    # Test password validation
    local weak_passwords=("123" "password" "12345678")
    
    for password in "${weak_passwords[@]}"; do
        response=$(curl -s -X POST "$BASE_URL/auth/register" \
            -H "Content-Type: application/json" \
            -d "{\"email\":\"test@test.com\",\"password\":\"$password\",\"name\":\"Test User\",\"phoneNumber\":\"+8210123456789\"}" \
            -w "%{http_code}")
        
        if [[ "$response" == *"400"* ]]; then
            log_result "Input Validation Test - Password" "PASS" "Properly rejects weak password"
        else
            log_result "Input Validation Test - Password" "FAIL" "Accepts weak password"
        fi
    done
}

# Test 7: CORS Tests
echo "🌍 Testing CORS Configuration..."
test_cors() {
    # Test preflight request
    response=$(curl -s -X OPTIONS "$BASE_URL/accounts" \
        -H "Origin: https://malicious-site.com" \
        -H "Access-Control-Request-Method: GET" \
        -H "Access-Control-Request-Headers: Authorization" \
        -w "%{http_code}")
    
    if [[ "$response" == *"200"* ]]; then
        log_result "CORS Test - Preflight" "PASS" "CORS preflight request handled"
    else
        log_result "CORS Test - Preflight" "FAIL" "CORS preflight request failed"
    fi
}

# Test 8: Information Disclosure Tests
echo "🔍 Testing Information Disclosure..."
test_info_disclosure() {
    # Test error messages
    response=$(curl -s -X POST "$BASE_URL/auth/login" \
        -H "Content-Type: application/json" \
        -d "{\"email\":\"nonexistent@test.com\",\"password\":\"wrongpassword\"}")
    
    if [[ "$response" == *"Invalid credentials"* ]]; then
        log_result "Info Disclosure Test - Error Messages" "PASS" "Generic error message returned"
    else
        log_result "Info Disclosure Test - Error Messages" "FAIL" "Detailed error information exposed"
    fi
}

# Run all tests
test_auth_bypass
test_sql_injection
test_xss
test_rate_limiting
test_jwt_security
test_input_validation
test_cors
test_info_disclosure

echo ""
echo "🔒 Security Tests Completed!"
echo "============================"
echo "📊 Results saved to: $REPORT_DIR/security-test-results-$TIMESTAMP.log"
echo ""

# Generate summary
echo "📋 Security Test Summary:"
echo "========================"
pass_count=$(grep -c "PASS" "$REPORT_DIR/security-test-results-$TIMESTAMP.log" || echo "0")
fail_count=$(grep -c "FAIL" "$REPORT_DIR/security-test-results-$TIMESTAMP.log" || echo "0")
skip_count=$(grep -c "SKIP" "$REPORT_DIR/security-test-results-$TIMESTAMP.log" || echo "0")

echo "✅ Passed: $pass_count"
echo "❌ Failed: $fail_count"
echo "⏭️ Skipped: $skip_count"
echo ""

if [ $fail_count -eq 0 ]; then
    echo "🎉 All security tests passed!"
    exit 0
else
    echo "⚠️ Some security tests failed. Please review the results."
    exit 1
fi
