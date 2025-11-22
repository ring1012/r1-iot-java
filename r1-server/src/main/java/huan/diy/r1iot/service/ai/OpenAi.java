package huan.diy.r1iot.service.ai;


import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import huan.diy.r1iot.model.Device;
import huan.diy.r1iot.service.IWebAlias;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;



@Service("OpenAi")
@Slf4j
@Data
public class OpenAi implements IAIService, IWebAlias {

    protected String BASE_URL;
    protected String MODEL;

    @Override
    public ChatModel buildModel(Device device) {
        return OpenAiChatModel.builder()
                .baseUrl(openaiBaseUrl(device))
                .apiKey(device.getAiConfig().getKey())
                .modelName(StringUtils.isEmpty(MODEL) ? device.getAiConfig().getModel() : MODEL)
                .strictTools(false)
                .build();
    }

    private String openaiBaseUrl(Device device) {
        if(StringUtils.isNoneEmpty(device.getAiConfig().getCdn())){
            return device.getAiConfig().getCdn();
        }
        return StringUtils.isEmpty(BASE_URL) ? device.getAiConfig().getEndpoint() : BASE_URL;
    }


    @Override
    public boolean isFirstMsg() {
        return false;
    }

    @Override
    public String getAlias() {
        return "OpenAi接口";
    }
}

