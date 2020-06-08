FROM maven as build
RUN mkdir -p $HOME/.m2 && echo '<settings><mirrors><mirror><id>google-maven-central</id><name>GCS Maven Central mirror</name><url>https://maven-central.storage-download.googleapis.com/maven2/</url><mirrorOf>central</mirrorOf></mirror></mirrors></settings>' > $HOME/.m2/settings.xml
RUN mkdir -p /build/out
WORKDIR /build
COPY ./ ./
RUN mvn package
RUN mvn help:evaluate -q -Dexpression=project.version -DforceStdout > out/version
RUN mv target/urlshortener-$(cat version)-jar-with-dependencies.jar out/urlshortener.jar

FROM openjdk
ENV APPROOT=/boleyn.su/opt/boleyn.su/urlshortener
RUN mkdir -p $APPROOT
WORKDIR $APPROOT

COPY  --from=build /build/out $APPROOT

RUN useradd -r urlshortener
USER urlshortener:urlshortener
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
                  -jar /boleyn.su/opt/boleyn.su/urlshortener/urlshortener.jar
