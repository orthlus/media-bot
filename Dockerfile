FROM eclipse-temurin:21-jre-alpine
ENV TZ=Europe/Moscow
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

ENV spring_profiles_active=production

COPY target/dependency dependency/
ADD target/app.jar .

ENTRYPOINT ["java", "-XX:+UseSerialGC", "-Xms64m", "-Xmx250m", "-jar", "app.jar"]