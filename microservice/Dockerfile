FROM amazoncorretto:21-alpine3.20-jdk AS builder
WORKDIR /builder
RUN apk --no-cache add maven

COPY pom.xml ./

COPY . .

RUN mvn -B -e clean install -DskipTests=true

RUN cp ./target/*.jar ./application.jar

RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted

FROM bellsoft/liberica-openjre-debian:21-cds
WORKDIR /application

COPY --from=builder /builder/extracted/dependencies/ ./
COPY --from=builder /builder/extracted/spring-boot-loader/ ./
COPY --from=builder /builder/extracted/snapshot-dependencies/ ./
COPY --from=builder /builder/extracted/application/ ./

RUN adduser --system --group spring

USER spring:spring

ENTRYPOINT ["java", "-XX:SharedArchiveFile=application.jsa", "-jar", "application.jar"]