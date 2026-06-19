# ==================== 构建阶段 ====================
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

WORKDIR /build
COPY pom.xml .

# 使用阿里云Maven镜像加速下载
RUN mkdir -p /root/.m2 && \
    echo '<settings><mirrors><mirror><id>aliyun</id><name>Aliyun Maven</name><url>https://maven.aliyun.com/repository/public</url><mirrorOf>central</mirrorOf></mirror></mirrors></settings>' > /root/.m2/settings.xml

# 先下载依赖（利用Docker缓存层）
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn package -Dmaven.test.skip=true -B

# ==================== 运行阶段 ====================
FROM eclipse-temurin:17-jre-alpine

LABEL maintainer="Mason <admin@zhizhi.ai>"
LABEL description="智知 - AI知识库 SaaS"

WORKDIR /app

# 安装字体（PDF解析需要）
RUN apk add --no-cache fontconfig ttf-dejavu

# 复制构建产物
COPY --from=builder /build/target/*.jar app.jar

# 创建数据目录
RUN mkdir -p /app/data /app/logs

# 暴露端口
EXPOSE 8080

# JVM参数
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
    CMD wget -qO- http://localhost:8080/api/v1/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
