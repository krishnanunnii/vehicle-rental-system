FROM eclipse-temurin:11-jdk

WORKDIR /app

COPY lib ./lib
COPY src ./src
COPY web ./web

RUN mkdir -p bin && \
    javac --release 8 -encoding UTF-8 -cp "lib/*" -d bin \
    src/*.java src/model/*.java src/dao/*.java src/servlet/*.java src/util/*.java

EXPOSE 8080

CMD ["sh", "-c", "java -cp 'bin:lib/*' ServerRunner"]
