#!/usr/bin/env bash
set -e

cleanup()
{
  su postgres -c postgres-shutdown.sh
}

source "$(which docker-environment.sh)"
source "$(which micronaut-config-for-postgres.sh)"
su postgres -c postgres-startup.sh
trap cleanup SIGTERM SIGINT
micronaut-startup.sh
