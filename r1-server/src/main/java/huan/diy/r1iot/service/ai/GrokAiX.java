package huan.diy.r1iot.service.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import huan.diy.r1iot.anno.AIDescription;
import huan.diy.r1iot.anno.AIEnums;
import huan.diy.r1iot.model.Device;
import huan.diy.r1iot.model.Message;
import huan.diy.r1iot.service.IWebAlias;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

@Service("Grok")
@Slf4j
public class GrokAiX implements IAIService, IWebAlias {

    protected String BASE_URL;
    protected String MODEL;

    public GrokAiX() {
        this.BASE_URL = "https://api.x.ai/v1";
        this.MODEL = "grok-2-latest";
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);  // 全局忽略未知字段
    }

    @Autowired
    private RestTemplate restTemplate;

    @Override
    public JsonNode buildRequest(String userInput, List<Message> history, String systemPrompt) {
        // 创建请求的JSON结构
        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("model", MODEL);
        requestNode.put("stream", false);
        requestNode.put("temperature", 0);

        // 创建messages数组
        ArrayNode messagesArray = objectMapper.createArrayNode();


        // 添加历史消息
        for (Message message : history) {
            ObjectNode historyMessage = objectMapper.createObjectNode();
            historyMessage.put("role", message.getRole()); // 假设Message类有getRole()方法
            historyMessage.put("content", message.getContent()); // 假设Message类有getContent()方法
            messagesArray.add(historyMessage);
        }

        // 添加系统消息
        ObjectNode systemMessage = objectMapper.createObjectNode();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemPrompt);
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

        try {
            // 发送POST请求
            ResponseEntity<JsonNode> responseEntity = restTemplate.exchange(
                    BASE_URL + "/chat/completions",
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
            return "AI服务出错了，返回码：" + responseEntity.getStatusCode();
        } catch (Exception e) {
            log.error("responseToUser error", e);
            return "AI服务出错了，返回码：";
        }

    }

    @Override
    public <T> T structureResponse(List<Message> messages, String key, Class<T> clazz) {
        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("model", MODEL);
        requestNode.put("stream", false);
        requestNode.put("temperature", 0);
        ArrayNode messagesArray = objectMapper.valueToTree(messages);

        requestNode.set("messages", messagesArray);

        ObjectNode formatNode = objectMapper.createObjectNode();

        for (Field field : clazz.getDeclaredFields()) {
            ObjectNode fieldNode = objectMapper.createObjectNode();

            // 处理AIDescription注解
            AIDescription desc = field.getAnnotation(AIDescription.class);
            if (desc != null) {
                fieldNode.put("type", desc.type());
                fieldNode.put("description", desc.value());
            } else {
                fieldNode.put("type", "string");
            }

            // 处理AIEnum注解
            AIEnums enumAnnotation = field.getAnnotation(AIEnums.class);
            if (enumAnnotation != null && enumAnnotation.value().length > 0) {
                fieldNode.putPOJO("enum", enumAnnotation.value());
            }

            formatNode.set(field.getName(), fieldNode);
        }
        ObjectNode schemaNode = objectMapper.createObjectNode();
        schemaNode.put("type", "json_object");
        schemaNode.set("schema", formatNode);
        requestNode.set("response_format", schemaNode);
        String aiReply = responseToUser(requestNode, key);
        try {
            return objectMapper.readValue(aiReply, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ChatLanguageModel buildModel(Device device) {
        return OpenAiChatModel.builder()
                .baseUrl(BASE_URL)
                .apiKey(device.getAiConfig().getKey())
                .modelName(MODEL)
                .strictTools(false)
                .build();
    }

    @Override
    public <T> T askHass(String userInput, JsonNode hassEntities, String key, Class<T> clazz) {
        List<Message> messages = new ArrayList<>();
        messages.add(new Message("system", hassEntities.toString()));
        messages.add(new Message("system", """
                从用户输入中提取以下信息：
                1. 动作（action）：用户想要执行的操作，如打开、关闭、调节亮度等。 打开就是ON，关闭就是OFF，查询就是QUERY，设定就是SET。
                2. 实体ID（entityId）：与动作相关的实体的ID。
                3. 其他信息（其他）：用户提供的其他信息，如温度、亮度等。
                """));
        messages.add(new Message("user", userInput));
        return structureResponse(messages, key, clazz);
    }


    @Override
    public String getAlias() {
        return "Grok";
    }
}