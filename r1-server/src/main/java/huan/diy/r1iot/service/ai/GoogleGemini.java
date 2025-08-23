package huan.diy.r1iot.service.ai;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import huan.diy.r1iot.model.Device;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;


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
                .baseUrl(StringUtils.hasLength(device.getAiConfig().getEndpoint()) ?
                        (device.getAiConfig().getEndpoint().endsWith("/") ? device.getAiConfig().getEndpoint() : device.getAiConfig().getEndpoint() + "/") + "v1beta" : null)
                .apiKey(device.getAiConfig().getKey())
                .modelName(StringUtils.hasLength(device.getAiConfig().getModel()) ? device.getAiConfig().getModel() : MODEL)
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