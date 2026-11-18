#!/usr/bin/env bash
set -e

ADD_PLUGINS="true"

if [ "${PLUGINS_IS_MOUNTED}" == "true" ];
then
  if [ "$(ls -1 /home/app/plugins)" != "" ];
  then
    echo "There are persisted plugins."
    ADD_PLUGINS="false"
  fi
fi

if [ -e /opt/init/micronaut ];
then
  mkdir -p /home/app/plugins || true
  pushd /opt/init/micronaut

        shopt -s nullglob
        for f in *
        do
          case "${f}" in
            *.sh)
                echo "Running ${f}"
                if [ -x "${f}" ];
                then
                    /bin/bash "${f}"
                else
                    . "${f}"
                fi
              ;;
            *.jar)
              if [ "${ADD_PLUGINS}" == "true" ];
              then
                  echo "Adding ${f} as plugin"
                  cp -pf ${f} /home/app/plugins/.
              fi
              ;;
            *)
                  echo "Copying ${f} to micronaut resources"
                  cp "${f}" /home/app/resources/.
              ;;
          esac
        done
        shopt -u nullglob
  popd
else
      echo "No /opt/init/micronaut for *.sh *.yml *.xml *.properties etc - skipping"
fi

# Figure out the java options
if [ "${PG_SHARING_HOST}" == "true" ];
then
declare -A java_opts=(
    [2]="-Xms128M -Xmx1G -XX:MetaspaceSize=64M -XX:MaxMetaspaceSize=64M -XX:CompressedClassSpaceSize=64M -XX:ReservedCodeCacheSize=128M -XX:MaxDirectMemorySize=64M"
    [4]="-Xms256M -Xmx2G -XX:MetaspaceSize=128M -XX:MaxMetaspaceSize=128M -XX:CompressedClassSpaceSize=64M -XX:ReservedCodeCacheSize=256M -XX:MaxDirectMemorySize=256M"
    [6]="-Xms768M -Xmx3G -XX:MetaspaceSize=256M -XX:MaxMetaspaceSize=256M -XX:CompressedClassSpaceSize=64M -XX:ReservedCodeCacheSize=384M -XX:MaxDirectMemorySize=512M"
    [8]="-Xms1G -Xmx4G -XX:MetaspaceSize=256M -XX:MaxMetaspaceSize=320M -XX:CompressedClassSpaceSize=64M -XX:ReservedCodeCacheSize=384M -XX:MaxDirectMemorySize=512M"
    [12]="-Xms1536M -Xmx6G -XX:MetaspaceSize=256M -XX:MaxMetaspaceSize=512M -XX:CompressedClassSpaceSize=128M -XX:ReservedCodeCacheSize=384M -XX:MaxDirectMemorySize=1G"
    [16]="-Xms2G -Xmx8G -XX:MetaspaceSize=256M -XX:MaxMetaspaceSize=704M -XX:CompressedClassSpaceSize=192M -XX:ReservedCodeCacheSize=384M -XX:MaxDirectMemorySize=1280M"
    [24]="-Xms3G -Xmx12G -XX:MetaspaceSize=256M -XX:MaxMetaspaceSize=1G -XX:CompressedClassSpaceSize=320M -XX:ReservedCodeCacheSize=512M -XX:MaxDirectMemorySize=2G"
    [32]="-Xms4G -Xmx16G -XX:MetaspaceSize=256M -XX:MaxMetaspaceSize=1408M -XX:CompressedClassSpaceSize=384M -XX:ReservedCodeCacheSize=512M -XX:MaxDirectMemorySize=2816M"
    [48]="-Xms6400M -Xmx24G -XX:MetaspaceSize=256M -XX:MaxMetaspaceSize=2112M -XX:CompressedClassSpaceSize=640M -XX:ReservedCodeCacheSize=512M -XX:MaxDirectMemorySize=4G"
    [64]="-Xms8448M -Xmx32G -XX:MetaspaceSize=256M -XX:MaxMetaspaceSize=2816M -XX:CompressedClassSpaceSize=832M -XX:ReservedCodeCacheSize=512M -XX:MaxDirectMemorySize=5632M"
    [96]="-Xms13G -Xmx48G -XX:MetaspaceSize=256M -XX:MaxMetaspaceSize=4G -XX:CompressedClassSpaceSize=1G -XX:ReservedCodeCacheSize=768M -XX:MaxDirectMemorySize=8960M"
    [128]="-Xms18G -Xmx64G -XX:MetaspaceSize=256M -XX:MaxMetaspaceSize=4G -XX:CompressedClassSpaceSize=1G -XX:ReservedCodeCacheSize=768M -XX:MaxDirectMemorySize=12G"
)
else
  declare -A java_opts=(
    [2]="-Xms256M -Xmx2G -XX:MetaspaceSize=64M -XX:MaxMetaspaceSize=64M -XX:CompressedClassSpaceSize=64M -XX:ReservedCodeCacheSize=128M -XX:MaxDirectMemorySize=64M"
    [4]="-Xms768M -Xmx4G -XX:MetaspaceSize=256M -XX:MaxMetaspaceSize=256M -XX:CompressedClassSpaceSize=64M -XX:ReservedCodeCacheSize=384M -XX:MaxDirectMemorySize=512M"
    [6]="-Xms1536M -Xmx6G -XX:MetaspaceSize=256M -XX:MaxMetaspaceSize=512M -XX:CompressedClassSpaceSize=128M -XX:ReservedCodeCacheSize=384M -XX:MaxDirectMemorySize=1G"
    [8]="-Xms2G -Xmx8G -XX:MetaspaceSize=256M -XX:MaxMetaspaceSize=704M -XX:CompressedClassSpaceSize=192M -XX:ReservedCodeCacheSize=384M -XX:MaxDirectMemorySize=1280M"
    [12]="-Xms3G -Xmx12G -XX:MetaspaceSize=256M -XX:MaxMetaspaceSize=1G -XX:CompressedClassSpaceSize=320M -XX:ReservedCodeCacheSize=512M -XX:MaxDirectMemorySize=2G"
    [16]="-Xms4G -Xmx16G -XX:MetaspaceSize=256M -XX:MaxMetaspaceSize=1408M -XX:CompressedClassSpaceSize=384M -XX:ReservedCodeCacheSize=512M -XX:MaxDirectMemorySize=2816M"
    [24]="-Xms6400M -Xmx24G -XX:MetaspaceSize=256M -XX:MaxMetaspaceSize=2112M -XX:CompressedClassSpaceSize=640M -XX:ReservedCodeCacheSize=512M -XX:MaxDirectMemorySize=4G"
    [32]="-Xms8448M -Xmx32G -XX:MetaspaceSize=256M -XX:MaxMetaspaceSize=2816M -XX:CompressedClassSpaceSize=832M -XX:ReservedCodeCacheSize=512M -XX:MaxDirectMemorySize=5632M"
    [48]="-Xms12800M -Xmx48G -XX:MetaspaceSize=256M -XX:MaxMetaspaceSize=4G -XX:CompressedClassSpaceSize=1G -XX:ReservedCodeCacheSize=768M -XX:MaxDirectMemorySize=8448M"
    [64]="-Xms17152M -Xmx64G -XX:MetaspaceSize=256M -XX:MaxMetaspaceSize=4G -XX:CompressedClassSpaceSize=1G -XX:ReservedCodeCacheSize=768M -XX:MaxDirectMemorySize=11G"
    [96]="-Xms26880M -Xmx96G -XX:MetaspaceSize=256M -XX:MaxMetaspaceSize=4G -XX:CompressedClassSpaceSize=1G -XX:ReservedCodeCacheSize=768M -XX:MaxDirectMemorySize=16G"
    [128]="-Xms36G -Xmx128G -XX:MetaspaceSize=256M -XX:MaxMetaspaceSize=4G -XX:CompressedClassSpaceSize=1G -XX:ReservedCodeCacheSize=768M -XX:MaxDirectMemorySize=16G"
  )
