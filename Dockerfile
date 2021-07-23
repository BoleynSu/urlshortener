FROM docker.io/library/maven@sha256:da478f0656b75aef1af7a94e30346fc7d6b959c7095129306f46579c7c754e00 AS builder
WORKDIR /build
COPY ./ ./

RUN mvn package
RUN mkdir -p out
RUN mvn help:evaluate -q -Dexpression=project.version -DforceStdout > out/version
RUN mv target/urlshortener-$(cat out/version)-jar-with-dependencies.jar out/urlshortener.jar

FROM docker.io/library/openjdk@sha256:20f84fab04768dfe848be018468a857d4fdd5825397b6b9e2c172cdeb98af86b
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
