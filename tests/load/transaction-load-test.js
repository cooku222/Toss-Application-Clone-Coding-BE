import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('errors');
const transactionRate = new Rate('transaction_success');

// Test configuration
export const options = {
  stages: [
    { duration: '2m', target: 50 },  // Ramp up to 50 users
    { duration: '5m', target: 50 },  // Stay at 50 users
    { duration: '2m', target: 100 }, // Ramp up to 100 users
    { duration: '5m', target: 100 }, // Stay at 100 users
    { duration: '2m', target: 0 },   // Ramp down to 0 users
  ],
  thresholds: {
    http_req_duration: ['p(95)<1000'], // 95% of requests must complete below 1000ms
    http_req_failed: ['rate<0.05'],     // Error rate must be below 5%
    errors: ['rate<0.05'],             // Custom error rate must be below 5%
    transaction_success: ['rate>0.95'], // Transaction success rate must be above 95%
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
let accountNumbers = {};

export function setup() {
  console.log('Setting up test data...');
  
  // Register test users and create accounts
  for (let i = 0; i < testUsers.length; i++) {
    const user = testUsers[i];
    
    // Register user
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
      
      // Create account
      const accountPayload = JSON.stringify({
        accountName: `Test Account ${i + 1}`,
        accountType: 'CHECKING',
        initialBalance: 1000000 // 1M KRW
      });
      
      const accountResponse = http.post(`${BASE_URL}/accounts`, accountPayload, {
        headers: { 
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${registerData.data.accessToken}`
        },
      });
      
      if (accountResponse.status === 200) {
        const accountData = JSON.parse(accountResponse.body);
        accountNumbers[user.email] = accountData.data.accountNumber;
        console.log(`Created account for user: ${user.email}`);
      }
    }
  }
  
  return { authTokens, accountNumbers };
}

export default function(data) {
  const user = testUsers[Math.floor(Math.random() * testUsers.length)];
  const token = data.authTokens[user.email];
  const fromAccount = data.accountNumbers[user.email];
  
  if (!token || !fromAccount) {
    console.log(`Missing data for user: ${user.email}`);
    return;
  }
  
  const headers = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`
  };
  
  // Get available accounts for transfer
  const accountsResponse = http.get(`${BASE_URL}/accounts`, { headers });
  if (accountsResponse.status !== 200) {
    errorRate.add(1);
    return;
  }
  
  const accountsData = JSON.parse(accountsResponse.body);
  const accounts = accountsData.data;
  
  if (accounts.length < 2) {
    console.log('Not enough accounts for transfer test');
    return;
  }
  
  // Find a different account for transfer
  const toAccount = accounts.find(acc => acc.accountNumber !== fromAccount);
  if (!toAccount) {
    console.log('No suitable target account found');
    return;
  }
  
  // Create transfer
  const transferPayload = JSON.stringify({
    fromAccountNumber: fromAccount,
    toAccountNumber: toAccount.accountNumber,
    amount: Math.floor(Math.random() * 10000) + 1000, // Random amount between 1K-11K
    description: 'Load test transfer',
    transactionType: 'TRANSFER',
    idempotencyKey: `test_${Date.now()}_${Math.random()}`
  });
  
  const transferResponse = http.post(`${BASE_URL}/transactions`, transferPayload, { headers });
  const transferCheck = check(transferResponse, {
    'transfer status is 200': (r) => r.status === 200,
    'transfer response time < 1000ms': (r) => r.timings.duration < 1000,
    'transfer has transaction ID': (r) => {
      if (r.status === 200) {
        const data = JSON.parse(r.body);
        return data.data && data.data.transactionId;
      }
      return false;
    },
  });
  
  errorRate.add(!transferCheck);
  transactionRate.add(transferCheck);
  
  sleep(2);
  
  // Get transaction history
  const transactionsResponse = http.get(`${BASE_URL}/transactions?page=0&size=5`, { headers });
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
