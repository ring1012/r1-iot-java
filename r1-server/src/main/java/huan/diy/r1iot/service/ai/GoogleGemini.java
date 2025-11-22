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
        this.MODEL = "gemini-2.5-flash";
    }

    @Override
    public ChatModel buildModel(Device device) {
        return GoogleAiGeminiChatModel.builder()
                .baseUrl(geminiBaseUrl(device))
                .apiKey(device.getAiConfig().getKey())
                .modelName(StringUtils.hasLength(device.getAiConfig().getModel()) ? device.getAiConfig().getModel() : MODEL)
                .build();
    }

    private String geminiBaseUrl(Device device) {
        String cdn = StringUtils.hasLength(device.getAiConfig().getCdn()) ?
                (device.getAiConfig().getCdn().endsWith("/") ? device.getAiConfig().getCdn() : device.getAiConfig().getCdn() + "/") + "v1beta" : null;
        return cdn!=null?cdn:
                StringUtils.hasLength(device.getAiConfig().getEndpoint()) ?
                        (device.getAiConfig().getEndpoint().endsWith("/") ? device.getAiConfig().getEndpoint() : device.getAiConfig().getEndpoint() + "/") + "v1beta" : null;
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