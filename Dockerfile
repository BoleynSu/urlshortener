FROM maven@sha256:c271b881d38d5753329d8cf135dc91a53d5c951d896b40d1248b687f7a4d9701 as build
RUN microdnf install -y shadow-utils && useradd builder
WORKDIR /build
RUN chown builder:builder /build
USER builder
COPY --chown=builder ./ ./

RUN mvn package
RUN mkdir -p out
RUN mvn help:evaluate -q -Dexpression=project.version -DforceStdout > out/version
RUN mv target/urlshortener-$(cat out/version)-jar-with-dependencies.jar out/urlshortener.jar

FROM openjdk@sha256:4b6864318b0effb41be1fcb3e8d0ef5f529534f8ad757e0330741d9664408507
RUN microdnf install -y shadow-utils && microdnf clean all
COPY --from=build /build/out /urlshortener

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
