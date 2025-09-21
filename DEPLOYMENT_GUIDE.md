# Toss Microservices Deployment Guide

## 🚀 Quick Start

### Prerequisites
- Docker & Docker Compose
- Java 17+
- Gradle 7+
- kubectl (for Kubernetes deployment)
- k6 (for load testing)

### 1. Development Environment

```bash
# Clone the repository
git clone <repository-url>
cd toss-microservices

# Start development environment
./scripts/start-dev.sh

# Test the API
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123","name":"Test User","phoneNumber":"+8210123456789"}'
```

### 2. Production Deployment (Kubernetes)

```bash
# Deploy to Kubernetes
./scripts/deploy-k8s.sh

# Check deployment status
kubectl get pods -n toss-system
```

## 📋 Service Architecture

### Core Services
- **Auth Service** (Port 8081): User authentication & authorization
- **Account Service** (Port 8082): Account management & transfers
- **Transaction Service** (Port 8083): Transaction processing
- **API Gateway** (Port 8080): Request routing & load balancing

### Infrastructure
- **PostgreSQL**: Primary database
- **Redis**: Caching & session storage
- **Kafka**: Message queuing & events
- **Prometheus**: Metrics collection
- **Grafana**: Monitoring dashboards
- **ELK Stack**: Logging & analysis

## 🔧 Configuration

### Environment Variables
```bash
# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/toss_db
SPRING_DATASOURCE_USERNAME=toss_user
SPRING_DATASOURCE_PASSWORD=toss_password

# Redis
SPRING_REDIS_HOST=redis
SPRING_REDIS_PORT=6379

# Kafka
KAFKA_BOOTSTRAP_SERVERS=kafka:9092

# JWT
JWT_SECRET=mySecretKey123456789012345678901234567890
```

### Service URLs
- **API Gateway**: http://localhost:8080
- **Grafana**: http://localhost:3000 (admin/admin)
- **Prometheus**: http://localhost:9090
- **Kibana**: http://localhost:5601

## 🧪 Testing

### Load Testing
```bash
# Run load tests
./scripts/run-tests.sh

# Individual tests
k6 run tests/load/auth-load-test.js
k6 run tests/load/transaction-load-test.js
```

### Security Testing
```bash
# Run security tests
./scripts/security/security-test.sh
```

## 📊 Monitoring

### Key Metrics
- Request rate and response times
- Error rates and types
- Database performance
- JVM memory usage
- Kafka consumer lag

### Alerts
- High error rates (>1%)
- Slow response times (>500ms)
- Database connection issues
- Memory usage (>80%)

## 🔒 Security

### Authentication
- JWT tokens with refresh rotation
- Rate limiting on auth endpoints
- Account lockout after failed attempts

### Authorization
- Role-based access control (RBAC)
- API endpoint protection
- Resource-level permissions

### Data Protection
- TLS 1.3 encryption in transit
- AES-256 encryption at rest
- Input validation and sanitization
- SQL injection prevention

## 🚨 Troubleshooting

### Common Issues

#### Services not starting
```bash
# Check Docker status
docker ps

# View service logs
docker-compose logs -f [service-name]

# Restart services
docker-compose restart
```

#### Database connection issues
```bash
# Check PostgreSQL status
docker-compose logs postgres

# Test connection
docker-compose exec postgres psql -U toss_user -d toss_db
```

#### High memory usage
```bash
# Check resource usage
docker stats

# Scale services
docker-compose up -d --scale auth-service=2
```

### Performance Issues

#### Slow response times
1. Check database query performance
2. Monitor Redis cache hit rates
3. Review Kafka consumer lag
4. Scale services horizontally

#### High error rates
1. Check application logs
2. Monitor database connections
3. Verify external service health
4. Review rate limiting settings

## 📈 Scaling

### Horizontal Scaling
```bash
# Scale services
docker-compose up -d --scale auth-service=3
docker-compose up -d --scale account-service=3

# Kubernetes scaling
kubectl scale deployment auth-service --replicas=5 -n toss-system
```

### Database Scaling
- Read replicas for read-heavy workloads
- Connection pooling optimization
- Query optimization and indexing

## 🔄 Maintenance

### Regular Tasks
- **Daily**: Monitor system health and alerts
- **Weekly**: Review logs and performance metrics
- **Monthly**: Security updates and dependency scanning
- **Quarterly**: Capacity planning and architecture review

### Backup Strategy
- Database backups (daily)
- Configuration backups (weekly)
- Disaster recovery testing (monthly)

## 📞 Support

### Documentation
- API Documentation: `docs/API_DOCUMENTATION.md`
- Threat Model: `docs/THREAT_MODEL.md`
- Final Report: `docs/FINAL_REPORT.md`

### Monitoring
- Grafana Dashboards: http://localhost:3000
- Prometheus Metrics: http://localhost:9090
- Kibana Logs: http://localhost:5601

### Emergency Contacts
- On-call engineer: [contact-info]
- Security team: [contact-info]
- Infrastructure team: [contact-info]

---

**Last Updated**: January 2024  
**Version**: 1.0
