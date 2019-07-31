FROM openjdk:8-alpine

COPY target/uberjar/technobabble.jar /technobabble/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/technobabble/app.jar"]