fi

chosen_opts=""
best_key=0
for key in "${!java_opts[@]}"; do
    if (( MEMORY_AVAILABLE_GB >= key && key > best_key ));
    then
        best_key=$key
        chosen_opts=${java_opts[$key]}
    fi
done

common_java_opts="-server -Xss1M -XX:+UseG1GC -XX:MaxGCPauseMillis=500 -XX:InitialCodeCacheSize=48M -XX:+ExitOnOutOfMemoryError -XX:-UseBASE64Intrinsics"
export JAVA_OPTS="${chosen_opts} ${common_java_opts}"

# Crack open the manifest to get the main class

APPLICATION_MANIFEST="$(unzip -p /home/app/application.jar META-INF/MANIFEST.MF)"
APPLICATION_MAIN_CLASS=$(echo "${APPLICATION_MANIFEST}" | grep '^Main-Class:' | awk '{print $2}' | tr -d '\r')

JAVA_BIN="$(readlink -f "$(command -v java)")"

# Start Micronaut
echo "Starting Micronaut..."
cd /home/app
# Give the directory and contents to the micronaut user
chown -R micronaut:micronaut /home/app
echo ${JAVA_BIN} "${JAVA_OPTS}" -cp "/home/app/application.jar" -DPLUGINS_IS_MOUNTED=${PLUGINS_IS_MOUNTED} "${APPLICATION_MAIN_CLASS}"
gosu micronaut ${JAVA_BIN} ${JAVA_OPTS} -cp /home/app/application.jar -DPLUGINS_IS_MOUNTED=${PLUGINS_IS_MOUNTED} "${APPLICATION_MAIN_CLASS}"
