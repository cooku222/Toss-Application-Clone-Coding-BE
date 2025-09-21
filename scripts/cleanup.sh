#!/bin/bash

# Toss Microservices Cleanup Script

set -e

echo "🧹 Cleaning up Toss Microservices Environment"
echo "============================================="

# Function to confirm action
confirm() {
    read -p "Are you sure you want to continue? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "❌ Operation cancelled."
        exit 1
    fi
}

echo "This script will clean up:"
echo "- Docker containers and volumes"
echo "- Kubernetes resources"
echo "- Test data and logs"
echo ""

confirm

echo "🐳 Stopping and removing Docker containers..."
if command -v docker-compose &> /dev/null; then
    docker-compose down -v --remove-orphans
fi

echo "🗑️ Removing Docker images..."
docker images | grep toss | awk '{print $3}' | xargs -r docker rmi -f

echo "🧹 Cleaning up Docker system..."
docker system prune -f

echo "☸️ Cleaning up Kubernetes resources..."
if command -v kubectl &> /dev/null; then
    kubectl delete namespace toss-system --ignore-not-found=true
fi

echo "📁 Cleaning up local data..."
rm -rf data/
rm -rf logs/
rm -rf test-results/
rm -rf security-reports/

echo "🔧 Cleaning up build artifacts..."
find . -name "build" -type d -exec rm -rf {} + 2>/dev/null || true
find . -name "target" -type d -exec rm -rf {} + 2>/dev/null || true
find . -name "*.log" -type f -delete 2>/dev/null || true

echo "✅ Cleanup completed!"
echo ""
echo "🎉 Environment has been cleaned up successfully."
echo "You can now run ./scripts/start-dev.sh to start fresh."
