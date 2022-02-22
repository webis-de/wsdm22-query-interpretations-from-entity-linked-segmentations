FROM ubuntu:22.04

RUN mkdir -p /deploy/ && cd /deploy &&\
    apt update && \
    apt install -y openjdk-8-jdk maven git zip build-essential && \
    git clone http://github.com/marcocor/smaph && \
    cd smaph/libs && /bin/bash ./install_libs.sh && cd .. && \
    mkdir -p storage/mapdb

ENV JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64/

WORKDIR /deploy/smaph

COPY e2a.db /deploy/smaph/storage/mapdb
COPY freebase.db /deploy/smaph/storage/mapdb
COPY wikipedia_pages.db /deploy/smaph/storage/mapdb

RUN mvn clean compile

CMD ["mvn", "exec:java", "-P", "server", "-Dsmaph.wat_gcube_token=4549d447-b3b9-4cf6-9748-3b2bcd39e7b4-843339462", "-Dsmaph.host=0.0.0.0"]

EXPOSE 8080