FROM eclipse-temurin:21-jdk

RUN apt-get update && \
    apt-get install -y nginx openssh-server

RUN mkdir -p /var/run/sshd

WORKDIR /app

COPY target/demo-0.0.1-SNAPSHOT.jar app.jar

COPY nginx.conf /etc/nginx/sites-enabled/default

EXPOSE 8080
EXPOSE 8443
EXPOSE 22

CMD service ssh start && \
    service nginx start && \
    java -jar app.jar