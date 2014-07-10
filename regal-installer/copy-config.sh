#!/bin/bash

source variables.conf

function copyConfig()
{
cp $ARCHIVE_HOME/conf/tomcat-users.xml $TOMCAT_CONF
cp $ARCHIVE_HOME/conf/fedora-users.xml $ARCHIVE_HOME/fedora/server/config/
cp $ARCHIVE_HOME/conf/setenv.sh $TOMCAT_HOME/bin
cp $ARCHIVE_HOME/conf/application.conf $ARCHIVE_HOME/src/regal-api/conf
echo "Please execute the following commands manualy"
echo "sudo cp $ARCHIVE_HOME/conf/elasticsearch.yml $ELASTICSEARCH_CONF"
}

copyConfig