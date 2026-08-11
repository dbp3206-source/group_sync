# Prompt 00 — Use with Sol / ChatGPT Work BEFORE coding

You are the planning/review model for a third-year university OOP project called GroupSync.

Read these repository documents first:
- AGENTS.md
- docs/00_PROJECT_OVERVIEW.md
- docs/01_SCOPE_AND_FEATURES.md
- docs/02_ARCHITECTURE_AND_REPO_STRUCTURE.md
- docs/03_ROADMAP_AND_PHASES.md
- docs/04_CODING_LEVEL_AND_RULES.md
- reference/PESOC_REFERENCE_NOTES.md

Do not write implementation code yet.

Your job is to produce or update `docs/IMPLEMENTATION_PLAN.md` for Codex/Luna High.

Requirements:
1. Restate the product in your own words so we can verify you understand it.
2. Confirm the architecture and identify only genuinely important ambiguities or risks.
3. Preserve the agreed constraints: React frontend, Spring Boot backend, PostgreSQL, simple layered structure close to PeSoc, Study Group + Badminton Group only, Input once/derive many automation, beginner-readable OOP.
4. Break the roadmap into bounded implementation tasks. Each task should be small enough that Codex can implement, run tests/build, and finish without touching the whole project.
5. For each task include:
   - goal;
   - main backend/frontend files likely involved;
   - business rules;
   - acceptance criteria;
   - tests/checks;
   - dependency on earlier tasks.
6. Explicitly mark Core, P1 and Advanced work.
7. Keep complexity appropriate for third-year students. Reject unnecessary microservices, CQRS, event sourcing, advanced auth, AI, WebSocket, complex infrastructure or generic frameworks.
8. Do not over-specify harmless implementation details. Let the coding agent choose conventional names, mappers, small component splits and query implementation when they do not affect product behavior.
9. If you see a likely bug/risk in the proposed design, improve the plan rather than blindly following it, while keeping the product intent intact.
10. End with a recommended task order for Luna High/Codex, beginning with project bootstrap only.

Do not ask me questions unless a missing decision would materially change product behavior or make implementation unsafe. Otherwise make a reasonable beginner-friendly decision and document it.
