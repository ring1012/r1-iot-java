package huan.diy.r1iot.direct;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import huan.diy.r1iot.model.Device;
import huan.diy.r1iot.service.ai.IAIService;
import huan.diy.r1iot.service.audio.IAudioService;
import huan.diy.r1iot.service.box.BoxControllerService;
import huan.diy.r1iot.service.news.INewsService;
import huan.diy.r1iot.service.hass.HassServiceImpl;
import huan.diy.r1iot.service.music.IMusicService;
import huan.diy.r1iot.service.radio.IRadioService;
import huan.diy.r1iot.service.weather.IWeatherService;
import huan.diy.r1iot.util.R1IotUtils;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
    private Map<String, INewsService> newsServiceMap;

    @Autowired
    private Map<String, IAudioService> audioServiceMap;

    @Autowired
    private Map<String, IMusicService> musicServiceMap;


    @Autowired
    private Map<String, IWeatherService> weatherServiceMap;

    @Autowired
    private HassServiceImpl hassService;

    @Autowired
    private BoxControllerService boxControllerService;

    @Autowired
    @Qualifier("defaultRadio")
    private IRadioService radioService;

    @Getter
    private Map<String, AiAssistant> assistants = new ConcurrentHashMap<>();


    public static class GuavaChatMemory implements ChatMemory {
        private final Cache<String, List<ChatMessage>> messageCache;
        private final int maxMessages;
        private final String key;

        public GuavaChatMemory(String key, long duration, TimeUnit unit, int maxMessages) {
            this.messageCache = CacheBuilder.newBuilder()
                    .expireAfterWrite(duration, unit)
                    .build();
            this.maxMessages = maxMessages;
            this.key = key;
        }

        @Override
        public List<ChatMessage> messages() {
            List<ChatMessage> cached = messageCache.getIfPresent(key);
            if (cached == null) {
                cached = new ArrayList<>();
            }
            messageCache.put(key, cached);
            return cached;
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
        public void add(ChatMessage message) {
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
        ChatMemory chatMemory = new GuavaChatMemory(deviceId, 2, TimeUnit.MINUTES, Math.max(4, device.getAiConfig().getChatHistoryNum()));
        IAIService aiService = aiServiceMap.get(device.getAiConfig().getChoice());
        ChatModel model = aiService.buildModel(device);
        assistants.put(deviceId, new AiAssistant(model, device.getAiConfig().getSystemPrompt(),
                new BoxDecision(device, musicServiceMap, newsServiceMap, audioServiceMap, weatherServiceMap, hassService, boxControllerService, radioService),
                chatMemory, aiService.isFirstMsg()));
    }

}
