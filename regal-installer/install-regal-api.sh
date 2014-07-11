#! /bin/bash

source variables.conf

cd $ARCHIVE_HOME/src/regal-archive
mvn clean install >> $ARCHIVE_HOME/logs/regal-build.log
cd -
cd $ARCHIVE_HOME/src/regal-mabconverter
mvn clean install >> $ARCHIVE_HOME/logs/regal-build.log
cd -
cd $ARCHIVE_HOME/src/
mvn clean install >> $ARCHIVE_HOME/logs/regal-build.log
cd -
cd $ARCHIVE_HOME/src/regal-api
mvn clean install -DplayInstallDirectory=$ARCHIVE_HOME/play
mvn play2:start
cd -
