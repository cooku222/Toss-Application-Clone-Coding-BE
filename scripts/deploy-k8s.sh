#!/bin/bash

# Toss Microservices Kubernetes Deployment Script

set -e

echo "🚀 Deploying Toss Microservices to Kubernetes"
echo "============================================="

# Check if kubectl is available
if ! command -v kubectl &> /dev/null; then
    echo "❌ kubectl is not installed. Please install kubectl and try again."
    exit 1
fi

# Check if cluster is accessible
if ! kubectl cluster-info &> /dev/null; then
    echo "❌ Kubernetes cluster is not accessible. Please check your kubeconfig."
    exit 1
fi

echo "📋 Current cluster context:"
kubectl config current-context

echo ""
echo "🔧 Creating namespace..."
kubectl apply -f k8s/namespace.yaml

echo "🔐 Creating secrets..."
kubectl apply -f k8s/secrets.yaml

echo "⚙️ Creating config maps..."
kubectl apply -f k8s/configmap.yaml

echo "🗄️ Deploying PostgreSQL..."
kubectl apply -f k8s/postgres.yaml

echo "⏳ Waiting for PostgreSQL to be ready..."
kubectl wait --for=condition=ready pod -l app=postgres -n toss-system --timeout=300s

echo "🔴 Deploying Redis..."
kubectl apply -f k8s/redis.yaml

echo "⏳ Waiting for Redis to be ready..."
kubectl wait --for=condition=ready pod -l app=redis -n toss-system --timeout=300s

echo "📨 Deploying Kafka..."
kubectl apply -f k8s/kafka.yaml

echo "⏳ Waiting for Kafka to be ready..."
kubectl wait --for=condition=ready pod -l app=kafka -n toss-system --timeout=300s

echo "🔐 Deploying Auth Service..."
kubectl apply -f k8s/auth-service.yaml

echo "⏳ Waiting for Auth Service to be ready..."
kubectl wait --for=condition=ready pod -l app=auth-service -n toss-system --timeout=300s

echo "✅ Deployment completed!"
echo ""
echo "📋 Deployment Status:"
echo "===================="
kubectl get pods -n toss-system

echo ""
echo "🌐 Service Endpoints:"
echo "===================="
kubectl get services -n toss-system

echo ""
echo "🔧 Useful Commands:"
echo "==================="
echo "View logs:        kubectl logs -f deployment/auth-service -n toss-system"
echo "Scale service:    kubectl scale deployment auth-service --replicas=3 -n toss-system"
echo "Port forward:     kubectl port-forward service/auth-service 8080:8080 -n toss-system"
echo "Delete deployment: kubectl delete namespace toss-system"
echo ""
echo "🎉 Deployment successful!"
