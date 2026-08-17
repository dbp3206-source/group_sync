#!/bin/sh
set -eu

# Render provides DATABASE_URL as postgresql://user:password@host/database.
# PostgreSQL JDBC expects jdbc:postgresql://host/database and receives the
# credentials separately through DB_USERNAME and DB_PASSWORD.
if [ -n "${DATABASE_URL:-}" ]; then
  database_url="${DATABASE_URL#jdbc:}"
  database_url="${database_url#postgresql://}"

  # Keep only the URI authority before removing optional credentials. This
  # prevents the database path from accidentally becoming part of the host.
  database_authority="${database_url%%/*}"
  host_and_port="${database_authority##*@}"

  # Prefer the explicit Blueprint value and retain a safe fallback for other
  # providers that expose only a connection string.
  database_name="${DB_NAME:-${database_url##*/}}"
  database_name="${database_name%%\?*}"
  export DB_URL="jdbc:postgresql://${host_and_port}/${database_name}"
  echo "Starting GroupSync with PostgreSQL host ${host_and_port} and database ${database_name}."
fi

exec java ${JAVA_OPTS:-} -Dserver.port="${PORT:-8080}" -jar /app/app.jar
