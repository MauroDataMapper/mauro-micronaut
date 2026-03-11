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
                    /bin/bash "${f}"
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
