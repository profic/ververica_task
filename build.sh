#! /bin/bash

if [ -d sbt ]
then
    echo "sbt dir present"
else
    wget https://piccolo.link/sbt-1.3.13.zip -O sbt.zip
    unzip sbt.zip
fi

sbt/bin/sbt universal:packageBin
unzip target/universal/ververica_task-0.1.zip