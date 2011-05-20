#!/bin/sh
for i in lib/*;do
    NAME=`basename $i | awk -F\- '{print $1}'`;
    VERSION=`echo $i | awk -F\- '{print $2}' | sed 's/.jar//g'`;
    FILENAME=$i;
    echo "INSTALLING LIBRARY TO YOUR LOCAL REPOSITORY";
    echo "NAME = $NAME";
    echo "VERSION = $VERSION";
    mvn install:install-file \
        -Dfile=$i \
        -DgroupId=3rdparty \
        -DartifactId=$NAME \
        -Dversion=$VERSION \
        -Dpackaging=jar
done;
