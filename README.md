# DepLens – Software Supply Chain Intelligence & Cascading Blast Radius Engine

> **Course:** JAVA and Spring Framework Lab (CS3603-1)  
> **Team No.:** 24CSB13 (Section B)  
> **Members:**
> - Gautham Salian (NNM24CS090)
> - K Smrithi Shenoy (NNM24CS110)
> - Kanishk Subhash Hegde (NNM24CS111)
> - Kashvi Suresh (NNM24CS114)

---

## 📌 Project Overview

**DepLens** is a web-based software supply chain intelligence platform that transforms opaque direct and transitive dependency trees into actionable risk insights. 

Built with **Java 21** and **Spring Boot 3**, the system:
- **Dependency Graph Construction:** Parses project manifests (`pom.xml`, `package.json`) into directed dependency graphs using **JGraphT**.
- **AST Exposure Analysis:** Inspects Abstract Syntax Trees (ASTs) via **JavaParser** to identify exact source code files, lines, and routes exposed to vulnerable packages.
- **Cascading Blast Radius:** Calculates dependency depth, centrality, and reverse-BFS blast radius to prioritize high-exposure risks over unused components.
- **GitHub OAuth2 & 30-Day Sessions:** Authenticates users via GitHub OAuth2 and issues persistent 30-day JWT sessions.
- **AI Remediation Guidance:** Connects with Spring AI (Gemini / OpenAI API) to summarize affected code paths and evaluate low-cost upgrade plans.

---

## 🛠 Tech Stack & Prerequisites

| Layer | Technology |
|---|---|
| **Language & Runtime** | Java 21 LTS (Eclipse Temurin) |
| **Backend Framework** | Spring Boot 3.3.5 |
| **Security & Auth** | Spring Security 6, OAuth2 Client, JJWT 0.12.6 (30-day expiration) |
| **Database & ORM** | MySQL 8.0+ / 9.0+, Spring Data JPA, Hibernate 6 |
| **Graph & AST Analysis** | JGraphT 1.5.2, JavaParser 3.26.1 |
| **Build Tool** | Maven 3.9+ (includes portable `./mvnw` wrapper) |
| **Frontend** | HTML5, CSS3 (Modern Dark Glassmorphism), Vanilla JavaScript |

### System Prerequisites Installation

#### macOS (via Homebrew)
```bash
# 1. Install Java 21 LTS
brew install --cask temurin@21

# 2. Install Maven
brew install maven

# 3. Install & Start MySQL
brew install mysql
brew services start mysql
```

