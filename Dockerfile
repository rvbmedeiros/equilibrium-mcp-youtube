# Multi-stage build para otimizar o tamanho da imagem

# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app

# Copiar pom.xml e código fonte
COPY pom.xml .
COPY src ./src

# Compilar (Maven vai baixar dependências automaticamente)
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Criar usuário não-root para segurança
RUN addgroup -g 1001 -S appuser && adduser -u 1001 -S appuser -G appuser

# Copiar JAR do stage de build
COPY --from=build /app/target/*.jar app.jar

# Definir usuário
USER appuser

# Expor porta
EXPOSE 8080

# Variáveis de ambiente (serão sobrescritas no Render)
ENV JAVA_OPTS="-Xmx512m -Xms256m"
ENV SPRING_PROFILES_ACTIVE=prod

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Comando de inicialização
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
