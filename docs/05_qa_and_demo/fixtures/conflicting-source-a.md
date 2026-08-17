# Database Performance Policy (Legacy 2024)

Standard configuration recommendations for PostgreSQL database connection pooling.

## Connection Limits

- **Maximum Connections**: The global database connection pool is strictly capped at `100` active client connections.
- **Idle Timeout**: Unused pooled connections terminate after `30000` milliseconds.
- **Leak Detection Threshold**: Queries executing longer than `5000` milliseconds trigger warning logs.
