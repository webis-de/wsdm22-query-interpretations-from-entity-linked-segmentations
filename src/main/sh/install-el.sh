#!/bin/bash

SCRIPT_PATH=$(dirname "$(realpath -s "$0")")
PROJECT_HOME=$(realpath "$SCRIPT_PATH/../../..")
DATA_PATH="$PROJECT_HOME/data/persistent"

mkdir -p "$DATA_PATH"
pushd "$DATA_PATH"
wget https://files.webis.de/wsdm22-query-interpretation-data/wiki-entity-index.zip
unzip wiki-entity-index.zip
popd