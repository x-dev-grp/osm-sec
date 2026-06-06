# Security Service (osm-sec)

Authentication and Authorization microservice. Acts as the OAuth2 Authorization Server and Resource Server.

## 📖 Functional Overview
Provides centralized security for the entire ecosystem. It ensures that only authorized users can access sensitive production and financial data.

### Key Features
- **OAuth2 / JWT Issuance**: Issues secure tokens for cross-service communication and frontend access.
- **RBAC (Role-Based Access Control)**: Manages permissions for different user levels (Admin, Producer, Accountant, etc.).
- **OTP Verification**: Multi-factor authentication for sensitive actions and login security.
- **User Management**: Unified registration and profile management.


## 🛠 Tech Stack
- **Spring Security OAuth2**
- **PostgreSQL** (`osmsecurity`)
- **Discovery:** Netflix Eureka

## 🚀 Getting Started
```bash
./mvnw spring-boot:run
```

## ⚙️ Configuration
| Variable | Default | Description |
| :--- | :--- | :--- |
| `SERVER_PORT` | `8088` | Auth Server Port |
| `DB_URL` | `jdbc:postgresql://localhost:5432/osmsecurity` | Database URL |
| `JWT_ISSUER_URI` | `http://localhost:8088` | Issuer Identity |
