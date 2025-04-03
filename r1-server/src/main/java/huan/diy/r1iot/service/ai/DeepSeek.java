package huan.diy.r1iot.service.ai;

import org.springframework.stereotype.Service;


@Service("DeepSeek")
public class DeepSeek extends GrokAiX {

    public DeepSeek() {
        this.BASE_URL = "https://api.deepseek.com";
        this.MODEL = "deepseek-chat";
    }

    @Override
    public String getAlias() {
        return "DeepSeek";
    }
}
