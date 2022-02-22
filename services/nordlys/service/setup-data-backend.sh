#!/bin/bash

/bin/bash /utils/wait-for-it.sh -t 100 nordlys-mongo:27017
/bin/bash /utils/wait-for-it.sh -t 100 nordlys-elastic:9200

CONTAINER_ALREADY_STARTED="CONTAINER_ALREADY_STARTED_PLACEHOLDER"
if [ ! -e $CONTAINER_ALREADY_STARTED ]; then
    touch $CONTAINER_ALREADY_STARTED
    echo "-- First container startup --"

    cd /deploy/nordlys || exit 1
    ./scripts/load_mongo_dumps.sh mongo_dbpedia-2015-10.tar.bz2 || exit 1
    ./scripts/load_mongo_dumps.sh mongo_surface_forms_dbpedia.tar.bz2 || exit 1
    ./scripts/load_mongo_dumps.sh mongo_surface_forms_facc.tar.bz2 || exit 1
    ./scripts/load_mongo_dumps.sh mongo_fb2dbp-2015-10.tar.bz2 || exit 1
    ./scripts/load_mongo_dumps.sh mongo_word2vec-googlenews.tar.bz2 || exit 1

    ./scripts/download_auxiliary.sh || exit 1
    ./scripts/download_dbpedia.sh || exit 1

    ./scripts/build_dbpedia_index.sh core
    ./scripts/build_dbpedia_index.sh types
    ./scripts/build_dbpedia_index.sh uri
else
    echo "-- Not first container startup --"
fi

