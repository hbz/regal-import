#! /bin/bash

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


MODULE_CONF=$1
source $MODULE_CONF
buildModule
