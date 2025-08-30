# Java 17 bilan Spring Boot run qilish uchun
FROM eclipse-temurin:17-jdk-alpine

# App uchun papka
WORKDIR /app

# Maven orqali build qilingan jar faylni image ichiga ko‘chir
COPY target/*.jar app.jar

# Applicationni run qilamiz
ENTRYPOINT ["java","-jar","app.jar"]