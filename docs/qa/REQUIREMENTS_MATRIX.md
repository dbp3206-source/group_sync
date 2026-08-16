# KnowledgeOS requirements matrix

This matrix records the current implementation evidence for the migration branch.

| Area | Evidence | Status |
|---|---|---|
| Product loop | Home, Library, Workspace, Ask, Focus, Insights | PASS |
| Resource ownership | Resource, note, collection, tag, relation queries validate owner | PASS |
| Title search | `GET /api/resources?q=` and Library UI | PASS |
| Vietnamese title search | UTF-8 title search exercised in Library | PASS |
| Tag CRUD and filtering | `/api/tags`, resource tag endpoints, Library filter | PASS |
| Collection CRUD and filtering | `/api/collections`, combined Library filter | PASS |
| Combined filters | title + tag + collection AND query | PASS |
| Smart tags | normalized, limited, user-reviewed suggestions | PASS |
| Smart collections | existing match or reviewable new collection suggestion | PASS |
| Related suggestions | pgvector-backed, self/owner filtering, confirmation before relation | PASS |
| Duplicate policy | checksum conflict blocks exact duplicate; semantic matches remain warnings/suggestions | PASS |
| Resource workspace | overview, reader, notes, related, activity, organize | PASS |
| Persistent chat | session list/detail, messages/citations reload | PASS |
| Four Ask scopes | THIS_RESOURCE, SELECTED_RESOURCES, COLLECTION, LIBRARY | PASS |
| Insufficient context | weak evidence gate and explicit fallback | PASS |
| Prompt injection | untrusted evidence delimiters and no secret/citation fabrication policy | PASS |
| Vietnamese RAG | live controlled case | PASS |
| RAG benchmark | 25 configured cases, 5 live cases | PASS |
| Backend regression | 43 tests pass in full local suite; live Neon/Gemini smoke passes | PASS |
| Frontend build | TypeScript and Vite production build | PASS |
| Responsive | 1440, 1024, 768, 390 widths previously verified without overflow | PASS |
| Database safety | V1-V8 unchanged; V9-V11 validate on Neon | PASS |
| Production safety | no production datasource change; no secrets committed | PASS |

Items intentionally outside this checkpoint remain documented in the architecture plan, including repository rename and production deployment.
