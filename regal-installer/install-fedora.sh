#! /bin/bash -x

source variables.conf

if [ -f fcrepo-installer-3.7.1.jar ]
then
	echo "fcrepo is already here! Stop downloading!"
else
	wget http://sourceforge.net/projects/fedora-commons/files/fedora/3.7.1/fcrepo-installer-3.7.1.jar 
fi

export FEDORA_ARCHIVE_HOME=$ARCHIVE_HOME/fedora
java -jar fcrepo-installer-3.7.1.jar  $ARCHIVE_HOME/conf/install.properties