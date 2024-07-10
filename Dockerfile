FROM python:3.12

ENV TZ=Europe/Moscow
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

RUN apt-get update
RUN apt-get install openjdk-17-jre -y
RUN apt-get install wget -y
RUN apt-get install ffmpeg -y

RUN wget https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp -O /usr/local/bin/yt-dlp
RUN chmod a+rx /usr/local/bin/yt-dlp

COPY target/app.jar .
ENTRYPOINT ["java", "-jar", "app.jar"]