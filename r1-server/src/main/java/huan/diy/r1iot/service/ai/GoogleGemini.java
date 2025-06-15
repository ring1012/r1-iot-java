package huan.diy.r1iot.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import huan.diy.r1iot.model.Device;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;


@Slf4j
@Service("Gemini")
public class GoogleGemini implements IAIService {

    protected String MODEL;

    public GoogleGemini() {
        this.MODEL = "gemini-2.5-flash-preview-05-20";
    }

    @Override
    public ChatModel buildModel(Device device) {

        return GoogleAiGeminiChatModel.builder()
                .apiKey(device.getAiConfig().getKey())
                .modelName(MODEL)
                .build();
    }

    @Override
    public boolean isFirstMsg() {
        return true;
    }


    @Override
    public String getAlias() {
        return "Gemini";
    }
}