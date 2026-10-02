#!/usr/bin/env bash

if [ -e /tmp/database-maintenance ]; then
  PG_ISREADY="$(find /usr/lib/postgresql -type f -name pg_isready -print -quit)"
  exec "${PG_ISREADY}" -h localhost -p "${POSTGRES_PORT:-5432}" -q
fi

exec curl -fsS http://localhost:8080/health
