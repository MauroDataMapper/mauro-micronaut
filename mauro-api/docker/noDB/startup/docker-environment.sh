#!/usr/bin/env bash
set -euo pipefail

# The actual maximum memory allowed is controlled by the
# docker engine VM's limit

if [ -f /sys/fs/cgroup/memory.max ];
then
    MEM_LIMIT=$(cat /sys/fs/cgroup/memory.max)
elif [ -f /sys/fs/cgroup/memory/memory.limit_in_bytes ]; then
    MEM_LIMIT=$(cat /sys/fs/cgroup/memory/memory.limit_in_bytes)
else
    MEM_LIMIT="max"
fi

if [ "$MEM_LIMIT" = "max" ];
then
    MEM_LIMIT_KB=$(grep MemTotal /proc/meminfo | awk '{print $2}')
    MEM_LIMIT=$((MEM_LIMIT_KB * 1024))
fi

export MEMORY_AVAILABLE_GB=$(( ( MEM_LIMIT + (1024 * 1024 * 1024) - 1 ) / 1024 / 1024 / 1024 ))
echo "Detected memory limit: ${MEMORY_AVAILABLE_GB}GB"
export CPU_COUNT=$(nproc --all)
echo "Detected ${CPU_COUNT} cores"

INET=$(ip -o -f inet addr show eth0)
CONTAINER_IP=$(echo "${INET}" | awk '{print $4}' | cut -d/ -f1)
PREFIX=$(echo "${INET}" | awk '{print $4}' | cut -d/ -f2)
IFS=. read -r a b c d <<< "$CONTAINER_IP"
export DOCKER_SUBNET="$((a & 255)).$((b & 255)).0.0/$PREFIX"

echo "Docker subnet ${DOCKER_SUBNET}"
