#! /bin/bash
#
# Author: Jan Schnasse
# schnasse@hbz-nrw.de
#

source variables.conf


function install()
{
echo "Install Elasticsearch"
./install-elasticsearch.sh
echo "Install Fedora"
./install-fedora.sh
echo "Install Regal API"
./install-regal-api.sh
}

install

