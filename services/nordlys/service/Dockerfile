FROM python:3.7

RUN apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 9DA31620334BD75D9DCB49F368818C72E52529D4 && \
    echo "deb [ arch=amd64 ] https://repo.mongodb.org/apt/ubuntu bionic/mongodb-org/4.0 multiverse" | tee /etc/apt/sources.list.d/mongodb-org-4.0.list && \
    apt-get update && \
    apt-get -y install git wget bzip2 mongodb-org-shell gfortran libblas-dev liblapack-dev libatlas-base-dev && \
    mkdir -p /install/ && cd /install/ && wget https://fastdl.mongodb.org/tools/db/mongodb-database-tools-ubuntu2004-x86_64-100.5.1.deb && \
    dpkg -i ./mongodb-database-tools-ubuntu2004-x86_64-100.5.1.deb || apt-get install -f && \
    mkdir -p /deploy/ && cd /deploy/ && \
    git clone https://github.com/iai-group/nordlys.git

WORKDIR /deploy/nordlys

COPY mongo.json /deploy/nordlys/config/mongo.json
COPY elastic.json /deploy/nordlys/config/elastic.json
COPY requirements.txt /deploy/nordlys/requirements.txt
COPY load_mongo_dumps.sh /deploy/nordlys/scripts/load_mongo_dumps.sh
COPY setup-data-backend.sh /deploy/nordlys/scripts/setup-data-backend.sh

ADD https://raw.githubusercontent.com/vishnubob/wait-for-it/e1f115e4ca285c3c24e847c4dd4be955e0ed51c2/wait-for-it.sh /utils/wait-for-it.sh

RUN python -m pip install --upgrade pip && \
    python -m pip install -r requirements.txt && \
    chmod +x /utils/wait-for-it.sh && \
    chmod +x /deploy/nordlys/scripts/load_mongo_dumps.sh && \
    chmod +x /deploy/nordlys/scripts/setup-data-backend.sh

CMD /bin/bash /deploy/nordlys/scripts/setup-data-backend.sh ; python -m nordlys.services.api
EXPOSE 5000