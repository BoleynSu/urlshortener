FROM docker.io/library/maven@sha256:7106ce47b5c226ee21bac386cb706ff9f3db7ac2f15e251e3aa491fcbfc30028 AS builder
WORKDIR /build
COPY ./ ./

RUN mvn package
RUN mkdir -p out
RUN mvn help:evaluate -q -Dexpression=project.version -DforceStdout > out/version
RUN mv target/urlshortener-$(cat out/version)-jar-with-dependencies.jar out/urlshortener.jar

FROM docker.io/library/openjdk@sha256:3d88de143bdd54d3e873302355564818e0ac0d17dbacc9869090197b7dd8a8f3
RUN microdnf install -y shadow-utils && microdnf clean all
COPY --from=builder /build/out /urlshortener

RUN useradd -r urlshortener
USER urlshortener
VOLUME /data
EXPOSE 8080

ENV ADDRESS 0.0.0.0
ENV PORT 8080
ENV DB /data/db
# ENV USERNAME
# ENV PASSWORD

CMD java -Durlshortener-host=$ADDRESS \
         -Durlshortener-port=$PORT \
         -Durlshortener-db=$DB \
         -Durlshortener-username=$USERNAME \
         -Durlshortener-password=$PASSWORD \
         -jar /urlshortener/urlshortener.jar
