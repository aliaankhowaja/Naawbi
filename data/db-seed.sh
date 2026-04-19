#!/usr/bin/env bash
# Usage: ./db-seed.sh [pg_user] [pg_password] [pg_database]
PG_USER="${1:-postgres}"
PG_PASS="${2:-postgres}"
PG_DB="${3:-naawbi}"

# Find psql — check PATH first, then common Windows install locations
if command -v psql &>/dev/null; then
    PSQL="psql"
elif [ -f "/c/Program Files/PostgreSQL/18/bin/psql.exe" ]; then
    PSQL="/c/Program Files/PostgreSQL/18/bin/psql"
elif [ -f "/c/Program Files/PostgreSQL/17/bin/psql.exe" ]; then
    PSQL="/c/Program Files/PostgreSQL/17/bin/psql"
elif [ -f "/c/Program Files/PostgreSQL/16/bin/psql.exe" ]; then
    PSQL="/c/Program Files/PostgreSQL/16/bin/psql"
else
    echo "ERROR: psql not found. Add it to PATH or install PostgreSQL."
    exit 1
fi

echo "Seeding $PG_DB as $PG_USER ..."
PGPASSWORD="$PG_PASS" "$PSQL" -U "$PG_USER" -d "$PG_DB" -f "$(dirname "$0")/seed.sql"
echo "Done."
