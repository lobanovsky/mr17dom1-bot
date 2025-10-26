FROM gradle:8.13-jdk21 AS builder

WORKDIR /app
COPY . .
RUN gradle shadowJar --no-daemon


FROM markhobson/maven-chrome:jdk-21

WORKDIR /app

COPY --from=builder /app/build/libs/mr17dom1-bot-all.jar app.jar

VOLUME ["/app/data"]

CMD ["java", "-jar", "app.jar"]
