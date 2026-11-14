# 빌드 단계: Gradle Wrapper로 실행 가능한 jar를 만든다.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
# 의존성 정의가 바뀌지 않으면 의존성 다운로드 레이어를 캐시에서 재사용한다.
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon > /dev/null
COPY src src
RUN ./gradlew bootJar --no-daemon

# 실행 단계: JRE와 jar만 둔다.
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --system app && useradd --system --gid app app
COPY --from=build /app/build/libs/*.jar app.jar
USER app
EXPOSE 8080
# 컨테이너 메모리 제한(mem_limit) 기준으로 힙 크기를 정한다.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -Duser.timezone=Asia/Seoul"
ENTRYPOINT ["java", "-jar", "app.jar"]
