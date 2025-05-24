package huan.diy.r1iot.service.ai;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import huan.diy.r1iot.model.Device;
import huan.diy.r1iot.service.IWebAlias;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;


@Service("Grok")
@Slf4j
public class GrokAiX implements IAIService, IWebAlias {
    protected String BASE_URL;
    protected String MODEL;

    public GrokAiX() {
        this.BASE_URL = "https://api.x.ai/v1";
        this.MODEL = "grok-2-latest";
    }

    @Autowired
    private RestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);  // 全局忽略未知字段
    }




//    @Override
//    public <T> T structureResponse(List<Message> messages, String key, Class<T> clazz) {
//        ObjectNode requestNode = objectMapper.createObjectNode();
//        requestNode.put("model", MODEL);
//        requestNode.put("stream", false);
//        requestNode.put("temperature", 0);
//        ArrayNode messagesArray = objectMapper.valueToTree(messages);
//
//        requestNode.set("messages", messagesArray);
//
//        ObjectNode formatNode = objectMapper.createObjectNode();
//
//        for (Field field : clazz.getDeclaredFields()) {
//            ObjectNode fieldNode = objectMapper.createObjectNode();
//
//            // 处理AIDescription注解
//            AIDescription desc = field.getAnnotation(AIDescription.class);
//            if (desc != null) {
//                fieldNode.put("type", desc.type());
//                fieldNode.put("description", desc.value());
//            } else {
//                fieldNode.put("type", "string");
//            }
//
//            // 处理AIEnum注解
//            AIEnums enumAnnotation = field.getAnnotation(AIEnums.class);
//            if (enumAnnotation != null && enumAnnotation.value().length > 0) {
//                fieldNode.putPOJO("enum", enumAnnotation.value());
//            }
//
//            formatNode.set(field.getName(), fieldNode);
//        }
//        ObjectNode schemaNode = objectMapper.createObjectNode();
//        schemaNode.put("type", "json_object");
//        schemaNode.set("schema", formatNode);
//        requestNode.set("response_format", schemaNode);
//        String aiReply = responseToUser(requestNode, key);
//        try {
//            return objectMapper.readValue(aiReply, clazz);
//        } catch (JsonProcessingException e) {
//            throw new RuntimeException(e);
//        }
//    }

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
    public String getAlias() {
        return "Grok";
    }
}