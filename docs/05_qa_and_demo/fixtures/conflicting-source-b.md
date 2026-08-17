# Database Performance Policy (Updated 2026)

Updated enterprise configuration recommendations for PostgreSQL database connection pooling.

## Connection Limits (Revised)

- **Maximum Connections**: Following the cluster hardware upgrade, the global connection pool limit has been increased from `100` to `500` active client connections.
- **Idle Timeout**: Retained at `30000` milliseconds.
- **Leak Detection Threshold**: Tightened to `3000` milliseconds for high-throughput microservices.
