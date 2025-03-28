# 第一阶段：构建 jar 包
FROM azul/zulu-openjdk:17 as builder

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
FROM azul/zulu-openjdk:17

WORKDIR /app

# 从构建阶段复制 jar 文件
COPY --from=builder /workspace/r1-server/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
