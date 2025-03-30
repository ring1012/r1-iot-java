package huan.diy.r1iot.model;

public enum IntentionEnum {
    CHAT("chat", "cn.yunzhisheng.chat", "聊天"),
    SMART_HOME("smart home", "cn.yunzhisheng.setting", "智能家居"),
    MUSIC("music", "cn.yunzhisheng.music", "播放量音乐"),
    NEWS("news", "cn.yunzhisheng.news", "播放新闻");
    private String name;
    private String serviceName;
    private String description;

    IntentionEnum(String name, String serviceName, String description) {
        this.name = name;
        this.serviceName = serviceName;
        this.description = description;
    }


}
