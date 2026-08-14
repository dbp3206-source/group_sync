#!/bin/sh
set -eu

# Render provides DATABASE_URL as postgresql://user:password@host/database.
# PostgreSQL JDBC expects credentials separately, which Render also provides
# through DB_USERNAME and DB_PASSWORD.
if [ -n "${DATABASE_URL:-}" ]; then
  database_url="${DATABASE_URL#jdbc:}"
  database_url="${database_url#postgresql://}"
  case "$database_url" in
    *@*) host_and_port="${database_url#*@}" ;;
    *) host_and_port="${database_url%%/*}" ;;
  esac
  # Keep only the final database segment in case the provider connection
  # string already contains an extra database path segment.
  database_name="${database_url##*/}"
  database_name="${database_name%%\?*}"
  export DB_URL="jdbc:postgresql://${host_and_port}/${database_name}"
fi

exec java ${JAVA_OPTS:-} -Dserver.port="${PORT:-8080}" -jar /app/app.jar
