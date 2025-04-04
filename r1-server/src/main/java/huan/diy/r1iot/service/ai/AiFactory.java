package huan.diy.r1iot.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import huan.diy.r1iot.model.Device;
import huan.diy.r1iot.model.IotAiResp;
import huan.diy.r1iot.util.R1IotUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class AiFactory {

    @Autowired
    private Map<String, IAIService> aiMap;


    public IotAiResp hassByAi(String deviceId, String userInput, JsonNode hassEntities) {
        Device device = R1IotUtils.getDeviceMap().get(deviceId);
        IAIService aiSvc = aiMap.get(device.getAiConfig().getChoice());

        return aiSvc.askHass(userInput, hassEntities, device.getAiConfig().getKey(), IotAiResp.class);
    }

}
