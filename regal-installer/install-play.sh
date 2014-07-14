#!/bin/bash

source variables.conf


if [ -f play-2.2.3.zip ]
then
	echo "Play is already here! Stop downloading!"
else
	wget http://downloads.typesafe.com/play/2.2.3/play-2.2.3.zip 
fi

if [ -d $ARCHIVE_HOME/play-2.2.3 ]
then
	echo "Play2 already installed!"
else
	unzip play-2.2.3.zip -d $ARCHIVE_HOME
fi