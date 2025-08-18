#!/usr/bin/env bash
set -e

# Start Postgres in background
echo "Starting Postgres... from ${DATABASE_DIRECTORY}"
PG_BIN="$(find /usr/lib/postgresql -name 'bin')"
export PATH="${PG_BIN}:${PATH}"

if [ ! -s "${DATABASE_DIRECTORY}/PG_VERSION" ];
then
    echo "Initializing Postgres database..."
    initdb -D "${DATABASE_DIRECTORY}"

    # Start up and create the default database

    pg_ctl -D "${DATABASE_DIRECTORY}" -l "${DATABASE_LOGS_DIRECTORY}/logfile" -o "-c listen_addresses='*'" -w start

    # create user and database
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
        CREATE USER $DATABASE_USERNAME with SUPERUSER PASSWORD '$DATABASE_PASSWORD';
        CREATE DATABASE ${DATABASE_NAME} OWNER $DATABASE_USERNAME;
EOSQL

  if [ -e /opt/init/postgres ];
  then
    pushd /opt/init/postgres

      for f in $(ls)
      do
        case "${f}" in
          *.sh)
              echo "Running ${f}"
              if [ -x "${f}" ];
              then
                  "${f}"
              else
                  . "${f}"
              fi
            ;;
          *.sql)
              echo "Running ${f}"
              psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" -f "${f}"
            ;;
        esac
      done
    popd
  else
      echo "No /opt/init/postgres *.sh *.sql - skipping"
  fi

  pg_ctl -D ${DATABASE_DIRECTORY} -w stop
fi

# Write some defaults

    usable_gb=$(( MEMORY_AVAILABLE_GB - 2 ))
    if (( usable_gb < 1 )); then
        usable_gb=1
    fi

    pg_gb=$(( usable_gb / 2 ))
    if (( pg_gb < 1 )); then
        pg_gb=1
    fi

    pg_mb=$(( pg_gb * 1024 ))

    max_worker_processes=$(( CPU_COUNT ))
    parallel_workers=$(( max_worker_processes / 2 ))

    work_mem=$(( pg_mb / 160 ))
    maintenance_work_mem=$(( work_mem * max_worker_processes * 2 ))

    shared_buffers=$(( pg_mb / 4 ))
    effective_cache_size=$(( MEMORY_AVAILABLE_GB * 1024 / 2 ))

    remaining_mem=$(( pg_mb - shared_buffers ))
    work_mem_three=$(( 3 * work_mem ))
    max_connections=$(( 4 + ( remaining_mem / work_mem_three) - max_worker_processes ))

    cat <<EOF > "${DATABASE_DIRECTORY}/postgresql.auto.conf"
# Auto-tuned settings
seq_page_cost = 1.0
random_page_cost = 1.1
huge_pages = try
wal_buffers = 16MB
min_wal_size = 4GB
max_wal_size = 16GB

shared_buffers = ${shared_buffers}MB
effective_cache_size = ${effective_cache_size}MB
maintenance_work_mem = ${maintenance_work_mem}MB
work_mem = ${work_mem}MB

max_worker_processes = ${max_worker_processes}
max_parallel_workers = ${max_worker_processes}
max_parallel_workers_per_gather = ${parallel_workers}
max_parallel_maintenance_workers = ${parallel_workers}

max_connections = ${max_connections}

EOF

chown postgres:postgres "${DATABASE_DIRECTORY}/postgresql.auto.conf"

# Check that the docker container has access

if [ -e "${DATABASE_DIRECTORY}/pg_hba.conf" ];
then
  echo grep "${DOCKER_SUBNET}" "${DATABASE_DIRECTORY}/pg_hba.conf"
  ALLOWED=$(grep "${DOCKER_SUBNET}" "${DATABASE_DIRECTORY}/pg_hba.conf" || true)
  echo ${ALLOWED}

  if [ "${ALLOWED}" == "" ];
  then
    echo "Adding Docker ${DOCKER_SUBNET} as a trusted network for postgres"
    echo "host ${DATABASE_NAME} ${DATABASE_USERNAME} ${DOCKER_SUBNET} trust" >> "${DATABASE_DIRECTORY}/pg_hba.conf"
  fi
fi

pg_ctl -D "${DATABASE_DIRECTORY}" -l "${DATABASE_LOGS_DIRECTORY}/logfile" -o "-c listen_addresses='*'" -w start