#### Windows / Linux
- **Java 21:** Download and install from [Adoptium Temurin](https://adoptium.net/temurin/releases/?version=21).
- **MySQL:** Download [MySQL Community Server](https://dev.mysql.com/downloads/mysql/) and ensure the service is running on port `3306`.

---

## ⚙️ Step-by-Step Setup Guide

### 1. Clone the Repository & Open in VS Code
```bash
git clone https://github.com/GauthamSalian/DepLensJSF.git
cd DepLensJSF
code .
```
*(When prompted by VS Code, click **Install All** to load the recommended Java, Spring Boot, and MySQL extensions configured in `.vscode/extensions.json`.)*

---

### 2. Set Up the MySQL Database

Log in to your local MySQL CLI:
```bash
mysql -u root
```

Create the application database:
```sql
CREATE DATABASE IF NOT EXISTS deplens_db;
EXIT;
```
*(Hibernate will automatically create and update all tables including `users` on application boot.)*

---

### 3. Create GitHub OAuth App Credentials

To enable **Sign in with GitHub**, each team member or deployment must create an OAuth App on GitHub:

1. Open your browser and navigate to:  
   👉 **[GitHub Developer Settings > OAuth Apps](https://github.com/settings/developers)**
2. Click **"New OAuth App"** (or **"Register a new application"**).
3. Fill in the application details exactly as shown below:

   | Field | Value | Notes |
   |---|---|---|
   | **Application name** | `DepLens` | Friendly display name |
   | **Homepage URL** | `http://localhost:8080` | Local development address |
   | **Application description** | `DepLens Software Supply Chain Engine` | Optional |
   | **Authorization callback URL** | `http://localhost:8080/login/oauth2/code/github` | **Crucial:** Must match Spring Security's OAuth2 redirect URI |

4. Click **"Register application"**.
5. On the registered app page:
   - Copy the **Client ID** (e.g., `Ov23li...`).
   - Click **"Generate a new client secret"** and copy the generated **Client Secret**.

---

### 4. Configure Local Secrets (`.env`)

DepLens is configured to automatically load local secrets from a `.env` file (which is ignored in `.gitignore` so your keys are never leaked to Git).

1. Copy the template to `.env`:
   ```bash
   cp .env.example .env
   ```
2. Open [`.env`](file:///Users/gauthamsalian/DepLensJSF/.env) and paste your credentials:
   ```properties
   # GitHub OAuth2 Credentials
   GITHUB_CLIENT_ID=your_github_client_id
   GITHUB_CLIENT_SECRET=your_github_client_secret

   # JWT Token Configuration (30-day session)
   JWT_SECRET=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
   JWT_EXPIRATION_MS=2592000000

   # MySQL Credentials
   DB_USERNAME=root
   DB_PASSWORD=
   ```
Spring Boot automatically imports `.env` into [`application.properties`](file:///Users/gauthamsalian/DepLensJSF/src/main/resources/application.properties) on startup.

---

### 5. Running the Application

You can launch the project using either VS Code or the terminal:

#### Option A: One-Click in VS Code
- Open [`src/main/java/com/deplens/DepLensApplication.java`](file:///Users/gauthamsalian/DepLensJSF/src/main/java/com/deplens/DepLensApplication.java).
- Click **Run** above the `main()` method, or press <kbd>F5</kbd>.

#### Option B: Terminal (Maven Wrapper)
```bash
./mvnw spring-boot:run
```

Once started, the backend will bind to **`http://localhost:8080`**.

---

## 🧪 Testing the Authentication & 30-Day Session Flow

1. Open **`http://localhost:8080`** in your browser.
2. Click **"Sign in with GitHub"**:
   - You will be redirected to GitHub to authorize DepLens.
   - Upon granting consent, GitHub redirects to `/login/oauth2/code/github`.
   - The backend creates/updates your record in MySQL `deplens_db.users`.
   - A signed **30-day JWT** is generated and saved to both `localStorage` and a 30-day browser cookie (`deplens_token`).
3. You will see your **GitHub Avatar**, **Name**, **Username**, and a green **"30-Day Session Active"** badge.
4. **Session Persistence Test:**
   - Refresh the page or close and reopen the browser.
   - Your session remains active without prompting for re-login.
5. Click **"Logout"** to clear the JWT token and terminate the session.

*(Note: A **"Dev Quick-Login"** button is also available on the dashboard for instant mock session testing during development.)*

---

## 📂 Project Structure

```text
DepLensJSF/
├── .vscode/
│   ├── extensions.json             # Recommended VS Code extensions
│   ├── launch.json                 # F5 one-click debug configuration
│   └── settings.json               # Java language server settings
├── src/
│   ├── main/
│   │   ├── java/com/deplens/
│   │   │   ├── config/
│   │   │   │   └── SecurityConfig.java            # Spring Security 6 & OAuth2 rules
│   │   │   ├── controller/
│   │   │   │   ├── AuthController.java            # /api/auth/me, logout, dev login
│   │   │   │   └── HealthController.java          # /api/health check
│   │   │   ├── model/
│   │   │   │   └── User.java                      # JPA Entity with GitHub tokens
│   │   │   ├── repository/
│   │   │   │   └── UserRepository.java            # Spring Data JPA repository
│   │   │   ├── security/
│   │   │   │   ├── CustomOAuth2UserService.java   # GitHub profile loader
│   │   │   │   ├── JwtAuthenticationFilter.java   # Bearer & Cookie JWT filter
│   │   │   │   ├── JwtTokenProvider.java          # 30-day token generator/validator
│   │   │   │   ├── OAuth2AuthenticationSuccessHandler.java # Callback & token redirect
│   │   │   │   └── UserPrincipal.java             # Authenticated user representation
│   │   │   └── DepLensApplication.java            # Main application entry point
│   │   └── resources/
│   │       ├── application.properties             # DB, OAuth & JWT configs
│   │       └── static/
│   │           └── index.html                     # Interactive dashboard UI
├── pom.xml                                        # Maven dependencies & plugins
├── mvnw / mvnw.cmd                                # Portable Maven wrapper
└── README.md                                      # Documentation & setup guide
```

---

## 👥 Authors & Team Contributions

- **Gautham Salian** (NNM24CS090)
- **K Smrithi Shenoy** (NNM24CS110)
- **Kanishk Subhash Hegde** (NNM24CS111)
- **Kashvi Suresh** (NNM24CS114)

*Department of Computer Science and Engineering — AY 2026-27*
