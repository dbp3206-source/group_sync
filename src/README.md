# KnowledgeOS — Application Source Code

This directory contains all executable production source code, build scripts, and local developer utilities.

---

## 📁 Source Subsystems

### 1. [`backend/`](file:///C:/Users/Bao%20Phuc/Documents/GroupSync_Build/src/backend) — Spring Boot 4 REST API Service
- Java 21, Spring Boot 4.1.0, Spring Data JPA, Spring Security, Flyway migrations (V1–V13), `pgvector` vector retrieval, and Google Gemini integration.
- Run locally with:
  ```bash
  cd src/backend
  ./mvnw spring-boot:run
  ```

### 2. [`frontend/`](file:///C:/Users/Bao%20Phuc/Documents/GroupSync_Build/src/frontend) — React 19 Single Page Application
- React 19, TypeScript 5.8, Vite 8, React Router, Design Tokens (`--gs-*` and `--kos-*`), self-hosted `Outfit` typography, and WCAG accessibility.
- Run locally with:
  ```bash
  cd src/frontend
  npm install
  npm run dev
  ```

### 3. [`scripts/`](file:///C:/Users/Bao%20Phuc/Documents/GroupSync_Build/src/scripts) — Developer & Automation Scripts
- Helper scripts for PDF guide generation, browser smoke tests, and local runtime management.
