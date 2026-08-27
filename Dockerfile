FROM eclipse-temurin:11-jdk

WORKDIR /app

COPY src ./src
COPY web ./web
COPY lib ./lib

RUN mkdir -p target/classes && \
    find src -name "*.java" > sources.txt && \
    javac -cp "lib/*" -d target/classes @sources.txt

EXPOSE 10000

CMD ["sh", "-c", "java -cp 'target/classes:lib/*' ServerRunner"]
