FROM ubuntu-22-jdk-17-djl-0.21-cpu:v1.0
LABEL maintainer=hongcai
ENV PATH /root/lhc-dev/jdk-17.0.5/bin:${PATH}
COPY target/edge-controller-1.0-SNAPSHOT.jar   /root/app.jar
WORKDIR /root
ENTRYPOINT ["sh","-c","java -jar app.jar"]