# Toss Microservices API Documentation

## Overview

This document provides comprehensive API documentation for the Toss-style microservices backend system.

## Base URL

- Development: `http://localhost:8080`
- Production: `https://api.toss.com`

## Authentication

All API endpoints (except authentication endpoints) require a valid JWT token in the Authorization header:

```
Authorization: Bearer <your-jwt-token>
```

## API Endpoints

### Authentication Service

#### Register User
```http
POST /auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123",
  "name": "John Doe",
  "phoneNumber": "+8210123456789"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "user": {
      "id": 1,
      "email": "user@example.com",
      "name": "John Doe",
      "phoneNumber": "+8210123456789",
      "roles": ["USER"],
      "lastLoginAt": null
    }
  }
}
```

#### Login
```http
POST /auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}
```

#### Refresh Token
```http
POST /auth/refresh
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

#### Get Current User
```http
GET /auth/me
Authorization: Bearer <token>
```

#### Logout
```http
POST /auth/logout
Authorization: Bearer <token>
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

### Account Service

#### Create Account
```http
POST /accounts
Authorization: Bearer <token>
Content-Type: application/json

{
  "accountName": "My Checking Account",
  "accountType": "CHECKING",
  "initialBalance": 1000000
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "accountNumber": "TOSS17031234567891234",
    "accountName": "My Checking Account",
    "accountType": "CHECKING",
    "status": "ACTIVE",
    "balance": 1000000.00,
    "availableBalance": 1000000.00,
    "dailyLimit": 1000000.00,
    "monthlyLimit": 10000000.00,
    "dailyUsedAmount": 0.00,
    "monthlyUsedAmount": 0.00,
    "lastUsedAt": null,
    "createdAt": "2024-01-01T00:00:00",
    "updatedAt": "2024-01-01T00:00:00"
  }
}
```

#### Get Accounts
```http
GET /accounts
Authorization: Bearer <token>
```

#### Get Account Summary
```http
GET /accounts/summary
Authorization: Bearer <token>
```

#### Get Account by Number
```http
GET /accounts/{accountNumber}
Authorization: Bearer <token>
```

#### Get Account Transactions
```http
GET /accounts/{accountId}/transactions?page=0&size=20
Authorization: Bearer <token>
```

#### Update Account
```http
PUT /accounts/{accountId}
Authorization: Bearer <token>
Content-Type: application/json

{
  "accountName": "Updated Account Name",
  "dailyLimit": 2000000,
  "monthlyLimit": 20000000
}
```

#### Transfer Money
```http
POST /accounts/transfer
Authorization: Bearer <token>
Content-Type: application/json

{
  "fromAccountNumber": "TOSS17031234567891234",
  "toAccountNumber": "TOSS17031234567891235",
  "amount": 100000,
  "description": "Transfer to friend",
  "idempotencyKey": "unique-key-123"
}
```

### Transaction Service

#### Create Transaction
```http
POST /transactions
Authorization: Bearer <token>
Content-Type: application/json

{
  "fromAccountNumber": "TOSS17031234567891234",
  "toAccountNumber": "TOSS17031234567891235",
  "amount": 100000,
  "description": "Payment for services",
  "transactionType": "TRANSFER",
  "idempotencyKey": "unique-key-456"
}
```

#### Get Transaction
```http
GET /transactions/{transactionId}
Authorization: Bearer <token>
```

#### Get Transactions
```http
GET /transactions?page=0&size=20&status=COMPLETED&transactionType=TRANSFER
Authorization: Bearer <token>
```

#### Get Transaction Summary
```http
GET /transactions/summary
Authorization: Bearer <token>
```

#### Update Transaction
```http
PUT /transactions/{transactionId}
Authorization: Bearer <token>
Content-Type: application/json

{
  "description": "Updated description"
}
```

#### Cancel Transaction
```http
POST /transactions/{transactionId}/cancel
Authorization: Bearer <token>
Content-Type: application/json

{
  "reason": "User requested cancellation"
}
```

## Error Responses

All error responses follow this format:

```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "Human readable error message",
    "details": {}
  },
  "timestamp": "2024-01-01T00:00:00"
}
```

### Common Error Codes

- `AUTH_001`: Authentication token expired
- `AUTH_002`: Authentication token invalid
- `AUTH_003`: Access denied
- `AUTH_004`: User not found
- `AUTH_005`: Password incorrect
- `ACC_001`: Account not found
- `ACC_002`: Insufficient balance
- `ACC_003`: Account already exists
- `ACC_004`: Account inactive
- `TXN_001`: Transaction not found
- `TXN_002`: Invalid amount
- `TXN_003`: Duplicate transaction
- `TXN_004`: Transaction limit exceeded
- `TXN_005`: Invalid recipient
- `VAL_001`: Validation failed
- `INT_001`: Internal server error

## Rate Limiting

API endpoints are rate limited to prevent abuse:

- Authentication endpoints: 10 requests per minute per IP
- Account endpoints: 100 requests per minute per user
- Transaction endpoints: 50 requests per minute per user

Rate limit headers are included in responses:

```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1640995200
```

## Pagination

List endpoints support pagination:

- `page`: Page number (0-based)
- `size`: Number of items per page (max 100)

Response includes pagination metadata:

```json
{
  "success": true,
  "data": {
    "content": [...],
    "page": 0,
    "size": 20,
    "totalElements": 150,
    "totalPages": 8,
    "first": true,
    "last": false
  }
}
```

## Webhooks

The system supports webhooks for real-time notifications:

### Transaction Completed
```json
{
  "eventType": "TRANSACTION_COMPLETED",
  "transactionId": "TXN_1703123456789_1",
  "userId": 1,
  "fromAccount": "TOSS17031234567891234",
  "toAccount": "TOSS17031234567891235",
  "amount": 100000,
  "status": "COMPLETED",
  "timestamp": "2024-01-01T00:00:00"
}
```

### Account Created
```json
{
  "eventType": "ACCOUNT_CREATED",
  "accountId": 1,
  "userId": 1,
  "accountNumber": "TOSS17031234567891234",
  "timestamp": "2024-01-01T00:00:00"
}
```

## SDKs

Official SDKs are available for:

- JavaScript/TypeScript
- Java
- Python
- Go

## Support

For API support, please contact:
- Email: api-support@toss.com
- Documentation: https://docs.toss.com
- Status Page: https://status.toss.com
