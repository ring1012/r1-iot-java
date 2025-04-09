#!/bin/bash

# 参数检查
if [ "$#" -ne 1 ]; then
    echo "Usage: $0 <serviceId>"
    exit 1
fi

SERVICE_ID=$1

cloudflared service uninstall

# 2. 启动新进程（后台运行，记录PID）
echo "Starting $SERVICE_NAME with serviceId: $SERVICE_ID..."
cloudflared service install "$SERVICE_ID"
