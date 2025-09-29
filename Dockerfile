FROM eclipse-temurin:21-jre-alpine
ENV TZ=Europe/Moscow
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

ENV spring_profiles_active=production

COPY target/dependency dependency/
ADD target/app.jar .

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]