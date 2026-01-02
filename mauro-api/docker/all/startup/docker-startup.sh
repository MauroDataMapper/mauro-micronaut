#!/usr/bin/env bash
set -e

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
source "$(which micronaut-config-for-postgres.sh)"
gosu postgres postgres-startup.sh
trap cleanup SIGTERM SIGINT
micronaut-startup.sh
