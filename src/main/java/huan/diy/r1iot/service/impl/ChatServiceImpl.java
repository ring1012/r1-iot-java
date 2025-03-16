package huan.diy.r1iot.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import huan.diy.r1iot.service.AiFactory;
import huan.diy.r1iot.service.IR1Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("cn.yunzhisheng.chat")
public class ChatServiceImpl implements IR1Service {

    @Autowired
    private AiFactory aiFactory;

    @Override
    public JsonNode replaceOutPut(JsonNode jsonNode) {
        String userInput = jsonNode.get("text").asText();
        String reply = aiFactory.responseToUser(userInput);

        ObjectNode generalNode = (ObjectNode) jsonNode.path("general");
        generalNode.put("text", reply);
        generalNode.put("style", "CQA_common_customized");
        generalNode.put("resourceId", "904754");
        ((ObjectNode) jsonNode).put("matchType", "NOT_UNDERSTAND");
        return jsonNode;
    }
}
