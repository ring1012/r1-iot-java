package huan.diy.r1iot.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import huan.diy.r1iot.service.IWebAlias;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service("Grok")
@Slf4j
public class GrokAiX implements IAIService, IWebAlias {

    private static final String API_URL = "https://api.x.ai/v1/chat/completions";
    private static final String MODEL = "grok-2-latest";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private RestTemplate restTemplate;

    @Override
    public JsonNode buildRequest(String userInput) {
        // 创建请求的JSON结构
        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("model", MODEL);
        requestNode.put("stream", false);
        requestNode.put("temperature", 0);

        // 创建messages数组
        ArrayNode messagesArray = objectMapper.createArrayNode();

        // 添加系统消息
        ObjectNode systemMessage = objectMapper.createObjectNode();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemInfo);
        messagesArray.add(systemMessage);

        // 添加用户消息
        ObjectNode userMessage = objectMapper.createObjectNode();
        userMessage.put("role", "user");
        userMessage.put("content", userInput);
        messagesArray.add(userMessage);

        // 将messages数组添加到请求中
        requestNode.set("messages", messagesArray);

        return requestNode;
    }

    @Override
    public String responseToUser(JsonNode request, String key) {
        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + key);

        // 创建HTTP请求实体
        HttpEntity<JsonNode> httpEntity = new HttpEntity<>(request, headers);

        try{
            // 发送POST请求
            ResponseEntity<JsonNode> responseEntity = restTemplate.exchange(
                    API_URL,
                    HttpMethod.POST,
                    httpEntity,
                    JsonNode.class
            );

            // 检查响应状态码
            if (responseEntity.getStatusCode() == HttpStatus.OK) {
                JsonNode responseBody = responseEntity.getBody();
                if (responseBody != null) {
                    // 从响应中提取choices[0].message.content
                    JsonNode choicesNode = responseBody.path("choices");
                    if (choicesNode.isArray() && !choicesNode.isEmpty()) {
                        JsonNode firstChoice = choicesNode.get(0);
                        JsonNode messageNode = firstChoice.path("message");
                        if (!messageNode.isMissingNode()) {
                            JsonNode contentNode = messageNode.path("content");
                            if (!contentNode.isMissingNode()) {
                                return contentNode.asText();
                            }
                        }
                    }
                }
            }

            // 如果请求失败或未找到内容，返回默认消息
            return "AI服务出错了，返回码："+responseEntity.getStatusCode();
        }catch (Exception e) {
            log.error("responseToUser error", e);
            return "AI服务出错了，返回码：";
        }

    }

    @Override
    public String getAlias() {
        return "Grok";
    }
}