# Security and Protocol Notes

## CVE and Vulnerability Reference

The following vulnerability has been catalogued in this research:

**CVE-2026-12345** is a critical remote code execution vulnerability affecting web servers running unpatched middleware. Affected versions: 2.1.0 through 2.4.7. The vulnerability allows unauthenticated attackers to execute arbitrary code via a crafted HTTP request header.

Recommended remediation: upgrade to version 2.5.0 or later. Hotfix patches are available for 2.1.x and 2.3.x branches.

## HTTP Standards Reference

**RFC 9110** defines the semantics of the Hypertext Transfer Protocol (HTTP). It obsoletes RFC 7231, RFC 7232, RFC 7233, RFC 7235, and RFC 7238. RFC 9110 specifies the meaning of HTTP request methods, status codes, header fields, and content negotiation. It is foundational for any web application implementing REST APIs.

Key sections:
- Section 4: Methods (GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS)
- Section 15: Status Codes
- Section 8: Representations and Content Negotiation

## Vector Search and Semantic Retrieval

Vector search operates by finding nearest-neighbor matches in a high-dimensional embedding space. Documents and queries are converted into dense vectors (embeddings) by a language model. The similarity between query and document embeddings is computed using cosine distance or dot product. The documents with the smallest distance (highest similarity) are returned as retrieval candidates.

This technique is known as **Approximate Nearest Neighbor (ANN)** search and is implemented in systems such as pgvector, FAISS, and Pinecone.

The key advantage over keyword search is that vector search captures **semantic similarity** — queries and documents can match even when they use different words to express the same concept.

## Notes on Hybrid Retrieval

Combining vector search with lexical/full-text search addresses the weakness of each approach:
- Pure semantic search may miss exact technical identifiers like CVE-2026-12345 or RFC 9110.
- Pure lexical search may miss paraphrase queries where the question uses different vocabulary than the document.
- Hybrid search using Reciprocal Rank Fusion (RRF) combines both ranked lists to surface the best candidates from either branch.
