import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('errors');

// Test configuration
export const options = {
  stages: [
    { duration: '2m', target: 100 }, // Ramp up to 100 users
    { duration: '5m', target: 100 }, // Stay at 100 users
    { duration: '2m', target: 200 }, // Ramp up to 200 users
    { duration: '5m', target: 200 }, // Stay at 200 users
    { duration: '2m', target: 0 },   // Ramp down to 0 users
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'], // 95% of requests must complete below 500ms
    http_req_failed: ['rate<0.1'],    // Error rate must be below 10%
    errors: ['rate<0.1'],             // Custom error rate must be below 10%
  },
};

const BASE_URL = 'http://localhost:8080';

// Test data
const testUsers = [
  { email: 'user1@test.com', password: 'password123' },
  { email: 'user2@test.com', password: 'password123' },
  { email: 'user3@test.com', password: 'password123' },
  { email: 'user4@test.com', password: 'password123' },
  { email: 'user5@test.com', password: 'password123' },
];

let authTokens = {};

export function setup() {
  console.log('Setting up test data...');
  
  // Register test users
  for (let i = 0; i < testUsers.length; i++) {
    const user = testUsers[i];
    const registerPayload = JSON.stringify({
      email: user.email,
      password: user.password,
      name: `Test User ${i + 1}`,
      phoneNumber: `+821012345678${i}`
    });
    
    const registerResponse = http.post(`${BASE_URL}/auth/register`, registerPayload, {
      headers: { 'Content-Type': 'application/json' },
    });
    
    if (registerResponse.status === 200) {
      const registerData = JSON.parse(registerResponse.body);
      authTokens[user.email] = registerData.data.accessToken;
      console.log(`Registered user: ${user.email}`);
    } else {
      console.log(`Failed to register user: ${user.email}, Status: ${registerResponse.status}`);
    }
  }
  
  return { authTokens };
}

export default function(data) {
  const user = testUsers[Math.floor(Math.random() * testUsers.length)];
  const token = data.authTokens[user.email];
  
  if (!token) {
    console.log(`No token found for user: ${user.email}`);
    return;
  }
  
  const headers = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`
  };
  
  // Test 1: Get current user info
  const userResponse = http.get(`${BASE_URL}/auth/me`, { headers });
  const userCheck = check(userResponse, {
    'get user info status is 200': (r) => r.status === 200,
    'get user info response time < 500ms': (r) => r.timings.duration < 500,
  });
  errorRate.add(!userCheck);
  
  sleep(1);
  
  // Test 2: Get accounts
  const accountsResponse = http.get(`${BASE_URL}/accounts`, { headers });
  const accountsCheck = check(accountsResponse, {
    'get accounts status is 200': (r) => r.status === 200,
    'get accounts response time < 500ms': (r) => r.timings.duration < 500,
  });
  errorRate.add(!accountsCheck);
  
  sleep(1);
  
  // Test 3: Get transactions
  const transactionsResponse = http.get(`${BASE_URL}/transactions?page=0&size=10`, { headers });
  const transactionsCheck = check(transactionsResponse, {
    'get transactions status is 200': (r) => r.status === 200,
    'get transactions response time < 500ms': (r) => r.timings.duration < 500,
  });
  errorRate.add(!transactionsCheck);
  
  sleep(1);
}

export function teardown(data) {
  console.log('Cleaning up test data...');
  // Cleanup logic if needed
}
