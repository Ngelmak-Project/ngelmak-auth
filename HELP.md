# 🔐 Ngelmak Vault: Secret management for Ngelmak

This documentation explains how to setup **HashiCorp Vault** as secret managemnt for Ngelmak-Project.  
The goal is to securely manage:

- A **JWT signing key** (for JJWT).
- **Postgres database credentials** (dynamic secrets).
- Transit encryption.
- Etc.

---

### 📂 Recommended Folder Layout

```bash
ngelmak-bao/
├── data/                 # Persistent Raft data (never touch manually)
├── config/               # OpenBao server config files
│   ├── config.hcl
│   ├── policies/
│   │   ├── spring-app-policy.hcl
│   │   ├── db-admin-policy.hcl
│   │   └── ...
│   ├── roles/            # Optional: JSON definitions for AppRoles
│   └── scripts/          # Optional: init/unseal scripts
└── README.md
```

---

### 📦 Prerequisites

This is the clean, stable, production‑ready way to run:

- PostgreSQL  
- OpenBao dynamic database credentials  
- Spring Boot  
- Hibernate auto‑DDL (optional)  

---
```bash
docker exec -it ngelmak-authdb bash
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

```bash
\du
```

### 1. PostgreSQL roles

### Create the OpenBao admin role (superuser)

```sql
CREATE ROLE vaultadmin WITH LOGIN SUPERUSER PASSWORD 'baopass';

CREATE ROLE
```
**Change vaultadmin password**

### Create the application database

```sql
CREATE DATABASE ngelmakauthdb OWNER vaultadmin;

CREATE DATABASE
```

### Create the schema owner role

```sql
CREATE ROLE app_migrator WITH LOGIN PASSWORD 'migratorpass';
GRANT ALL PRIVILEGES ON DATABASE ngelmakauthdb TO app_migrator;
```

Switch to the database:

```sql
\c ngelmakauthdb
```

Grant schema privileges:

```sql
GRANT USAGE, CREATE ON SCHEMA public TO app_migrator;
```

## 2. Create Schema

Pass the real credentials via command line:
#### If you run with Maven:
```bash
BOOTSTRAP_DB_USERNAME=app_migrator \
BOOTSTRAP_DB_PASSWORD=migratorpass \
mvn spring-boot:run -Dspring-boot.run.profiles=bootstrap
# or
BOOTSTRAP_DB_USERNAME=app_migrator \
BOOTSTRAP_DB_PASSWORD=migratorpass \
mvn spring-boot:run -Dspring-boot.run.profiles=bootstrap -Dspring-boot.run.arguments="--LIQUIBASE_RUN=true"
```


---

## 2. Configure the OpenBao for accesses

### Database Backend

#### 1. Postgres connection
Configure Postgres connection:

```bash
bao write database/config/ngelmak-auth-config \
    plugin_name=postgresql-database-plugin \
    allowed_roles="ngelmak-auth-role" \
    connection_url="postgresql://{{username}}:{{password}}@ngelmak_auth_db:5432/ngelmakauthdb?sslmode=disable" \
    username="admin" \
    password="changeme"

Success! Data written to: database/config/ngelmak-auth-config
```

#### 2. Define a role for dynamic users:

```bash
bao write database/roles/ngelmak-auth-role \
    db_name=ngelmak-auth-config \
    creation_statements="\
        CREATE ROLE \"{{name}}\" WITH LOGIN PASSWORD '{{password}}' VALID UNTIL '{{expiration}}'; \
        GRANT USAGE ON SCHEMA public TO \"{{name}}\"; \
        GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO \"{{name}}\"; \
        GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO \"{{name}}\"; \
        ALTER DEFAULT PRIVILEGES FOR ROLE app_migrator IN SCHEMA public \
            GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO \"{{name}}\"; \
        ALTER DEFAULT PRIVILEGES FOR ROLE app_migrator IN SCHEMA public \
            GRANT USAGE, SELECT ON SEQUENCES TO \"{{name}}\"; \
    " \
    default_ttl="24h" \
    max_ttl="48h"

Success! Data written to: database/roles/ngelmak-auth-role
```

### KV Secrets Engine

```bash
bao kv put kv/jjwt/dev jwt-secret-key="NzgwODE3NjExMzk1MDFjYzc2NmRjMmM2Yjc0ZTYyMGUxODM3ZThjMzk0ZTliMTE0MjhlNjliOWRhYTI2MzFkN2RkMGU3NDVhYTA0MzRkNTBkNGEzYmZlMzE1MTg4ZjVmYzA5NmFlNTEyZjkyZjYxMGJlMTM1NmU3ZmU0NDg2Yjk="

Success! Data written to: kv/jjwt/dev
```


### Define a Policy

Create `auth-app-policy.hcl`:

```bash
tee /etc/openbao/policies/auth-app-policy.hcl <<EOF
# Dynamic PostgreSQL credentials (read-only access to generated creds)
path "database/creds/ngelmak-auth-role" {
  capabilities = ["read"]
}

# KV reading JWT secret
path "kv/*" {
  capabilities = ["read"]
}
EOF
```
Load the policy:

```bash
bao policy write ngelmak-auth-policy /etc/openbao/policies/auth-app-policy.hcl

Success! Uploaded policy: ngelmak-auth-policy
```

Create an AppRole:

```bash
bao write auth/approle/role/ngelmak-auth-role \
  policies="ngelmak-auth-policy" \
  secret_id_ttl=48h \
  token_ttl=1h \
  token_max_ttl=4h

Success! Data written to: auth/approle/role/ngelmak-auth-role
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







BOOTSTRAP_DB_USERNAME=app_migrator \
BOOTSTRAP_DB_PASSWORD=migratorpass \
SPRING_PROFILES_ACTIVE=bootstrap \
LIQUIBASE_RUN=true \
docker compose up --build


docker compose up --build