FROM docker.io/library/maven@sha256:da478f0656b75aef1af7a94e30346fc7d6b959c7095129306f46579c7c754e00 AS builder
WORKDIR /build
COPY ./ ./

RUN mvn package
RUN mkdir -p out
RUN mvn help:evaluate -q -Dexpression=project.version -DforceStdout > out/version
RUN mv target/urlshortener-$(cat out/version)-jar-with-dependencies.jar out/urlshortener.jar

FROM docker.io/library/openjdk@sha256:f5a0725c9ecc4d9b7fb6286c3ae98bd368c62db5116dba4176cf19aadf965514
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
