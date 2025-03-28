package huan.diy.r1iot.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import huan.diy.r1iot.model.Message;

import java.util.List;

public interface IAIService {

    JsonNode buildRequest(String userInput, List<Message> history, String systemPrompt);

    String responseToUser(JsonNode request, String key);

    <T> T askHass(String userInput, JsonNode hassEntities, String key, Class<T> clazz);

}
