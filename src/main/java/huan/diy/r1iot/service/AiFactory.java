package huan.diy.r1iot.service;

import huan.diy.r1iot.service.impl.GoogleGemini;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AiFactory {

    @Autowired
    private GoogleGemini googleGemini;

    public String responseToUser(String userInput) {
        return googleGemini.responseToUser(googleGemini.buildRequest(userInput));
    }

}
