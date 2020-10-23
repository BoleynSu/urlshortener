FROM maven@sha256:aea55d01edac9df38eb17d19f6aabb55263f93cc8b23ac170a792922111ea487 as build
RUN microdnf install -y shadow-utils && useradd builder
WORKDIR /build
RUN chown builder:builder /build
USER builder
COPY --chown=builder ./ ./

RUN mvn package
RUN mkdir -p out
RUN mvn help:evaluate -q -Dexpression=project.version -DforceStdout > out/version
RUN mv target/urlshortener-$(cat out/version)-jar-with-dependencies.jar out/urlshortener.jar

FROM openjdk@sha256:3eb45b023dd807b424a1e6ff31713497cfbe6028cca140a2a4fd14e854344fc7
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
