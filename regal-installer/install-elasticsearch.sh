#! /bin/bash

if [ -f elasticsearch-1.1.0.deb ]
then
    echo "elasticsearch is already here! Stop downloading!"
else
    wget https://download.elasticsearch.org/elasticsearch/elasticsearch/elasticsearch-1.1.0.deb
fi
echo "Install Elasticsearch"
sudo dpkg -i elasticsearch-1.1.0.deb
echo "Start Elasticsearch"
sudo service elasticsearch start