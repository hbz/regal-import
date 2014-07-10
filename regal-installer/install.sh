#! /bin/bash
#
# Author: Jan Schnasse
# schnasse@hbz-nrw.de
#

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
-e "s,\$ELASTICSEARCH_PORT,$ELASTICSEARCH_PORT,g" $file > $target
}

function install()
{
echo "download some files"

if [ -f $ARCHIVE_HOME/src/README.textile ]
then
	echo "regal src clone already exists. Stop cloning!"
else
	git clone https://github.com/edoweb/regal.git $ARCHIVE_HOME/src
	cd $ARCHIVE_HOME/src
	if [ $? -ne 0 ]
	then
		echo "Can not fetch code from git. FAILED: git clone http://github.com/edoweb/regal.git $ARCHIVE_HOME/src "
		exit 2;
	fi
 	git checkout -b test origin/test
	git checkout master
	cd -
fi

if [ -f fcrepo-installer-3.7.1.jar ]
then
	echo "fcrepo is already here! Stop downloading!"
else
	wget http://sourceforge.net/projects/fedora-commons/files/fedora/3.7.1/fcrepo-installer-3.7.1.jar 
fi

if [ -f elasticsearch-1.1.0.tar.gz ]
then
	echo "elasticsearch is already here! Stop downloading!"
else
	wget http://download.elasticsearch.org/elasticsearch/elasticsearch/elasticsearch-1.1.0.tar.gz
fi

echo "install fedora"
export FEDORA_ARCHIVE_HOME=$ARCHIVE_HOME/fedora
export CATALINA_ARCHIVE_HOME=$ARCHIVE_HOME/fedora/tomcat
java -jar fcrepo-installer-3.7.1.jar  $ARCHIVE_HOME/conf/install.properties

echo "install elasticsearch"
tar -xzf elasticsearch-1.1.0.tar.gz
mv elasticsearch-1.1.0 $ARCHIVE_HOME/elasticsearch
pwd
cp variables.conf $ARCHIVE_HOME/bin/
cp -r templates $ARCHIVE_HOME/bin/
cd $ARCHIVE_HOME/src/regal-archive
mvn clean install >> $ARCHIVE_HOME/logs/regal-build.log
cd -
cd $ARCHIVE_HOME/src/regal-mabconverter
mvn clean install >> $ARCHIVE_HOME/logs/regal-build.log
cd -
cd $ARCHIVE_HOME/src/
mvn clean install >> $ARCHIVE_HOME/logs/regal-build.log
cd -
}

function copyConfig()
{
echo "copy tomcat config"
cp $ARCHIVE_HOME/conf/tomcat-users.xml $ARCHIVE_HOME/fedora/tomcat/conf
cp $ARCHIVE_HOME/conf/fedora-users.xml $ARCHIVE_HOME/fedora/server/config/
cp $ARCHIVE_HOME/conf/setenv.sh $ARCHIVE_HOME/fedora/tomcat/bin
cp $ARCHIVE_HOME/conf/logging.properties $ARCHIVE_HOME/fedora/tomcat/conf
cp $ARCHIVE_HOME/conf/catalina.out $ARCHIVE_HOME/fedora/tomcat/logs/catalina.out
echo "copy elasticsearch config"
mv $ARCHIVE_HOME/elasticsearch/config/elasticsearch.yml $ARCHIVE_HOME/elasticsearch/config/elasticsearch.yml.bck
cp $ARCHIVE_HOME/conf/elasticsearch.yml $ARCHIVE_HOME/elasticsearch/config/
echo "install archive"
cp  $ARCHIVE_HOME/conf/api.properties $ARCHIVE_HOME/src/regal-api/conf
cp  $ARCHIVE_HOME/conf/application.conf $ARCHIVE_HOME/src/regal-api/conf
}

function updateMaster()
{
echo "Fetch source..."
cd $ARCHIVE_HOME/src
git pull
git checkout master
cp  $ARCHIVE_HOME/conf/api.properties $ARCHIVE_HOME/src/regal-api/src/main/resources
echo -e "Start maven build. If this is your first regal installation, downloading all dependencies can take a lot of time. You can follow the process with tail -f $ARCHIVE_HOME/logs/regal-build.log"
mvn -e clean install -DskipTests --settings settings.xml >> $ARCHIVE_HOME/logs/regal-build.log
cd -
}

function updateTest()
{
echo "Fetch source..."
cd $ARCHIVE_HOME/src
git pull
git checkout test
cd -
}

function configApi()
{
cd $ARCHIVE_HOME/src
cp  $ARCHIVE_HOME/conf/api.properties $ARCHIVE_HOME/src/regal-api/src/main/resources
echo "Start maven build. If this is your first regal installation, downloading all dependencies can take a lot of time. You can follow the process with tail -f $ARCHIVE_HOME/logs/regal-build.log"
mvn -e clean install -DskipTests --settings settings.xml >> $ARCHIVE_HOME/logs/regal-build.log 
cd -
}

