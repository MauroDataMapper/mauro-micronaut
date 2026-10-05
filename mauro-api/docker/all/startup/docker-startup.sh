#!/usr/bin/env bash
set -e

rm -f /tmp/database-maintenance
DATABASE_MAINTENANCE="false"
for arg in "$@"; do
  case "${arg}" in
    --database-maintenance)
      DATABASE_MAINTENANCE="true"
      touch /tmp/database-maintenance
      ;;
    *)
      echo "ERROR: Unsupported startup option: ${arg}"
      exit 2
      ;;
  esac
done

if ! capsh --print | awk '/^Current:/ {print}' | grep -q cap_net_bind_service; then
  echo "ERROR: CAP_NET_BIND_SERVICE is required"
  echo
  capsh --print
  exit 1
fi

if ! capsh --print | awk '/^Current:/ {print}' | grep -q cap_setuid; then
  echo "ERROR: CAP_SETUID is required"
  echo
  capsh --print
  exit 1
fi

if ! capsh --print | awk '/^Current:/ {print}' | grep -q cap_setgid; then
  echo "ERROR: CAP_SETGID is required"
  echo
  capsh --print
  exit 1
fi

if ! capsh --print | awk '/^Current:/ {print}' | grep -q cap_chown; then
  echo "ERROR: CAP_CHOWN is required"
  echo
  capsh --print
  exit 1
fi

cleanup()
{
  gosu postgres postgres-shutdown.sh
}

source "$(which docker-environment.sh)"
source "$(which micronaut-config.sh)"
source "$(which micronaut-config-for-postgres.sh)"
if [ "${DATABASE_MAINTENANCE}" == "true" ] && [ "${PG_RUNNING_INTERNALLY}" == "false" ]; then
  echo "ERROR: Database maintenance mode requires the internal Postgres database"
  exit 1
fi

gosu postgres postgres-startup.sh
trap cleanup SIGTERM SIGINT

if [ "${DATABASE_MAINTENANCE}" == "true" ]; then
  shutdown_for_maintenance()
  {
    cleanup
    exit 0
  }

  trap shutdown_for_maintenance SIGTERM SIGINT
  PG_CTL="$(find /usr/lib/postgresql -type f -name pg_ctl -print -quit)"
  echo "Database maintenance mode: waiting for Postgres to stop..."
  while gosu postgres "${PG_CTL}" -D "${DATABASE_DIRECTORY}" status > /dev/null 2>&1; do
    sleep 1
  done
  echo "Postgres stopped; exiting database maintenance mode."
  exit 1
fi

micronaut-startup.sh
