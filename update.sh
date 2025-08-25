#!/bin/sh
# curl -sSL https://raw.githubusercontent.com/ring1012/r1-iot-java/refs/heads/master/update.sh | sh


# 停止并删除现有容器
docker stop r1 >/dev/null 2>&1
docker rm r1 >/dev/null 2>&1

# 获取用户输入 - 路径
echo -n "请输入存储路径（默认/root/r1-iot/）: "
read user_path < /dev/tty
data_path="${user_path:-/root/r1-iot/}"

# 获取用户输入 - 密码
echo -n "请输入密码（默认123456）: "
read user_pw < /dev/tty
password="${user_pw:-123456}"

# 显示用户选择
echo ""
echo "=== 配置确认 ==="
echo "存储路径: $data_path"
echo "密码    : $password"
echo "================="
echo ""

# 运行容器
echo "正在启动容器..."
docker run \
  --pull=always \
  --restart=always \
  --name="r1" \
  -it -d \
  --network=host \
  -e password="$password" \
  -v "$data_path:/root/.r1-iot" \
  registry.cn-hangzhou.aliyuncs.com/ring1012/r1

# 检查容器状态
if docker ps --filter "name=r1" | grep -q "r1"; then
  echo "✅ 容器启动中，预计1分钟"
  docker images \
  | awk '$1=="registry.cn-hangzhou.aliyuncs.com/ring1012/r1" && $2=="<none>" {print $3}' \
  | xargs -r docker rmi -f
else
  echo "❌ 容器启动失败，请检查日志"
  docker logs r1
fi