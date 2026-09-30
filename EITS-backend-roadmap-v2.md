# Enterprise Issue Tracking System (EITS) — Backend Edition
### Requirements & Roadmap v2 — Java / Spring Boot, API-First
*(From Product Owner to Backend Developer)*

Since your goal is **Java Backend Developer**, this version drops the frontend entirely. Every feature is validated through **Swagger UI** and **Postman**. Frontend is now optional, future work — not part of this roadmap.

---

## 1. Functional Requirements (unchanged, API-first)

| # | Requirement |
|---|---|
| F1 | Auth: register, login, refresh token, logout |
| F2 | Roles: Admin, Project Manager, Developer, Reporter |
| F3 | Project & Project Member management |
| F4 | Issue CRUD (type, priority, status, labels, assignee, due date, attachments) |
| F5 | Configurable workflow (Open → In Progress → In Review → Done) |
| F6 | Comments & full activity history |
| F7 | Notifications (in-app; email optional later) |
| F8 | Search/filter (status, assignee, label, priority, project) with pagination & sorting |
| F9 | Dashboard endpoints (issues by status/assignee, simple burndown data) |
| F10 | Audit log for key actions |

All of the above are exposed **only as REST APIs**, documented via OpenAPI/Swagger, and tested via Postman collections.

---

## 2. Backend Architecture

Enterprise-style layered package structure (per module or globally — your choice, but be consistent):

```
com.eits
├── controller/     # REST endpoints only — no business logic
├── dto/
│   ├── request/
│   └── response/
├── service/
│   ├── impl/
├── repository/     # Spring Data JPA interfaces
├── entity/         # JPA entities
├── mapper/         # Entity <-> DTO (MapStruct recommended)
├── exception/      # Custom exceptions + GlobalExceptionHandler
├── security/       # JWT filter, UserDetailsService, SecurityConfig
├── config/         # Swagger, CORS, Beans, Redis, etc.
├── validation/     # Custom annotations/validators
├── util/           # Stateless helpers
└── common/         # Shared constants, enums, base response wrapper
```

**Why each package exists:**
- **controller** — thin, only handles HTTP concerns (request/response, status codes). No logic.
- **dto** — never expose entities directly; request/response split prevents over-posting and keeps API contracts stable even if entities change.
- **service** — business logic lives here; interface + impl separation supports testing and future swap-ability.
- **repository** — pure data access, no business rules.
- **entity** — DB representation only, isolated from API layer.
- **mapper** — keeps entity↔DTO conversion out of services (cleaner, testable).
- **exception** — centralizes error handling so every endpoint returns consistent error shapes.
- **security** — isolates auth/authorization concerns from business code.
- **config** — all cross-cutting setup in one place, easy to find.
- **validation** — reusable custom validation logic beyond basic annotations.
- **util/common** — shared code without becoming a dumping ground (keep these disciplined and small).

---

## 3. Database Design

Design a full ERD before writing any code. Required entities and relationships:

- **users** (id, email, password_hash, full_name, status, created_at, ...)
- **roles** (id, name) + **user_roles** (many-to-many)
- **projects** (id, key, name, description, owner_id)
- **project_members** (project_id, user_id, role_in_project) — many-to-many with attributes
- **issues** (id, project_id, title, description, type, status, priority, assignee_id, reporter_id, due_date, created_at, updated_at)
- **comments** (id, issue_id, author_id, body, created_at)
- **attachments** (id, issue_id, file_url, uploaded_by, uploaded_at)
- **labels** (id, name, color) + **issue_labels** (many-to-many)
- **activity_logs** (id, entity_type, entity_id, action, actor_id, timestamp, metadata)
- **notifications** (id, user_id, message, read_status, created_at)

**You must define, for each table:**
- Primary/foreign keys, cascade rules (e.g., deleting a project → soft-delete or restrict on issues?)
- Indexes (especially on foreign keys and frequently filtered columns like `status`, `assignee_id`)
- Normalization decisions — justify any deliberate denormalization
- Soft-delete vs hard-delete strategy (recommended: soft-delete for issues/projects, hard-delete for sessions/tokens)

Deliverable: an ERD diagram + a short doc explaining relationship choices, before Phase 1 begins.

---

## 4. API Design (Contract-First)

Before implementation, produce a complete API specification per resource:

For **each** endpoint, define:
- HTTP method + path (e.g., `POST /api/v1/projects/{projectId}/issues`)
- Request DTO (fields, types, required/optional)
- Response DTO
- Status codes (200/201/204/400/401/403/404/409/422/500)
- Auth requirement (role needed)
- Validation rules
- Pagination/sorting/filtering params where relevant (`page`, `size`, `sort`, `status`, `assignee`, etc.)

