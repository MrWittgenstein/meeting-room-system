FROM maven:3.9-eclipse-temurin-17 AS api-build
WORKDIR /build
COPY backend/pom.xml pom.xml
COPY backend/src src
COPY deploy/demo/application.yml src/main/resources/application.yml
RUN --mount=type=cache,id=meeting-room-demo-maven,target=/root/.m2/repository,sharing=locked mvn -B -itr -DskipTests package

FROM eclipse-temurin:17-jre-jammy AS api
WORKDIR /app
COPY --from=api-build /build/target/meetroom_management-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]

FROM node:22-bookworm-slim AS web-build
WORKDIR /build
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY frontend/ ./
RUN VITE_API_BASE_URL=/demo-api npm run build

FROM nginx:1.28-alpine AS web
COPY --from=web-build /build/dist/ /usr/share/nginx/html/
COPY deploy/demo/nginx.conf /etc/nginx/conf.d/default.conf