function rollout()
{
setSystemVars

installApi

buildModule
}

function setSystemVars
{
export FEDORA_HOME=$ARCHIVE_HOME/fedora
export CATALINA_HOME=$FEDORA_HOME/tomcat
}

function installApi
{
cd $SRC/regal-api
mvn clean install
mvn play2:start
cd -
}

function buildModule
{
SRC=$ARCHIVE_HOME/src
SYNCER_SRC=$ARCHIVE_HOME/src/${MODULE}-sync/target/${MODULE}sync.jar
SYNCER_DEST=$ARCHIVE_HOME/sync/${MODULE}sync.jar
if [ -n "$MODULE" ]
then
	cd $SRC
	mvn install -DskipTests >> $ARCHIVE_HOME/logs/regal-build.log
	cd -
	echo "Generate Module $MODULE, templates can be found in $ARCHIVE_HOME/sync"
	cd $ARCHIVE_HOME/src/${MODULE}-sync
	mvn -q -e assembly:assembly -DskipTests --settings ../settings.xml >> $ARCHIVE_HOME/logs/regal-build.log
	cd -
	cp $SYNCER_SRC $SYNCER_DEST 
	echo -e "#! /bin/bash" > ${NAMESPACE}Sync.sh.tmpl
	echo -e "" >> ${NAMESPACE}Sync.sh.tmpl
	echo -e "source $ARCHIVE_HOME/sync/${NAMESPACE}Variables.conf" >> ${NAMESPACE}Sync.sh.tmpl
	echo -e "export LANG=en_US.UTF-8" >> ${NAMESPACE}Sync.sh.tmpl
	echo -e "" >> ${NAMESPACE}Sync.sh.tmpl
	echo -e "cd \$ARCHIVE_HOME/sync" >> ${NAMESPACE}Sync.sh.tmpl
	echo -e "" >> ${NAMESPACE}Sync.sh.tmpl
	echo -e "cp .oaitimestamp\$NAMESPACE oaitimestamp\${NAMESPACE}\`date +\"%Y%m%d\"\`" >> ${NAMESPACE}Sync.sh.tmpl
	echo -e "" >> ${NAMESPACE}Sync.sh.tmpl
	echo -e "java -jar -Xms512m -Xmx512m \${MODULE}sync.jar --mode INIT -list \$ARCHIVE_HOME/sync/pidlist.txt --user \$ARCHIVE_USER --password \$ARCHIVE_PASSWORD --dtl \$DOWNLOAD --cache \$ARCHIVE_HOME/\${NAMESPACE}base --oai  \$OAI --set \$SET --timestamp .oaitimestamp\$NAMESPACE --fedoraBase http://\$SERVER:\$TOMCAT_PORT/fedora --host http://\$SERVER --namespace \$NAMESPACE >> ${NAMESPACE}log\`date +\"%Y%m%d\"\`.txt 2>&1" >> ${NAMESPACE}Sync.sh.tmpl
	echo -e "" >> ${NAMESPACE}Sync.sh.tmpl
	echo -e "cd -" >> ${NAMESPACE}Sync.sh.tmpl

	mv ${NAMESPACE}Sync.sh.tmpl $ARCHIVE_HOME/sync
	cat $MODULE_CONF variables.conf > $ARCHIVE_HOME/sync/${NAMESPACE}Variables.conf.tmpl
fi
}

usage="Wrong usage! Please try with: \n -u to update to last release \n -u test to update to last test build. \n -ext <sync.conf>. to create a sync module.";

if [ $# -eq 0 ]
then
	makeDir
	createConfig
	install
	copyConfig
	configApi
	rollout
elif [ $# -eq 1 ]
then
    
    if [ $1 == "-u" ]
    then
	makeDir > /dev/null 2>&1
	createConfig
	copyConfig
	updateMaster
	configApi
	rollout     
    else
	echo -e $usage
    fi
else
    if [[ $1 == "-u" ]] && [[ $2 == "test" ]]
    then
	makeDir > /dev/null 2>&1
	createConfig
	copyConfig
	updateTest
	configApi
	rollout	
    elif [[ $1 == "-u" ]] && [[ $2 == "local" ]]
    then
	makeDir > /dev/null 2>&1
	createConfig
	copyConfig
	configApi
	rollout
    elif [[ $1 == "-ext" ]] && [[ -n "$2" ]]
    then
	MODULE_CONF=$2
	source $MODULE_CONF
	buildModule
    else
	echo -e $usage
    fi
fi
