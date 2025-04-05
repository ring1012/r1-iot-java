package huan.diy.r1iot.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.langchain4j.model.chat.ChatLanguageModel;
import huan.diy.r1iot.model.Device;
import huan.diy.r1iot.model.Message;
import huan.diy.r1iot.service.IWebAlias;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service("Gemini")
public class GoogleGemini implements IAIService, IWebAlias {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper = new ObjectMapper();

    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key="; // 替换为实际的 API URL

    @Override
    public JsonNode buildRequest(String userInput, List<Message> history, String systemPrompt) {
        // 创建 JSON 请求体
        ObjectNode requestBody = objectMapper.createObjectNode();
        ArrayNode contents = objectMapper.createArrayNode();

        for (Message message : history) {
            ObjectNode historyMessage = objectMapper.createObjectNode();
            historyMessage.put("role", message.getRole()); // 假设 Message 类有 getRole() 方法
            ArrayNode historyParts = objectMapper.createArrayNode();
            historyParts.add(objectMapper.createObjectNode().put("text", message.getContent())); // 假设 Message 类有 getContent() 方法
            historyMessage.set("parts", historyParts);
            contents.add(historyMessage);
        }

        // 添加用户消息
        ObjectNode userMessage = objectMapper.createObjectNode();
        userMessage.put("role", "user");
        ArrayNode userParts = objectMapper.createArrayNode();
        userParts.add(objectMapper.createObjectNode().put("text", userInput));
        userMessage.set("parts", userParts);
        contents.add(userMessage);

        // 添加模型消息（系统提示）
        ObjectNode modelMessage = objectMapper.createObjectNode();
        modelMessage.put("role", "model");
        ArrayNode modelParts = objectMapper.createArrayNode();
        modelParts.add(objectMapper.createObjectNode().put("text", systemPrompt));
        modelMessage.set("parts", modelParts);
        contents.add(modelMessage);

        // 将 contents 添加到请求体中
        requestBody.set("contents", contents);

        return requestBody;
    }

    @Override
    public String responseToUser(JsonNode request, String key) {
        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        // 创建 HttpEntity，包含请求头和请求体
        HttpEntity<JsonNode> requestEntity = new HttpEntity<>(request, headers);

        // 发送 POST 请求
        ResponseEntity<JsonNode> responseEntity = restTemplate.exchange(
                API_URL+key,
                HttpMethod.POST,
                requestEntity,
                JsonNode.class
        );

        // 处理响应
        if (responseEntity.getStatusCode() == HttpStatus.OK) {
            try {
                // 解析响应 JSON
                JsonNode responseJson = responseEntity.getBody();
                // 提取助手的回复
                String reply = responseJson.path("candidates")
                        .path(0) // 第一个候选回复
                        .path("content")
                        .path("parts")
                        .path(0) // 第一个部分
                        .path("text")
                        .asText().trim(); // 提取文本内容
                log.info("AI reply: {}", reply);
                return reply;
            } catch (Exception e) {
                log.error("error: ",e);
                return "AI服务出错了，返回码：" + responseEntity.getStatusCode().value() ;
            }
        } else {
            log.error("call ai failed: {}", responseEntity.getStatusCode());
            return "AI服务出错了，返回码：" + responseEntity.getStatusCode().value() ;
        }
    }

    @Override
    public <T> T askHass(String userInput, JsonNode hassEntities, String key, Class<T> clazz) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public <T> T structureResponse(List<Message> messages, String key, Class<T> clazz) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ChatLanguageModel buildModel(Device device) {
        throw new UnsupportedOperationException("Not supported yet.");
    }


    @Override
    public String getAlias() {
        return "Gemini";
    }
}