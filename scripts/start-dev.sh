#!/bin/bash

# Toss Microservices Development Environment Startup Script

set -e

echo "🚀 Starting Toss Microservices Development Environment"
echo "======================================================"

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker and try again."
    exit 1
fi

# Check if Docker Compose is available
if ! command -v docker-compose &> /dev/null; then
    echo "❌ Docker Compose is not installed. Please install Docker Compose and try again."
    exit 1
fi

# Create necessary directories
echo "📁 Creating necessary directories..."
mkdir -p logs
mkdir -p data/postgres
mkdir -p data/redis
mkdir -p data/kafka

# Set environment variables
export JWT_SECRET="mySecretKey123456789012345678901234567890"
export POSTGRES_PASSWORD="toss_password"
export REDIS_PASSWORD=""

echo "🐳 Starting infrastructure services..."
docker-compose up -d postgres redis zookeeper kafka

echo "⏳ Waiting for services to be ready..."
sleep 30

echo "📊 Starting monitoring services..."
docker-compose up -d prometheus grafana elasticsearch kibana logstash

echo "⏳ Waiting for monitoring services to be ready..."
sleep 30

echo "🔧 Building microservices..."
# Build shared library first
cd shared && ./gradlew build -x test && cd ..

# Build all services
cd services/auth-service && ./gradlew build -x test && cd ../..
cd services/account-service && ./gradlew build -x test && cd ../..
cd services/transaction-service && ./gradlew build -x test && cd ../..

echo "🚀 Starting microservices..."
docker-compose up -d auth-service account-service transaction-service

echo "⏳ Waiting for microservices to be ready..."
sleep 60

echo "🌐 Starting API Gateway..."
docker-compose up -d api-gateway

echo "✅ Development environment is ready!"
echo ""
echo "📋 Service URLs:"
echo "=================="
echo "API Gateway:     http://localhost:8080"
echo "Auth Service:    http://localhost:8081"
echo "Account Service: http://localhost:8082"
echo "Transaction Service: http://localhost:8083"
echo "Grafana:         http://localhost:3000 (admin/admin)"
echo "Prometheus:      http://localhost:9090"
echo "Kibana:          http://localhost:5601"
echo ""
echo "🔧 Useful Commands:"
echo "==================="
echo "View logs:       docker-compose logs -f [service-name]"
echo "Stop services:   docker-compose down"
echo "Restart service: docker-compose restart [service-name]"
echo "Scale service:   docker-compose up -d --scale [service-name]=3"
echo ""
echo "🧪 Test the API:"
echo "================"
echo "curl -X POST http://localhost:8080/auth/register \\"
echo "  -H 'Content-Type: application/json' \\"
echo "  -d '{\"email\":\"test@example.com\",\"password\":\"password123\",\"name\":\"Test User\",\"phoneNumber\":\"+8210123456789\"}'"
echo ""
echo "🎉 Happy coding!"
