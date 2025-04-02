package huan.diy.r1iot.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import huan.diy.r1iot.model.Device;
import huan.diy.r1iot.model.MusicAiResp;
import huan.diy.r1iot.service.IR1Service;
import huan.diy.r1iot.service.ai.AiFactory;
import huan.diy.r1iot.service.music.IMusicService;
import huan.diy.r1iot.util.R1IotUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service("cn.yunzhisheng.music")
public class MusicServiceImpl implements IR1Service {

    @Autowired
    private AiFactory aiFactory;

    @Autowired
    private Map<String, IMusicService> musicServiceMap;


    private static final ObjectMapper objectMapper;

    private static final ObjectNode intent;

    static{
        objectMapper = new ObjectMapper();
        intent = objectMapper.createObjectNode();

        ObjectNode operationsObj = objectMapper.createObjectNode();
        ArrayNode operations = objectMapper.createArrayNode();
        operationsObj.set("operations", operations);
        intent.set("intent", operationsObj);
        ObjectNode operator = objectMapper.createObjectNode();
        operations.add(operator);

        operator.put("operator", "ACT_PLAY");

    }

    @Override
    public JsonNode replaceOutPut(JsonNode jsonNode, String deviceId) {


        String uerInput = jsonNode.get("text").asText();
        MusicAiResp musicAiResp = aiFactory.musicByAi(deviceId, uerInput);

        Device device = R1IotUtils.getDeviceMap().get(deviceId);
        IMusicService musicService = musicServiceMap.get(device.getMusicConfig().getChoice());

        JsonNode musicResp = musicService.fetchMusics(musicAiResp, device);
        ObjectNode ret = ((ObjectNode) jsonNode);
        ret.set("data", musicResp);
        ret.set("semantic", intent);
        ret.put("code", "SETTING_EXEC");
        ret.put("matchType", "FUZZY");

        ObjectNode general = objectMapper.createObjectNode();
        general.put("text", "好的，已为您播放");
        general.put("type", "T");
        ret.set("general", general);

        ret.remove("taskName");

        return jsonNode;
    }
}
