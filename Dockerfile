# 1-bosqich: Maven yordamida .jar faylni build qilish
FROM maven:3.8.1-openjdk-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# 2-bosqich: Faqat .jarni ishga tushuradigan engil image
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]