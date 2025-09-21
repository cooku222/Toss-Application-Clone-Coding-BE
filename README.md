# 토스 스타일 백엔드

토스의 핀테크 서비스를 모방한 마이크로서비스 아키텍처 백엔드 시스템입니다.

## 🏗️ 아키텍처

### 마이크로서비스 구성
- **Auth Service**: 사용자 인증 및 권한 관리
- **Account Service**: 계좌 정보 관리
- **Transaction Service**: 거래 처리 및 검증
- **Ledger Service**: 원장 관리 및 정산
- **Notification Service**: 알림 발송

### 기술 스택
- **언어/프레임워크**: Kotlin + Spring Boot / Spring Cloud
- **데이터베이스**: PostgreSQL (샤딩/리플리카)
- **캐시**: Redis (세션, 분산락)
- **메시지 큐**: Kafka (이벤트 처리, DLQ)
- **인증**: OAuth2/JWT (RS256), refresh token rotation
- **컨테이너**: Docker + Kubernetes
- **모니터링**: Prometheus + Grafana, ELK Stack
- **시크릿 관리**: Vault
- **CI/CD**: GitHub Actions

## 🚀 빠른 시작

### 개발 환경 실행
```bash
# Docker Compose로 전체 스택 실행
docker-compose up -d

# 또는 Kubernetes로 실행
kubectl apply -f k8s/
```

### API 테스트
```bash
# 인증 토큰 발급
curl -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test@example.com","password":"password"}'

# 계좌 조회
curl -X GET http://localhost:8082/accounts \
  -H "Authorization: Bearer <token>"
```

## 📊 모니터링

- **Grafana**: http://localhost:3000
- **Prometheus**: http://localhost:9090
- **Kibana**: http://localhost:5601

## 🔒 보안

- JWT 토큰 기반 인증
- Refresh token rotation
- Idempotency keys
- Rate limiting
- Input validation
- SQL injection 방지

## 🧪 테스트

### 부하 테스트
```bash
# k6 부하 테스트 실행
k6 run tests/load/auth-load-test.js
k6 run tests/load/transaction-load-test.js
```

### 보안 테스트
```bash
# 의존성 스캔
trivy fs .
snyk test

# API 보안 테스트
./scripts/security-test.sh
```

## 📁 프로젝트 구조

```
├── services/                 # 마이크로서비스
│   ├── auth-service/        # 인증 서비스
│   ├── account-service/     # 계좌 서비스
│   ├── transaction-service/ # 거래 서비스
│   ├── ledger-service/      # 원장 서비스
│   └── notification-service/# 알림 서비스
├── shared/                  # 공통 라이브러리
├── infrastructure/          # 인프라 설정
│   ├── docker/             # Docker 설정
│   ├── k8s/                # Kubernetes 매니페스트
│   └── monitoring/         # 모니터링 설정
├── tests/                   # 테스트
│   ├── load/               # 부하 테스트
│   └── security/           # 보안 테스트
└── docs/                    # 문서
```

## 📋 개발 가이드

### 새로운 서비스 추가
1. `services/` 디렉토리에 새 서비스 생성
2. `shared/` 라이브러리 활용
3. Docker 및 K8s 매니페스트 추가
4. 모니터링 설정 추가

### API 개발 규칙
- RESTful API 설계
- OpenAPI 3.0 스펙 준수
- 에러 응답 표준화
- 로깅 및 메트릭 수집

## 🔄 CI/CD

GitHub Actions를 통한 자동화:
- 코드 빌드 및 테스트
- Docker 이미지 빌드
- 보안 스캔
- Kubernetes 배포

## 📞 지원

문제가 발생하면 이슈를 생성해 주세요.
