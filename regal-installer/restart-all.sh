#!/bin/bash 

source variables.conf
export FEDORA_HOME=$ARCHIVE_HOME/fedora

sudo service elasticsearch restart

kill `ps -eaf|grep tomcat|awk '{print $2}'|head -1`


cd $ARCHIVE_HOME/src/regal-api
kill `ps -eaf|grep regal-api|awk '{print $2}'|head -1`
nohup $ARCHIVE_HOME/play-2.2.3/play start &
cd -


$TOMCAT_HOME/bin/startup.sh