This becomes your Swagger/OpenAPI spec and your Postman collection — build both from the same contract so they never drift apart.

---

## 5. Spring Boot Concepts — Learn-As-You-Go Map

**Beginner (Phase 0–1):**
Spring Boot project structure, Spring MVC (`@RestController`, `@RequestMapping`), Spring Data JPA basics, `application.yml` profiles, Bean Validation basics, DTOs.

**Intermediate (Phase 2–4):**
Spring Security (JWT), custom exception handling (`@ControllerAdvice`), MapStruct, pagination/sorting (`Pageable`), transactions (`@Transactional`), relationships in JPA (lazy/eager), testing (`@SpringBootTest`, `@WebMvcTest`, `@DataJpaTest`), profiles for dev/staging/prod.

**Advanced (Phase 5+):**
Caching (Spring Cache + Redis), async processing (`@Async`, events), Testcontainers, actuator + custom health indicators/metrics, rate limiting, connection pool tuning (HikariCP), N+1 query detection, database migrations (Flyway/Liquibase).

---

## 6. Git Workflow

- **Branching:** `main` (prod-ready), `develop` (integration), `feature/*`, `bugfix/*`, `hotfix/*`
- **Commits:** Conventional Commits (`feat:`, `fix:`, `refactor:`, `test:`, `docs:`, `chore:`)
- **PRs:** must reference an issue, pass CI, get at least a self-review checklist before merge
- **Releases:** tag with Semantic Versioning (`v1.2.0`), changelog per release
- **GitHub Issues:** use for tracking your own backlog — ironically, dogfood your own issue-tracking concept

---

## 7. Validation Strategy

- Bean Validation annotations (`@NotNull`, `@Size`, `@Email`, etc.) on request DTOs
- Custom validators for domain rules (e.g., valid status transitions)
- Global exception handler (`@ControllerAdvice`) catching validation, business, and unexpected errors
- Standardized error response shape — adopt **RFC 7807 (Problem Details)** or a consistent custom format (`timestamp`, `status`, `error`, `message`, `path`, `errors[]`)
- Input sanitization to prevent injection in free-text fields (descriptions, comments)

---

## 8. Security

- JWT access token + refresh token flow
- Password hashing with BCrypt
- Account lockout after repeated failed logins
- Email verification on registration (can stub the email send initially)
- Forgot password / reset flow
- CORS configuration (restrict origins per environment)
- CSRF: explain why it's typically not needed for stateless JWT APIs, but document the decision
- Rate limiting on auth endpoints (e.g., Bucket4j)
- Method-level authorization checks (`@PreAuthorize`) matching each role

---

## 9. Testing Roadmap

- **Unit tests** — services, mappers, validators (JUnit 5 + Mockito)
- **Repository tests** — `@DataJpaTest` against an in-memory or Testcontainers Postgres
- **Controller tests** — `@WebMvcTest` with mocked service layer
- **Integration tests** — full context with Testcontainers (real Postgres in a container)
- **Code coverage** — target a meaningful threshold (e.g., 70-80%), tracked via JaCoCo
- **API testing** — Postman collection with scripted assertions, run manually and eventually in CI (Newman)

---

## 10. Performance

- Pagination on all list endpoints (never return unbounded lists)
- Indexing on filtered/joined columns
- Avoid N+1 queries — use `JOIN FETCH` / entity graphs where needed, verify with SQL logging
- Lazy vs eager loading decisions per relationship, explicitly justified
- Redis caching for expensive/read-heavy endpoints (e.g., dashboard aggregates)
- HikariCP connection pool tuning for expected load

---

## 11. Logging Strategy (before centralized logging)

- Structured logging via SLF4J + Logback (JSON format recommended for later aggregation)
- Log levels used consistently (ERROR/WARN/INFO/DEBUG) — no `System.out.println`
- Request/correlation IDs propagated through each request for traceability
- Exception logging with enough context to debug without reproducing locally
- Log rotation configured (size/time-based)

---

## 12. Documentation Standards

- OpenAPI/Swagger UI live and accurate at all times
- Architecture Decision Records (ADRs) for major choices (e.g., "Why Postgres over MySQL", "Why JWT over sessions")
- Sequence diagrams for key flows (auth, issue creation, notification dispatch)
- Deployment guide (how to deploy to each environment)
- Troubleshooting guide (common errors and fixes)
- CONTRIBUTING.md if you ever open this up

---

## 13. Coding Standards

