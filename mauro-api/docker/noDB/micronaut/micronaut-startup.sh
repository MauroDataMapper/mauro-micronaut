#!/usr/bin/env bash
set -e

if [ -e /opt/init/micronaut ];
then
  mkdir -p /home/app/plugins
  pushd /opt/init/micronaut

        shopt -s nullglob
        for f in *
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
            *.jar)
              echo "Adding ${f} as plugin"
              cp -pf ${f} /home/app/plugins/.
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

declare -A java_opts=(
  [2]="-server -Xms614M -Xmx2457M -XX:MaxNewSize=1126M -XX:NewSize=204M -XX:MetaspaceSize=256M -XX:MaxMetaspaceSize=784M -XX:NewRatio=2 -XX:SurvivorRatio=2 -XX:TargetSurvivorRatio=80 -XX:+UseParallelGC -XX:+AggressiveHeap -XX:GCTimeRatio=19 -XX:MaxGCPauseMillis=3500 -XX:InitialCodeCacheSize=48M -XX:ReservedCodeCacheSize=240M"
  [4]="-server -Xms1843M -Xmx7372M -XX:MaxNewSize=3379M -XX:NewSize=614M -XX:MetaspaceSize=768M -XX:MaxMetaspaceSize=2764M -XX:NewRatio=2 -XX:SurvivorRatio=2 -XX:TargetSurvivorRatio=80 -XX:+UseParallelGC -XX:+AggressiveHeap -XX:GCTimeRatio=19 -XX:MaxGCPauseMillis=3500 -XX:InitialCodeCacheSize=48M -XX:ReservedCodeCacheSize=307M"
  [6]="-server -Xms3G -Xmx12G -XX:MaxNewSize=5632M -XX:NewSize=1G -XX:MetaspaceSize=1280M -XX:MaxMetaspaceSize=4608M -XX:NewRatio=2 -XX:SurvivorRatio=2 -XX:TargetSurvivorRatio=80 -XX:+UseParallelGC -XX:+AggressiveHeap -XX:GCTimeRatio=19 -XX:MaxGCPauseMillis=3500 -XX:InitialCodeCacheSize=48M -XX:ReservedCodeCacheSize=512M"
  [8]="-server -Xms4300M -Xmx17203M -XX:MaxNewSize=7884M -XX:NewSize=1433M -XX:MetaspaceSize=1792M -XX:MaxMetaspaceSize=6451M -XX:NewRatio=2 -XX:SurvivorRatio=2 -XX:TargetSurvivorRatio=80 -XX:+UseParallelGC -XX:+AggressiveHeap -XX:GCTimeRatio=19 -XX:MaxGCPauseMillis=3500 -XX:InitialCodeCacheSize=48M -XX:ReservedCodeCacheSize=716M"
  [12]="-server -Xms6758M -Xmx27033M -XX:MaxNewSize=12390M -XX:NewSize=2252M -XX:MetaspaceSize=2816M -XX:MaxMetaspaceSize=10137M -XX:NewRatio=2 -XX:SurvivorRatio=2 -XX:TargetSurvivorRatio=80 -XX:+UseParallelGC -XX:+AggressiveHeap -XX:GCTimeRatio=19 -XX:MaxGCPauseMillis=3500 -XX:InitialCodeCacheSize=48M -XX:ReservedCodeCacheSize=1126M"
  [16]="-server -Xms9G -Xmx36G -XX:MaxNewSize=16896M -XX:NewSize=3G -XX:MetaspaceSize=3840M -XX:MaxMetaspaceSize=13824M -XX:NewRatio=2 -XX:SurvivorRatio=2 -XX:TargetSurvivorRatio=80 -XX:+UseParallelGC -XX:+AggressiveHeap -XX:GCTimeRatio=19 -XX:MaxGCPauseMillis=3500 -XX:InitialCodeCacheSize=48M -XX:ReservedCodeCacheSize=1536M"
  [24]="-server -Xms14131M -Xmx56524M -XX:MaxNewSize=25907M -XX:NewSize=4710M -XX:MetaspaceSize=5888M -XX:MaxMetaspaceSize=21196M -XX:NewRatio=2 -XX:SurvivorRatio=2 -XX:TargetSurvivorRatio=80 -XX:+UseParallelGC -XX:+AggressiveHeap -XX:GCTimeRatio=19 -XX:MaxGCPauseMillis=3500 -XX:InitialCodeCacheSize=48M -XX:ReservedCodeCacheSize=2355M"
  [32]="-server -Xms19046M -Xmx76185M -XX:MaxNewSize=34918M -XX:NewSize=6348M -XX:MetaspaceSize=7936M -XX:MaxMetaspaceSize=28569M -XX:NewRatio=2 -XX:SurvivorRatio=2 -XX:TargetSurvivorRatio=80 -XX:+UseParallelGC -XX:+AggressiveHeap -XX:GCTimeRatio=19 -XX:MaxGCPauseMillis=3500 -XX:InitialCodeCacheSize=48M -XX:ReservedCodeCacheSize=3174M"
  [48]="-server -Xms28876M -Xmx115507M -XX:MaxNewSize=52940M -XX:NewSize=9625M -XX:MetaspaceSize=12032M -XX:MaxMetaspaceSize=43315M -XX:NewRatio=2 -XX:SurvivorRatio=2 -XX:TargetSurvivorRatio=80 -XX:+UseParallelGC -XX:+AggressiveHeap -XX:GCTimeRatio=19 -XX:MaxGCPauseMillis=3500 -XX:InitialCodeCacheSize=48M -XX:ReservedCodeCacheSize=4812M"
  [64]="server -Xms38707M -Xmx154828M -XX:MaxNewSize=70963M -XX:NewSize=12902M -XX:MetaspaceSize=16128M -XX:MaxMetaspaceSize=58060M -XX:NewRatio=2 -XX:SurvivorRatio=2 -XX:TargetSurvivorRatio=80 -XX:+UseParallelGC -XX:+AggressiveHeap -XX:GCTimeRatio=19 -XX:MaxGCPauseMillis=3500 -XX:InitialCodeCacheSize=48M -XX:ReservedCodeCacheSize=6451M"
)

chosen_opts=""
best_key=0
for key in "${!java_opts[@]}"; do
    if (( available_gb >= key && key > best_key ));
    then
        best_key=$key
        chosen_opts=${java_opts[$key]}
    fi
done

export JAVA_OPTS="${chosen_opts}"

# Crack open the manifest to get the main class

APPLICATION_MANIFEST="$(unzip -p /home/app/application.jar META-INF/MANIFEST.MF)"
APPLICATION_MAIN_CLASS=$(echo "${APPLICATION_MANIFEST}" | grep '^Main-Class:' | awk '{print $2}' | tr -d '\r')


# Start Micronaut
echo "Starting Micronaut..."
cd /home/app
echo java "${JAVA_OPTS}" -cp "/home/app/application.jar" "${APPLICATION_MAIN_CLASS}"
java $JAVA_OPTS -cp "/home/app/application.jar" "${APPLICATION_MAIN_CLASS}"
