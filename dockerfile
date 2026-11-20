FROM node:20-alpine AS frontend
WORKDIR /build
COPY package.json package-lock.json ./
RUN npm ci
COPY public public
COPY src src
COPY babel.config.js vue.config.js .eslintrc.js .browserslistrc ./
RUN npm run build

FROM maven:3.9-eclipse-temurin-17 AS backend
WORKDIR /build
COPY backend/pom.xml backend/pom.xml
COPY backend/src backend/src
COPY --from=frontend /build/dist backend/src/main/resources/static
RUN mvn -f backend/pom.xml -B package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=backend /build/backend/target/backend-1.0.0.jar app.jar
COPY public/data /app/seed/data
COPY public/images /app/seed/images
ENV PORT=3000 MENU_SEED_DIR=/app/seed MENU_DATA_DIR=/app/storage
EXPOSE 3000
ENTRYPOINT ["java", "-jar", "/app/app.jar"]