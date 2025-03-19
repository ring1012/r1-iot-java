package huan.diy.r1iot.service.ai;

import huan.diy.r1iot.model.Device;
import huan.diy.r1iot.util.R1IotUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class AiFactory {

    @Autowired
    private Map<String, IAIService> aiMap;

    public String responseToUser(String userInput) {

        Device device = R1IotUtils.getDeviceMap().get(R1IotUtils.getCurrentDeviceId());
        IAIService ai = aiMap.get(device.getAiConfig().getChoice());

        return ai.responseToUser(ai.buildRequest(userInput), device.getAiConfig().getKey());
    }

}
