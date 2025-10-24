#!/usr/bin/env bash
set -e

source "$(which docker-environment.sh)"
micronaut-startup.sh