- Package naming: lowercase, singular where reasonable (`entity`, not `entities`)
- DTOs: `CreateIssueRequest`, `IssueResponse`, `UpdateIssueRequest` — verb/purpose explicit
- Services: `IssueService` (interface) / `IssueServiceImpl`
- Repositories: `IssueRepository`
- REST endpoints: plural nouns, versioned (`/api/v1/issues`), consistent casing
- Exceptions: `ResourceNotFoundException`, `InvalidStatusTransitionException`, etc. — descriptive, not generic
- Code review checklist per PR: architecture fit, naming, test coverage, security implications, no dead code

---

## 14. Revised DevOps Phases (Kubernetes moved later)

Reasoning: deploying via Docker Compose to a real server first teaches you deployment fundamentals — networking, environment config, process management, basic monitoring — without the added complexity of an orchestrator. Kubernetes is much easier to learn once you've operated a containerized app in production at least once.

```
Docker
  ↓
CI (build, test, lint, image build)
  ↓
Deploy via Docker Compose to a VPS
  ↓
Monitoring & Logging (Prometheus, Grafana, structured logs)
  ↓
Infrastructure as Code (Terraform)
  ↓
Kubernetes migration
```

---

## 15. Full Roadmap (Backend-Only, API-First)

### Phase 0 — Planning & Design
- Finalize stack: Spring Boot, PostgreSQL, JWT, Docker
- ERD (Section 3) + API contract (Section 4)
- Repo setup with Git workflow (Section 6)

**Acceptance criteria:** ERD reviewed and approved, API spec covers all F1–F10 endpoints, repo initialized with branch protection.

### Phase 1 — Core Domain APIs
- Implement auth (F1), users/roles (F2), projects (F3)
- Layered architecture per Section 2
- Swagger UI live and matches contract

**Acceptance criteria:** Can register, log in, create a project, and add a member — all verified in Swagger and Postman. Unit tests passing for services.

### Phase 2 — Issue Management APIs
- Issues, comments, labels, attachments (F4–F6)
- Validation strategy applied (Section 7)
- Global exception handling live

**Acceptance criteria:** Full issue lifecycle (create → update status → comment → close) testable via Postman collection with assertions.

### Phase 3 — Search, Notifications, Dashboard
- Filtering/pagination/sorting (F8), notifications (F7), dashboard endpoints (F9), audit log (F10)

**Acceptance criteria:** All list endpoints paginated; dashboard returns correct aggregates against seeded test data.

### Phase 4 — Testing
- Full testing pyramid per Section 9
- JaCoCo coverage report generated

**Acceptance criteria:** Coverage threshold met; Postman collection runs clean end-to-end.

### Phase 5 — Containerization
- Dockerfile (multi-stage build), docker-compose.yml (app + Postgres + Redis)

**Acceptance criteria:** `docker-compose up` gives a fully working API with one command, documented in README.

### Phase 6 — CI Pipeline
- GitHub Actions: lint → test → build image → (optionally) push to a registry

**Acceptance criteria:** Every PR triggers pipeline; broken tests block merge.

### Phase 7 — Deploy to a VPS via Docker Compose
- Provision a small VPS, deploy manually first, then scripted
- Environment-specific configs (dev/staging/prod), secrets via `.env` (not committed)

**Acceptance criteria:** API reachable over HTTPS on a real server; deployment steps documented in the deployment guide.

### Phase 8 — Monitoring & Logging
- Spring Boot Actuator + Prometheus + Grafana dashboard
- Centralized logs (e.g., Loki or ELK)

**Acceptance criteria:** Dashboard shows live request metrics; a simulated failure is visible in logs and alerts.

### Phase 9 — Infrastructure as Code
- Terraform scripts to provision the same environment currently set up manually

**Acceptance criteria:** Environment can be destroyed and recreated from Terraform alone.

### Phase 10 — Kubernetes Migration
- Move from Compose to Kubernetes (start local via Minikube/kind)
- Health checks, readiness/liveness probes, ConfigMaps/Secrets

**Acceptance criteria:** App runs on Kubernetes with the same functionality; a pod crash self-heals without data loss.

### Phase 11 — Security Hardening & Final Review
- Full Section 8 checklist implemented
- Senior-engineer-style review across the whole codebase (Section 16)

**Acceptance criteria:** No hardcoded secrets, all endpoints enforce correct role checks, review checklist passed.

*(Frontend, if you choose to add it later, becomes Phase 12+ — entirely optional and out of scope for this roadmap.)*

---

## 16. Ongoing Review Process

At the end of every phase, before moving on, bring me your work and I'll review it as a senior backend engineer would — covering architecture, readability, maintainability, performance, security, and testing, and flagging the kind of mistakes a junior dev typically makes versus what's expected at a senior level.

---

## How We'll Work Together

- Tell me which phase you're on and what you've built — I'll review it like a stakeholder/senior engineer would
- Ask me to expand any section above in more depth before you start it
- I won't write code, but I will clarify requirements, review designs, and push back where something's underspecified
