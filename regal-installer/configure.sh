#! /bin/bash

source variables.conf

function makeDir()
{
echo "Create ARCHIVE_HOME $ARCHIVE_HOME"
mkdir -v -p $ARCHIVE_HOME/src
mkdir -v $ARCHIVE_HOME/sync
mkdir -v $ARCHIVE_HOME/fedora
if [ -n $MODULE ]
then
mkdir -v $ARCHIVE_HOME/${MODULE}base
fi
mkdir -v $ARCHIVE_HOME/logs
mkdir -v $ARCHIVE_HOME/conf
mkdir -v $ARCHIVE_HOME/bin
mkdir -v -p $ARCHIVE_HOME/proai/cache
mkdir -v -p $ARCHIVE_HOME/proai/sessions
mkdir -v -p $ARCHIVE_HOME/proai/schemas
ln -s $ARCHIVE_HOME/bin/variables.conf $ARCHIVE_HOME/bin/scripts/variables.conf
}

function createConfig()
{
substituteVars install.properties $ARCHIVE_HOME/conf/install.properties
substituteVars fedora-users.xml $ARCHIVE_HOME/conf/fedora-users.xml
substituteVars api.properties $ARCHIVE_HOME/conf/api.properties
substituteVars tomcat-users.xml $ARCHIVE_HOME/conf/tomcat-users.xml
substituteVars setenv.sh $ARCHIVE_HOME/conf/setenv.sh
substituteVars elasticsearch.yml $ARCHIVE_HOME/conf/elasticsearch.yml
substituteVars site.conf $ARCHIVE_HOME/conf/site.conf
substituteVars logging.properties $ARCHIVE_HOME/conf/logging.properties
substituteVars catalina.out $ARCHIVE_HOME/conf/catalina.out
substituteVars Identify.xml $ARCHIVE_HOME/conf/Identify.xml
substituteVars proai.properties $ARCHIVE_HOME/conf/proai.properties
substituteVars robots.txt $ARCHIVE_HOME/conf/robots.txt
substituteVars tomcat.conf $ARCHIVE_HOME/conf/tomcat.conf
substituteVars application.conf $ARCHIVE_HOME/conf/application.conf
cp templates/favicon.ico $ARCHIVE_HOME/conf/favicon.ico
}

function substituteVars()
{
file=templates/$1
target=$2
sed -e "s,\$ARCHIVE_HOME,$ARCHIVE_HOME,g" \
-e "s,\$ARCHIVE_USER,$ARCHIVE_USER,g" \
-e "s,\$ARCHIVE_PASSWORD,$ARCHIVE_PASSWORD,g" \
-e "s,\$SERVER,$SERVER,g" \
-e "s,\$BACKEND,$BACKEND,g" \
-e "s,\$FRONTEND,$FRONTEND,g" \
-e "s,\$URNBASE,$URNBASE,g" \
-e "s,\$IP,$IP,g" \
-e "s,\$TOMCAT_PORT,$TOMCAT_PORT,g" \
-e "s,\$EMAIL,$EMAIL,g" \
-e "s,\$PLAYPORT,$PLAYPORT,g" \
-e "s,\$TOMCAT_HOME,$TOMCAT_HOME,g" \
-e "s,\$TOMCAT_CONF,$TOMCAT_CONF,g" \
-e "s,\$ELASTICSEARCH_CONF,$ELASTICSEARCH_CONF,g" \
-e "s,\$ELASTICSEARCH_PORT,$ELASTICSEARCH_PORT,g" $file > $target
}



makeDir > /dev/null 2>&1
createConfig
