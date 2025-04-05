# 第一阶段：构建 jar 包
FROM eclipse-temurin:17.0.14_7-jdk as builder

ENV MAVEN_VERSION=3.9.9
ENV MAVEN_HOME=/opt/maven
ENV PATH=${MAVEN_HOME}/bin:${PATH}

RUN apt-get update && \
    apt-get install -y curl tar && \
    curl -fsSL https://downloads.apache.org/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz -o /tmp/maven.tar.gz && \
    mkdir -p ${MAVEN_HOME} && \
    tar -xzf /tmp/maven.tar.gz -C ${MAVEN_HOME} --strip-components=1 && \
    rm -rf /tmp/maven.tar.gz && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /workspace
COPY . .

# 构建项目，生成 jar
RUN mvn clean package -DskipTests

# 第二阶段：运行 jar 包
FROM eclipse-temurin:17.0.14_7-jdk

WORKDIR /app

# 安装基础工具和ffmpeg
RUN apt-get update && \
    apt-get install -y wget ffmpeg ca-certificates && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# 根据系统架构下载对应的 yt-dlp 二进制
RUN ARCH=$(uname -m) && \
    echo "检测到系统架构: $ARCH" && \
    case "$ARCH" in \
        "x86_64") \
            URL="https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp_linux" \
            ;; \
        "armv7l"|"armhf") \
            URL="https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp_linux_armv7l" \
            ;; \
        "aarch64"|"arm64") \
            URL="https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp_linux_aarch64" \
            ;; \
        *) \
            echo "不支持的架构: $ARCH"; exit 1 \
            ;; \
    esac && \
    echo "下载URL: $URL" && \
    wget "$URL" -O /usr/local/bin/yt-dlp && \
    chmod a+rx /usr/local/bin/yt-dlp && \
    yt-dlp --version

# 从构建阶段复制 jar 文件
COPY --from=builder /workspace/r1-server/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]