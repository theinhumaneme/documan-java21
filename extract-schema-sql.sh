#!/bin/bash
# Refresh schema.sql.
#
# Default: generate it from the JPA mapping. This needs no running database and cannot drift from
# the entities, which is what left the previous snapshot stale.
#
#   ./extract-schema-sql.sh
#
# Pass --from-database to dump a live instance instead, e.g. to capture manual changes that were
# applied outside the mapping.
#
#   ./extract-schema-sql.sh --from-database
set -euo pipefail

if [[ "${1:-}" == "--from-database" ]]; then
    docker exec postgres pg_dump -U root -s postgres > schema.sql
    echo "schema.sql dumped from the running postgres container"
    exit 0
fi

mvn -B -q test -Dtest=SchemaExportTest
cp target/schema-postgres.sql schema.sql
echo "schema.sql generated from the JPA mapping"
