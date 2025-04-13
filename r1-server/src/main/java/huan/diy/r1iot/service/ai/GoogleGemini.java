package huan.diy.r1iot.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import huan.diy.r1iot.model.Device;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;


@Slf4j
@Service("Gemini")
public class GoogleGemini implements IAIService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper = new ObjectMapper();

    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key="; // 替换为实际的 API URL

    @Override
    public ChatLanguageModel buildModel(Device device) {
        // todo add langchain gemini dependency
        throw new UnsupportedOperationException("Not supported yet.");
    }


    @Override
    public String getAlias() {
        return "Gemini";
    }
}