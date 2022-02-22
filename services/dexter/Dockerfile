FROM openjdk:7

RUN mkdir -p /deploy/ \
    && curl -Sl http://dexter.isti.cnr.it/dexter.tar.gz \
    | tar -xzC /deploy/

WORKDIR /deploy/dexter2
CMD ["java", "-Xmx4G", "-jar", "dexter-2.1.0.jar"]

EXPOSE 8080