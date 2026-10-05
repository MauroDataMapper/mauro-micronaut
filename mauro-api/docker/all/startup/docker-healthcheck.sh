#!/usr/bin/env bash

if [ -e /tmp/database-maintenance ]; then
  PG_ISREADY="$(find /usr/lib/postgresql -type f -name pg_isready -print -quit)"
  if [ -z "${PG_ISREADY}" ]; then
    echo "pg_isready not found"
    exit 1
  fi
  "${PG_ISREADY}" -h localhost -p "${POSTGRES_PORT:-5432}" -q
  exit $?
else
  curl -fsS http://localhost:8080/health
  exit $?
fi
