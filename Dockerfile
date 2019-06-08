FROM maven:3.5.4-jdk-8 as builder

LABEL company="Pharos Production Inc."
LABEL version="0.9.2"

ENV LANG=C.UTF-8 \
  REFRESHED_AT=2019-08-07-1
ENV DEBIAN_FRONTEND noninteractive

USER nobody:nogroup
ADD --chown=nobody:nogroup . /tessera
RUN cd /tessera && mvn clean -Dmaven.repo.local=/tessera/.m2/repository -DskipTests -Denforcer.skip=true package

#############################################################

FROM openjdk:8-jre-alpine

LABEL company="Pharos Production Inc."
LABEL version="0.9.2"

ENV LANG=C.UTF-8 \
  REFRESHED_AT=2019-08-07-1
ENV DEBIAN_FRONTEND noninteractive

COPY --from=builder /tessera/tessera-dist/tessera-app/target/*-app.jar /opt/tessera/tessera-app.jar

COPY ./scripts /opt/tessera/scripts
RUN chmod +x /opt/tessera/scripts/tessera.sh

CMD ["/bin/bash"]
