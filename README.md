# Gentleman Store 🎩

A full-stack e-commerce platform for premium menswear — suits, shirts, shoes, ties, and accessories. Built as a native Android application backed by a Spring Boot REST API and PostgreSQL, with role-based access for customers, employees, managers, and administrators.

---

## Badges

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.3-brightgreen?logo=springboot)
![Android API](https://img.shields.io/badge/Android%20API-31%2B-green?logo=android)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-purple?logo=kotlin)
![License](https://img.shields.io/badge/License-MIT-blue)

---

## ✨ Features

- 🛍️ **Product catalog** — paginated grid with search, category filters, sort, and an alternative vertical swipe/feed view
- 🧺 **Shopping cart** — add/remove items, automatic discount application at the item level
- 💳 **Checkout** — address management, payment method selection, promo code validation, loyalty tier discount
- 📦 **Order tracking** — full order history with status filters, detail view, and cancellation
- 👑 **Loyalty program** — four tiers (Gentleman / Distinguished / Elite / Black Label) with automatic point earning and tier promotion
- 🏷️ **Discount & promotions** — percentage and fixed-amount codes, product or category-scoped, with broadcast notifications on creation
- 🔔 **In-app notifications** — per-type filtering, unread badge, bulk mark-as-read
- 🎧 **Support chat** — structured bot intake questionnaire, live chat with staff, per-ticket unread counts
- 📊 **Analytics dashboard** — revenue, order counts, new users, monthly revenue chart, top products (MANAGER/ADMIN)
- 🗂️ **Admin panel** — user management, role assignment, account deactivation/reactivation
- 🛡️ **Security hardening** — JWT with rotating refresh tokens, BCrypt, rate limiting, IDOR protection, AES-256-GCM token storage on device

---

## 🏗️ Architecture

```
┌─────────────────────────────┐
│  Android App (Kotlin)        │
│  Jetpack Compose · MVVM      │
│  Screens → ViewModels        │
│    → Repositories            │
│       → Retrofit ApiService  │
└──────────────┬───────────────┘
               │ HTTPS · JWT Bearer Token
               ▼
┌─────────────────────────────┐
│  Spring Boot REST API        │
│  Java 21 · Spring Boot 3.4   │
│  Controller → Service        │
│    → Repository (JPA)        │
│  JwtAuthenticationFilter     │
│  AuthRateLimitingFilter       │
└──────────────┬───────────────┘
               │ JDBC (Hibernate / JPA)
               ▼
┌─────────────────────────────┐
│  PostgreSQL 16               │
│  Schema managed by Liquibase │
│  Migrations V1 – V20         │
└─────────────────────────────┘
```

### Tech Stack

| Technology | Version | Purpose |
|---|---|---|
| Java | 21 | Backend runtime |
| Spring Boot | 3.4.3 | REST API framework |
| PostgreSQL | 16 | Primary database |
| Liquibase | Spring Boot managed | Schema migrations |
| jjwt | 0.12.6 | JWT generation & validation |
| Bucket4j | 8.19.0 | Auth rate limiting |
| MapStruct | 1.5.5.Final | Entity ↔ DTO mapping |
| Lombok | 1.18.30 | Boilerplate reduction |
| Kotlin | 2.0.21 | Android development language |
| Jetpack Compose | BOM 2024.09.00 | Declarative Android UI |
| Hilt | 2.56.1 | Dependency injection |
| Retrofit | 2.11.0 | HTTP client |
| OkHttp | 4.12.0 | HTTP engine + token refresh |
| DataStore | 1.1.4 | Encrypted local token storage |
| Coil | 2.7.0 | Image loading |

---

## 📱 Screenshots

> _Screenshots coming soon. To add them, place images in a `docs/screenshots/` folder and update this section._

---

## 🚀 Getting Started

### Prerequisites

| Tool | Version |
|---|---|
| JDK | 21 |
| Maven | 3.9+ |
| PostgreSQL | 16 |
| Android Studio | Latest stable (AGP 8.11.2, SDK Platform 36) |
| Docker + Compose | Optional (for containerized setup) |

---

### Backend Setup

1. **Clone the repository**
   ```bash
   git clone <repo-url>
   cd gntl_app/gntl_app_backend
   ```

2. **Create the database**
   ```bash
   # Option A — local PostgreSQL
   createdb gentleman_store

   # Option B — Docker (starts Postgres only)
   docker-compose up postgres
   ```

3. **Set required environment variables** (see [Environment Variables](#-environment-variables) below)

4. **Run the application**
   ```bash
   # Maven
   mvn spring-boot:run

   # Or build and run the JAR
   mvn clean package
   java -jar target/gentleman-store-0.0.1-SNAPSHOT.jar

   # Docker Compose (starts Postgres + backend)
   docker-compose up
   ```

   Liquibase migrations (V1–V20) run automatically on startup. The API is available at `http://localhost:8080/api/`.

---

### Frontend Setup

1. **Open the project** — open `gntl_app_frontend/` in Android Studio. Gradle sync downloads all dependencies automatically.

2. **`local.properties`** — Android Studio generates this file on first open with your local SDK path:
   ```properties
   sdk.dir=C:\Users\<you>\AppData\Local\Android\Sdk
   ```

3. **Run on emulator** — create an AVD with API level 31+, ensure the backend is running locally on port 8080, and launch the `debug` build variant. The emulator reaches the host machine at `10.0.2.2:8080`.

4. **Run on a physical device** — update the `debug` `BASE_URL` in `app/build.gradle.kts` from `10.0.2.2` to your machine's local IP, and allow that IP in `app/src/debug/res/xml/network_security_config.xml`.

---

## 🔐 Environment Variables

All secrets are loaded from environment variables at runtime. The application will not start without them.

| Variable | Required | Description |
|---|---|---|
| `DB_USERNAME` | ✅ | PostgreSQL username |
| `DB_PASSWORD` | ✅ | PostgreSQL password |
| `JWT_SECRET` | ✅ | Base64-encoded HMAC signing key for JWTs (generate with `openssl rand -base64 64`) |
| `MAIL_USERNAME` | ✅ | Gmail SMTP account address |
| `MAIL_PASSWORD` | ✅ | Gmail app password (not the account password) |
| `CORS_ALLOWED_ORIGINS` | ➖ | Comma-separated list of allowed CORS origins (empty = no cross-origin access) |

In IntelliJ IDEA, add these in the **Run Configuration → Environment variables** field. Never commit them to the repository.

---

## 👥 User Roles

| Role | Capabilities |
|---|---|
| `CUSTOMER` | Browse catalog, manage cart, checkout, track orders, loyalty program, support tickets, notifications, profile & addresses |
| `EMPLOYEE` | All customer API access + manage all orders (status updates), staff support chat, product CRUD, inventory updates |
| `MANAGER` | Analytics dashboard, discount & promotion management, loyalty point grants, read access to all orders/payments/tickets |
| `ADMIN` | All of the above + user management (roles, deactivation, reactivation), delete-access on all resources |

Roles are flat — each `@PreAuthorize` annotation explicitly lists every permitted role. Assigning a new role forces a re-login by revoking all existing refresh tokens.

---

## 🔒 Security

- **JWT authentication** — 15-minute access tokens, 30-day single-use rotating refresh tokens stored as SHA-256 hashes
- **BCrypt password hashing** — strength 10; password policy enforces minimum 8 characters, at least one digit, uppercase letter, and symbol
- **Rate limiting** — Bucket4j, 5 requests/minute per IP on `/api/auth/login` and `/api/auth/register`
- **IDOR protection** — ownership checks on all user-scoped resources; returns 404 instead of 403 to avoid resource enumeration
- **CORS** — explicit whitelist via environment variable, restricted headers and methods, no credentials
- **Android Keystore encryption** — access and refresh tokens, user role, and user ID are encrypted with AES-256-GCM before writing to DataStore
- **Backup exclusion** — DataStore file containing encrypted tokens is excluded from Android Auto Backup and device transfer
- **Network security** — cleartext HTTP globally disabled in release builds; debug allows it only to `10.0.2.2`
- **ProGuard/R8** — minification and obfuscation enabled for release builds

---

## 📡 API Overview

All endpoints return `{ success, message, data }` (the `ApiResponse<T>` envelope). Base path: `/api/`.

| Module | Base Path | Description |
|---|---|---|
| Auth | `/api/auth` | Register, login, token refresh, logout — all public |
| Users | `/api/users` | Profile management, role changes, account lifecycle |
| Addresses | `/api/addresses` | Customer delivery addresses (CRUD) |
| Products | `/api/products` | Catalog with pagination, search, and category filter |
| Cart | `/api/cart` | Add/remove items, checkout |
| Orders | `/api/orders` | Order creation, status management, history |
| Payments | `/api/payments` | Payment records and status updates |
| Inventory | `/api/inventory` | Stock levels per product size, low-stock alerts |
| Loyalty | `/api/loyalty` | Account, tiers, point transactions |
| Discounts | `/api/discounts` | Promo codes, promotions, validation |
| Notifications | `/api/notifications` | In-app notifications, unread count |
| Support | `/api/support` | Tickets, chat messages, bot intake |
| Analytics | `/api/analytics` | Sales dashboard (MANAGER/ADMIN) |

---

## 🗄️ Database

Schema is managed exclusively through Liquibase migrations (V1–V20). Hibernate is set to `validate` — it never modifies the schema.

| Table group | Key tables |
|---|---|
| Users & auth | `users`, `roles`, `user_roles`, `addresses`, `refresh_tokens` |
| Products | `categories`, `products`, `sizes`, `images`, `tags`, `product_tags`, `outfits`, `outfit_items` |
| Orders & payments | `orders`, `order_items`, `shipments`, `payments` |
| Inventory | `inventory`, `stock_alerts` |
| Loyalty | `loyalty_tiers`, `loyalty_accounts`, `loyalty_transactions`, `points_rules` |
| Discounts | `discounts`, `promotions`, `user_promotions` |
| Support | `support_tickets`, `chat_sessions`, `chat_messages`, `bot_questions`, `bot_responses` |
| Notifications | `notifications` |
| Cart | `carts`, `cart_items` |

All 32 entities use soft-delete (`deleted = true`) — no hard deletes anywhere in the application.

