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

export DOCKER_SUBNET="$(ip -o -4 addr show 2>/dev/null | awk '/scope global/ {split($4,a,"/");split(a[1],b,".");printf "%d.%d.%d.0/%s\n",b[1],b[2],b[3],a[2];exit}')"

echo "Docker subnet ${DOCKER_SUBNET}"

export DOCKER_LOCAL_ADDRESSES="localhost ::1 $(ip -o -4 addr show | awk ' {split($4,a,"/"); printf "%s ",a[1]} ')"
export DOCKER_CONTAINER_NETWORK_PREFIX="$(ip -o -4 addr show 2>/dev/null | awk '/scope global/ {split($4,a,"/");split(a[1],b,".");printf "%d.%d.%d.*",b[1],b[2],b[3];exit}')"

export DOCKER_BRIDGE_ADDRESSES="host.docker.internal ${DOCKER_CONTAINER_NETWORK_PREFIX}"

echo "Docker internal addresses ${DOCKER_LOCAL_ADDRESSES}"
echo "Docker bridge addresses ${DOCKER_BRIDGE_ADDRESSES}"
