#!/usr/bin/env bash
set -e

# Function to get a value from a yaml property
function yaml_val {
   local prop=$2
   local s='[[:space:]]*' w='[a-zA-Z0-9_]*' fs=$(echo @|tr @ '\034')
   sed -ne "s|^\($s\):|\1|" \
        -e "s|^\($s\)\($w\)$s:$s[\"']\(.*\)[\"']$s\$|\1$fs\2$fs\3|p" \
        -e "s|^\($s\)\($w\)$s:$s\(.*\)$s\$|\1$fs\2$fs\3|p"  $1 |
   awk -F$fs -v prop="$prop" '{
    cur_indent = length($1)
    cur_key    = $2
    cur_value  = $3
        if (NR == 1) {
      level = 0
      indent[0] = cur_indent
    }

    if (cur_indent > indent[level]) {
      level++
      indent[level] = cur_indent
    }
    else if (cur_indent < indent[level]) {
      while (level > 0 && indent[level] != cur_indent) {
        delete indent[level]
        delete key[level]
        level--
      }
    }

    key[level] = cur_key

    if (length(cur_value) > 0) {
      path = ""
      for (i = 0; i < level; i++) {
        path = path key[i] "_"
      }
      full = path cur_key

      if (full == prop) {
        print cur_value
        exit 0
      }
    }
   }'
}

if [ -e /home/app/resources ];
  then
      if [[ -v DATABASE_NAME ]];
      then
        echo "Using environment variable settings for Postgres"
      else
        echo "Reading Micronaut configuration for Postgres..."
        datasources_default_url=""
        datasources_default_username=""
        datasources_default_password=""

        if [ -e "/home/app/resources/application-datasources.yml" ];
        then
          datasources_default_url=$(yaml_val "/home/app/resources/application-datasources.yml" 'datasources_default_url')
          datasources_default_username=$(yaml_val "/home/app/resources/application-datasources.yml" 'datasources_default_username')
          datasources_default_password=$(yaml_val "/home/app/resources/application-datasources.yml" 'datasources_default_password')
        else
          if [ -e "/home/app/resources/application.yml" ];
          then
            datasources_default_url=$(yaml_val "/home/app/resources/application.yml" 'datasources_default_url')
            datasources_default_username=$(yaml_val "/home/app/resources/application.yml" 'datasources_default_username')
            datasources_default_password=$(yaml_val "/home/app/resources/application.yml" 'datasources_default_password')
          fi
        fi

        if [ "${datasources_default_url}" == "" ];
        then
          datasources_default_url="jdbc:postgresql://localhost:5432/sandbox"
        fi

        if [ "${datasources_default_username}" == "" ];
        then
                datasources_default_username="sandbox"
        fi

        if [ "${datasources_default_password}" == "" ];
        then
                datasources_default_password="sandbox"
        fi

        export DATABASE_USERNAME="${datasources_default_username}"
        export DATABASE_PASSWORD="${datasources_default_password}"

        db="${datasources_default_url##*/}"
        db="${db%%\?*}"

        export DATABASE_NAME="${db}"
      fi
else
  echo "Missing resource files"
  exit 1
fi
