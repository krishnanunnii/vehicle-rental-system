FROM eclipse-temurin:11-jdk

WORKDIR /app

COPY pom.xml .
COPY src ./src
COPY web ./web
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

RUN mvn dependency:copy-dependencies -DincludeScope=provided -DoutputDirectory=target/dependency

RUN find src -name "*.java" > sources.txt && \
    javac -cp "target/dependency/*" -d target/classes @sources.txt

EXPOSE 10000

CMD ["sh", "-c", "java -cp 'target/classes:target/dependency/*' ServerRunner"]
