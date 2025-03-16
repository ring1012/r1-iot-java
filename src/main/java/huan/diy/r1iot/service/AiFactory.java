package huan.diy.r1iot.service;

import huan.diy.r1iot.service.ai.GoogleGemini;
import huan.diy.r1iot.service.ai.GrokAiX;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AiFactory {

    @Autowired
    private GoogleGemini googleGemini;

    @Autowired
    private GrokAiX grokAiX;

    public String responseToUser(String userInput) {
        return grokAiX.responseToUser(grokAiX.buildRequest(userInput));
    }

}
