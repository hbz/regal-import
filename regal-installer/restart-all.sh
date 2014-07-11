#!/bin/bash 

source variables.conf
export FEDORA_HOME=$ARCHIVE_HOME/fedora

sudo service elasticsearch restart

kill `ps -eaf|grep tomcat|awk '{print $2}'|head -1`


cd $ARCHIVE_HOME/src/regal-api
kill `ps -eaf|grep regal-api|awk '{print $2}'|head -1`
mvn play2:start -DplayInstallDirectory=$ARCHIVE_HOME/play
cd -


$TOMCAT_HOME/bin/startup.sh