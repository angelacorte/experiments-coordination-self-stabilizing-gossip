FROM eclipse-temurin:25
RUN mkdir -p /experiment
WORKDIR /experiment
ENV GRADLE_TASK=runAllBatch
ENV MAX_SEED=31
COPY effects effects
COPY gradle gradle
COPY src src
COPY docker docker
COPY *.kts ./
COPY *.properties ./
COPY gradlew* ./
RUN sed -i '/alias(libs.plugins.gitSemVer)/d' build.gradle.kts
RUN sed -i '/alias(libs.plugins.multiJvmTesting)/d' build.gradle.kts
RUN sed -i '/multiJvm {/,/}/d' build.gradle.kts
# Only runs with FIND_MAX if it's defined in docker-compose
CMD sh -c 'if [ -n "$FIND_MAX" ]; then \
             FIND_MAX=$FIND_MAX MAX_SEED=$MAX_SEED ./gradlew $GRADLE_TASK; \
           else \
             MAX_SEED=$MAX_SEED ./gradlew $GRADLE_TASK; \
           fi'