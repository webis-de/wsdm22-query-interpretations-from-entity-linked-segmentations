#!/bin/bash

SCRIPT_PATH=$(dirname "$(realpath -s "$0")")
PROJECT_HOME=$(realpath "$SCRIPT_PATH/../../..")
DATA_PATH="$PROJECT_HOME/data/persistent"
CORPORA_PATH="$PROJECT_HOME/data/corpora"


mkdir -p "$DATA_PATH"
pushd "$DATA_PATH"
if [ ! -d "$DATA_PATH/entity-commonness" ]
then
  wget -nc https://files.webis.de/wsdm22-query-interpretation-data/entity-commonness.zip
fi

if [ ! -d "$DATA_PATH/wiki-entity-index" ]
then
  wget -nc https://files.webis.de/wsdm22-query-interpretation-data/wiki-entity-index.zip
fi

# check if unzip is installed (avoid deleting dataset if not)
if [ -n "$(which unzip)" ]
then
  unzip '*.zip'
  rm -rf *.zip
else
  echo "unzip is not installed. Skip unzipping and deleting archives."
fi
popd

mkdir -p "$CORPORA_PATH"
pushd "$CORPORA_PATH"

if  [ ! -d "$CORPORA_PATH/webis-qinc-22" ]
then
  wget -nc https://zenodo.org/record/5820673/files/webis-qinc-22.zip
fi

if [ -n "$(which unzip)" ]
then
unzip webis-qinc-22.zip -d webis-qinc-22
rm -rf *.zip
else
  echo "unzip is not installed. Skip unzipping and deleting archives."
fi
popd
