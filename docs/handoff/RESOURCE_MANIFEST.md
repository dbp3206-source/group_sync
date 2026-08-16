# KnowledgeOS external/resource manifest

This manifest records the reference material that shaped the migration. The repository and the
handoff documents are the durable source for the receiving agent; large external archives do not
need to be copied into Git.

| Name | Purpose | Current location | Still needed? | Integrated into repo docs? |
|---|---|---|---|---|
| KnowledgeOS product/architecture pack | Product, migration, RAG, design, roadmap, acceptance and Gemini requirements | `C:\Users\Bao Phuc\Downloads\KnowledgeOS_Pack` | Yes for resolving requirement ambiguity; not for routine code changes | Yes, summarized in `docs/handoff/ANTIGRAVITY_HANDOFF.md`, `docs/qa/`, and implementation |
| `01_PRODUCT_SPEC.md` | Product scope and core loop | `C:\Users\Bao Phuc\Downloads\KnowledgeOS_Pack\01_PRODUCT_SPEC.md` | Reference | Yes |
| `02_ARCHITECTURE_MIGRATION.md` | Modular monolith and additive migration rules | same pack folder | Reference | Yes |
| `03_RAG_INTEGRATION_SPEC.md` | RAG flow/scopes/citations/vector direction | same pack folder | Reference | Yes |
| `04_DESIGN_TASTESKILL_SPEC.md` | Anti-generic visual direction | same pack folder | For future UI passes | Yes, plus current CSS/pages |
| `05_IMPLEMENTATION_ROADMAP.md` | Phase ordering | same pack folder | Reference | Yes |
| `06_ACCEPTANCE_CHECKLIST.md` | Acceptance gates | same pack folder | Yes for final acceptance | Partly in `docs/qa/REQUIREMENTS_MATRIX.md` |
| `09_GEMINI_PROVIDER_SPEC.md` | Locked Gemini model/configuration | same pack folder | Yes if provider compatibility changes | Yes |
| Design references | Quality benchmarks, legacy screenshots, anti-goals | `C:\Users\Bao Phuc\Downloads\design_references` | Yes for visual QA | Partly in handoff and implemented UI |
| Original GroupSync source | Working legacy baseline and reusable infrastructure | `C:\Users\Bao Phuc\Documents\GroupSync_Build` | Yes; it is this repository | Yes through code/history/docs |
| `rag-demo` | Reference-only extraction/chunking/embedding/retrieval concepts | `C:\Users\Bao Phuc\Documents\ProntonX\Context Harness\rag-demo` | Optional reference; do not copy architecture | Yes in architecture decisions |
| QA fixtures | Controlled RAG facts and safety cases | repository `qa\fixtures` | Yes for regression | Yes |
| QA matrix/benchmark/journeys/performance | Evidence and known limitations | repository `docs\qa` | Yes | Yes |
| TasteSkill | Primary future frontend art direction | `C:\Users\Bao Phuc\.agents\skills\design-taste-frontend\SKILL.md` | Yes for future redesign work | Direction recorded; invoke skill before UI changes |

**ADD THIS FOLDER TO ANTIGRAVITY PROJECT: YES**

Add the repository root:

`C:\Users\Bao Phuc\Documents\GroupSync_Build`

The external pack and design references may also be added as read-only context if Antigravity
supports multiple project folders:

- `C:\Users\Bao Phuc\Downloads\KnowledgeOS_Pack`
- `C:\Users\Bao Phuc\Downloads\design_references`
- `C:\Users\Bao Phuc\Documents\ProntonX\Context Harness\rag-demo`

Do not add `.env.local`, any secret-bearing file, database dumps, or runtime storage as context.
