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
mvn clean install -DplayInstallDirectory=$ARCHIVE_HOME -Dplay2version=2.2.3 >> $ARCHIVE_HOME/logs/regal-build.log
mvn play2:start -DplayInstallDirectory=$ARCHIVE_HOME -Dplay2version=2.2.3 >> $ARCHIVE_HOME/logs/regal-build.log
cd -
