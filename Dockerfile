FROM eclipse-temurin:latest
ARG APP_JAR
# persistent volume containing the app data (biopax model ad index files;
# can use target/work demo/test data)
COPY ${APP_JAR} cpath2.war
ENV CPATH2_HOME /work
ENV JDK_JAVA_OPTIONS="--add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.lang.reflect=ALL-UNNAMED --add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens java.base/java.io=ALL-UNNAMED"
ENTRYPOINT ["java", "-server", "-Djava.security.egd=file:/dev/./urandom", \
"-Dfile.encoding=UTF-8", "-Xss32m", "-Xmx64g", \
"-Dspring.profiles.active=docker", \
"-Dpaxtools.model.safeset=list", \
"-Dpaxtools.normalizer.use-latest-registry=true", \
"-jar", "/cpath2.war", "--server"]
EXPOSE 8080
