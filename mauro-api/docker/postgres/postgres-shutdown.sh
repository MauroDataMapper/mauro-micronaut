#!/usr/bin/env bash
set -e

if [ -s "${DATABASE_DIRECTORY}/PG_VERSION" ];
then
  PG_BIN="$(find /usr/lib/postgresql -name 'bin')"
  export PATH="${PG_BIN}:${PATH}"
  echo "Shutting down Postgres..."
  pg_ctl -D "${DATABASE_DIRECTORY}" stop -m fast -w
fi
