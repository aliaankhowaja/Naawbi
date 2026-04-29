#!/usr/bin/env bash
# Ensure the dedicated naawbi_test database exists. Idempotent.
# Schema is created by DB.createTables() at test startup, so this script
# only handles the one thing psql is required for: CREATE DATABASE.
#
# Usage: ./db-test-init.sh [pg_user] [pg_password] [pg_database]

PG_USER="${1:-postgres}"
PG_PASS="${2:-postgres}"
TEST_DB="${3:-naawbi_test}"

# Find psql — same lookup pattern as db-seed.sh
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

# Check whether the database already exists; only create if missing.
EXISTS=$(PGPASSWORD="$PG_PASS" "$PSQL" -U "$PG_USER" -d postgres -tAc \
    "SELECT 1 FROM pg_database WHERE datname = '$TEST_DB'")

if [ "$EXISTS" = "1" ]; then
    echo "Test DB '$TEST_DB' already exists — skipping CREATE."
else
    echo "Creating test DB '$TEST_DB' ..."
    PGPASSWORD="$PG_PASS" "$PSQL" -U "$PG_USER" -d postgres \
        -c "CREATE DATABASE $TEST_DB;"
    echo "Done."
fi
