# 🔐 Ngelmak Auth - Vault Integration with Spring Boot

This documentation explains how to integrate **HashiCorp Vault** with a **Spring Boot authentication service**.  
The goal is to securely manage:

- A **JWT signing key** (for JJWT).
- **Postgres database credentials** (dynamic secrets).

---

## 📦 Prerequisites

- HashiCorp Vault installed and running (e.g., see install for [Linux](https://developer.hashicorp.com/vault/install#linux)).
- Postgres database accessible.
- Spring Boot project with `spring-cloud-starter-vault-config` dependency.

---

# 🔐 Ngelmak Vault: Secret management for Ngelmak

This documentation explains how to setup **OpenBao Vault** as secret managemnt for Ngelmak-Project.  
The goal is to securely manage:

- A **JWT signing key** (for JJWT).
- **Postgres database credentials** (dynamic secrets).
- Transit encryption.
- Etc.

---

### 📦 Prerequisites

This is the clean, stable, production‑ready way to run:

- PostgreSQL  
- OpenBao dynamic database credentials  
- Spring Boot  
- Hibernate auto‑DDL (optional)  

---
```bash
docker exec -it ngelmak-auth bash
```

After the container starts, connect:
```bash
psql -U admin postgres
```

Change the admin password (optional but recommended)
```bash
ALTER USER admin WITH PASSWORD 'new-admin-password';

ALTER ROLE
```
### 2. Database Setup

Create the application database and migration user:

```sql
-- Connect to PostgreSQL as superuser
psql -U admin postgres

-- Create the application database
CREATE DATABASE ngelmakauthdb OWNER vaultadmin;

-- Create the migration user with schema privileges
CREATE ROLE app_migrator WITH LOGIN PASSWORD 'migratorpass';

-- Grant database privileges
GRANT ALL PRIVILEGES ON DATABASE ngelmakauthdb TO app_migrator;

-- Switch to the new database and grant schema privileges
\c ngelmakauthdb
GRANT USAGE, CREATE ON SCHEMA public TO app_migrator;
```

## 2. Create Schema

Pass the real credentials via command line:
#### If you run with Maven:
```bash
DB_USERNAME=app_migrator \
DB_PASSWORD=migratorpass \
mvn spring-boot:run -Dspring-boot.run.profiles=bootstrap
# or
DB_USERNAME=app_migrator \
DB_PASSWORD=migratorpass \
mvn spring-boot:run -Dspring-boot.run.profiles=bootstrap -Dspring-boot.run.arguments="--LIQUIBASE_RUN=true"
```


---

## 2. Configure the OpenBao for accesses

### Database Backend


### KV Secrets Engine

```bash
bao kv put secret/jjwt/prod jwt-secret-key="NzgwODE3NjExMzk1MDFjYzc2NmRjMmM2Yjc0ZTYyMGUxODM3ZThjMzk0ZTliMTE0MjhlNjliOWRhYTI2MzFkN2RkMGU3NDVhYTA0MzRkNTBkNGEzY
mZlMzE1MTg4ZjVmYzA5NmFlNTEyZjkyZjYxMGJlMTM1NmU3ZmU0NDg2Yjk="

==== Secret Path ====
secret/data/jjwt/prod

======= Metadata =======
Key                Value
---                -----
created_time       2026-06-14T09:55:11.156869923Z
custom_metadata    <nil>
deletion_time      n/a
destroyed          false
version            1
```


### Define a Policy

Create a policy file (`springboot-policy.hcl`) to restrict Spring Boot's access:

```bash
tee /etc/openbao/policies/auth-app-policy.hcl <<EOF
# Allow reading database dynamic credentials
path "database/creds/ngelmak-springboot-role" {
  capabilities = ["read"]
}

# Allow reading JWT secrets under secret/jjwt/*
path "secret/data/jjwt/*" {
  capabilities = ["read"]
}

path "secret/metadata/jjwt/*" {
  capabilities = ["read"]
}
EOF
```
Load the policy:

```bash
bao policy write ngelmak-auth-policy /etc/openbao/policies/auth-app-policy.hcl

Success! Uploaded policy: ngelmak-auth-policy
```

Fetch Role ID:

```bash
bao read auth/approle/role/ngelmak-auth-role/role-id

Key        Value
---        -----
role_id    5c5885c7-0378-5245-75ca-6ce512f859c4
```

- **auth/approle/role/<role-name>/role-id** → path that returns the Role ID (non‑secret identifier).

Generate Secret ID:

```bash
bao write -f auth/approle/role/ngelmak-auth-role/secret-id

Key                   Value
---                   -----
secret_id             5f51b847-9bec-708d-0aad-923560929a42
secret_id_accessor    70391877-226a-2724-aeee-7cfa774594d4
secret_id_num_uses    0
secret_id_ttl         48h
```

---

## 5. Spring Boot Configuration

Add dependency in `pom.xml`:

```xml
<dependency>
  <groupId>org.springframework.cloud</groupId>
  <artifactId>spring-cloud-starter-vault-config</artifactId>
  <version>2.1.3.RELEASE</version>
</dependency>
```

- **spring-cloud-starter-vault-config** → integrates Spring Boot with Vault, exposing secrets as properties.

Configure `application.yml`:

```yml
spring:
  application:
    name: auth-service

  profiles:
    active: prod # or prod

  config.import: optional:vault:// # Enable Vault as a Config Data source. This requires at least one Vault backend to be enabled and resolvable.
  cloud:
    vault:
      uri: http://vault:8200
      authentication: approle
      fail-fast: true # Enable fail-fast to force confirmation. Spring Boot to crash immediately when Vault/OpenBao is not reachable
      app-role:
        role-id: ${VAULT_ROLE_ID} # The Vault AppRole Role ID
        secret-id: ${VAULT_SECRET_ID} # The Vault AppRole Secret ID
      database:
        enabled: true # Disable the database backend if not needed. Set to true if you want to use Vault's database secrets engine for dynamic credentials.
        backend: database
        role: ngelmak-springboot-role
      kv:
        enabled: true
        application-name: "" # skip the application name prefix
        backend: secret # The Vault KV backend path (e.g. "secret" for KV v2 or "kv" for KV v1)
        default-context: jjwt/prod

  datasource:
    # hikari:
    #   initialization-fail-timeout: 0 # Don't fail on startup if the database is unavailable. Useful for development and testing environments.
    url: jdbc:postgresql://postgres:5432/ngelmakauthdb
    # username:
    # password:
```

- **uri** → Vault server address.
- **authentication** → auth method (`approle`).
- **app-role.role-id / secret-id** → values retrieved from Vault (supply via env vars).
- **kv.enabled** → enables KV property source.
- **kv.backend** → KV mount path (`secret`).
- **kv.default-context** → subpath for the secret (`jjwt` → resolves to `secret/data/jjwt`).
- The KV entry `jwt-secret-key="super-secret"` becomes a Spring property named `jwt-secret-key`, which can be injected with:

```java
@Value("${jwt-secret-key}")
private String jwtSecret;
```

- **spring.cloud.vault.database.enabled: true** → tells Spring Cloud Vault to fetch dynamic DB creds.
- **spring.cloud.vault.database.role: springboot-db-role** → matches the Vault role you defined.
- Spring Cloud Vault injects those values into `spring.cloud.vault.database.username/password`, then DataSource uses Vault‑issued credentials to authenticate.

🗝️ **At runtime**

1. Spring Boot starts.
2. Spring Cloud Vault authenticates to Vault (AppRole).
3. Vault issues a token.
4. Spring Cloud Vault fetches Database creds → exposed as `spring.cloud.vault.database.username/password`.
5. Spring Boot’s DataSource picks up those properties and connects to Postgres.

---

## ✅ Summary

- **KV engine** → stores JWT secret.
- **Database engine** → generates dynamic Postgres credentials.
- **Policy** → restricts access to only required paths.
- **AppRole** → authenticates Spring Boot app.
- **Spring Boot config** → fetches secrets at runtime, no hardcoding.


## 🤝 Contributing

Contributions are welcome! Please follow these guidelines:

1. **Fork** the repository
2. **Create a feature branch** (`git checkout -b feature/amazing-feature`)
3. **Commit your changes** (`git commit -m 'Add amazing feature'`)
4. **Push to the branch** (`git push origin feature/amazing-feature`)
5. **Open a Pull Request**

### Code Style

- Follow [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- Use meaningful commit messages
- Write tests for new features

---

## 📄 License

This project is licensed under the **GNU General Public License v3.0** — see the [LICENSE](LICENSE) file for details.

You are free to:
- **Use** this software for any purpose
- **Copy** and distribute it
- **Modify** and distribute modified versions

Under the condition that you:
- **Disclose** the source code
- **License** derivative works under GPLv3
- **Include** a copy of this license

For the full license text, visit [gnu.org/licenses/gpl-3.0.html](https://www.gnu.org/licenses/gpl-3.0.html)
---

## 📞 Support

For issues, questions, or suggestions:

- **GitHub Issues**: [Report a bug](https://github.com/yourusername/Ngelmak-Thruline-Core/issues)
- **Discussions**: [Join the conversation](https://github.com/yourusername/Ngelmak-Thruline-Core/discussions)

---

## 🔗 Related Projects

- [Ngelmak API Gateway](https://github.com/yourusername/Ngelmak-API-Gateway)
- [Ngelmak User Service](https://github.com/yourusername/Ngelmak-User-Service)
- [Ngelmak Infrastructure](https://github.com/yourusername/Ngelmak-Infrastructure)