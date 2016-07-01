FROM java:8-jre

WORKDIR /srv

COPY build/install/lumbermill-simple-samples lumbermill-simple-samples
ADD  src/main/groovy /srv

ENTRYPOINT ["/srv/lumbermill-simple-samples/bin/lumbermill-simple-samples"]

CMD ["docker-welcome.groovy"]