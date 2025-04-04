package huan.diy.r1iot.direct;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import huan.diy.r1iot.model.Device;
import huan.diy.r1iot.service.ai.IAIService;
import huan.diy.r1iot.service.audio.IAudioService;
import huan.diy.r1iot.service.hass.HassServiceImpl;
import huan.diy.r1iot.service.music.IMusicService;
import huan.diy.r1iot.util.R1IotUtils;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Component
public class AIDirect {

    @Autowired
    private Map<String, IAIService> aiServiceMap;

    @Autowired
    private Map<String, IAudioService> audioServiceMap;

    @Autowired
    private Map<String, IMusicService> musicServiceMap;

    @Autowired
    private HassServiceImpl hassService;

    @Getter
    private Map<String, Assistant> assistants = new ConcurrentHashMap<>();

    static class GuavaChatMemory implements ChatMemory {
        private final Cache<String, List<ChatMessage>> messageCache;
        private final int maxMessages;
        private final String key;

        public GuavaChatMemory(String key, long duration, TimeUnit unit, int maxMessages) {
            this.messageCache = CacheBuilder.newBuilder()
                    .expireAfterWrite(duration, unit) // 8 分钟后自动过期
                    .build();
            this.maxMessages = maxMessages;
            this.key = key;
        }

        @Override
        public List<ChatMessage> messages() {
            return messageCache.getIfPresent(key);
        }

        @Override
        public void clear() {
            messageCache.invalidate(key); // 清除该 key 的缓存
        }

        @Override
        public Object id() {
            return key;
        }

        @Override
        public void add( ChatMessage message) {
            List<ChatMessage> messages = messageCache.getIfPresent(key);
            if (messages == null) {
                messages = new ArrayList<>();
            }
            if (messages.size() >= maxMessages) {
                messages.remove(0); // 超过最大数量时，移除最早的消息
            }
            messages.add(message);
            messageCache.put(key, messages);
        }
    }

    public void upsertAssistant(String deviceId) {
        Device device = R1IotUtils.getDeviceMap().get(deviceId);
        if (device.getAiConfig() == null) {
            return;
        }
        IAIService aiService = aiServiceMap.get(device.getAiConfig().getChoice());
        ChatLanguageModel model = aiService.buildModel(device);
        assistants.put(deviceId, AiServices.builder(Assistant.class)
                .chatLanguageModel(model)
                .tools(new BoxDecision(device, musicServiceMap, audioServiceMap, hassService))
                .chatMemory(new GuavaChatMemory(deviceId, 8, TimeUnit.MINUTES, Math.max(8, device.getAiConfig().getChatHistoryNum())))
                .systemMessageProvider(generateSystemPromptFunc(device.getAiConfig().getSystemPrompt()))
                .build());
    }

    public static Function<Object, String> generateSystemPromptFunc(String systemPrompt) {
        return (context) -> systemPrompt + """
                
                注意：
                不要翻译用户的输入！
                至少一定要选questionAnswer tool！
                """;
    }


}
