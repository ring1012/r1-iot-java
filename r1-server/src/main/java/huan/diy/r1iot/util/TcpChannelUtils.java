package huan.diy.r1iot.util;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

public class TcpChannelUtils {
    public static final AttributeKey<Channel> REMOTE_CHANNEL = AttributeKey.valueOf("REMOTE_CHANNEL");
    public static final AttributeKey<Channel> CLIENT_CHANNEL = AttributeKey.valueOf("CLIENT_CHANNEL");
    public static final AttributeKey<String> DEVICE_ID = AttributeKey.valueOf("DEVICE_ID");
    public static final AttributeKey<Boolean> END = AttributeKey.valueOf("END");
    public static final AttributeKey<String> CLIENT_IP = AttributeKey.valueOf("CLIENT_IP");
    // 远程服务器地址
    public static final String REMOTE_HOST = "39.105.252.245";  // 目标服务器 IP
    public static final int REMOTE_PORT = 80;  // 远程服务器的端口
}