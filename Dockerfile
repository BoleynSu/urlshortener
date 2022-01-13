FROM docker.io/library/maven@sha256:208ceab2ef5edc2845e18942fd63a153842ed7a098687321a8012c53c0b5b3ef AS builder
WORKDIR /build
COPY ./ ./

RUN mvn package
RUN mkdir -p out
RUN mvn help:evaluate -q -Dexpression=project.version -DforceStdout > out/version
RUN mv target/urlshortener-$(cat out/version)-jar-with-dependencies.jar out/urlshortener.jar

FROM docker.io/library/openjdk@sha256:0da39ed69dec14f9603e2b916592691cc39341510abdf4255abb1c90b00eb3f4
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
