package huan.diy.r1iot.service.ai;

import com.fasterxml.jackson.databind.JsonNode;

public interface IAIService {

    String systemInfo = "你是一个智能音箱，简短回答问题。";

    JsonNode buildRequest(String userInput);

    String responseToUser(JsonNode request);

}
