package huan.diy.r1iot.service.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Service("Grok")
@Slf4j
public class GrokAiX extends OpenAi {

    public GrokAiX() {
        this.BASE_URL = "https://api.x.ai/v1";
        this.MODEL = "grok-2-latest";
    }



    @Override
    public boolean isFirstMsg() {
        return false;
    }


    @Override
    public String getAlias() {
        return "Grok";
    }
}