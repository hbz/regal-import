#!/bin/bash

source variables.conf

cd $ARCHIVE_HOME/src/regal-tests
mvn clean test
cd -