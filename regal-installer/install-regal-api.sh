#! /bin/bash

source variables.conf


cd $ARCHIVE_HOME/src/
mvn clean install >> $ARCHIVE_HOME/logs/regal-build.log
cd -
cd $ARCHIVE_HOME/src/regal-api
nohup $ARCHIVE_HOME/play-2.2.3/play start &
cd -
