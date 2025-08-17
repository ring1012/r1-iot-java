# 使用官方Python运行时作为基础镜像
FROM python:3.11-slim

# 设置工作目录
WORKDIR /app

# 安装所需的包和 cron
RUN apt-get update && \
    apt-get install -y --no-install-recommends ffmpeg cron && \
    rm -rf /var/lib/apt/lists/* && \
    pip install --no-cache-dir flask yt-dlp

RUN pip install -r requirements.txt
# 暴露端口8080
EXPOSE 8080

# 定义环境变量
ENV FLASK_APP=app.py
ENV FLASK_RUN_HOST=::

# 设置定时任务每天16点更新 yt-dlp
RUN echo "0 16 * * * root pip install -U yt-dlp >> /var/log/yt-dlp-update.log 2>&1" > /etc/cron.d/yt-dlp-cron && \
    chmod 644 /etc/cron.d/yt-dlp-cron && \
    crontab /etc/cron.d/yt-dlp-cron


# 将当前目录内容复制到容器的/app目录下
COPY . /app
COPY youtube.txt /root/youtube.txt


# 运行应用
CMD ["flask", "run", "--host", "::", "--port", "8080"]
