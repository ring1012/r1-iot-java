#!/bin/bash

# 参数检查
if [ "$#" -ne 1 ]; then
    echo "Usage: $0 <serviceId>"
    exit 1
fi

SERVICE_ID=$1
SERVICE_NAME="cloudflared"
PID_FILE="/tmp/cloudflared.pid"

# 1. 检查并杀死现有进程
if [ -f "$PID_FILE" ]; then
    OLD_PID=$(cat "$PID_FILE")
    if kill -0 "$OLD_PID" > /dev/null 2>&1; then
        echo "Killing existing $SERVICE_NAME (PID: $OLD_PID)..."
        kill -9 "$OLD_PID"
        rm -f "$PID_FILE"
        sleep 1  # 等待进程终止
    fi
fi

# 2. 启动新进程（后台运行，记录PID）
echo "Starting $SERVICE_NAME with serviceId: $SERVICE_ID..."
cloudflared service install "$SERVICE_ID" && \
cloudflared service start > /dev/null 2>&1 &
NEW_PID=$!
echo "$NEW_PID" > "$PID_FILE"
echo "$SERVICE_NAME started with PID: $NEW_PID"