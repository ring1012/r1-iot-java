package huan.diy.r1iot.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import huan.diy.r1iot.model.Device;
import huan.diy.r1iot.model.IotAiResp;
import huan.diy.r1iot.model.Message;
import huan.diy.r1iot.model.MusicAiResp;
import huan.diy.r1iot.util.R1IotUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class AiFactory {

    @Autowired
    private Map<String, IAIService> aiMap;

    private Cache<String, List<Message>> CACHES = CacheBuilder.newBuilder()
            // 8分钟清理聊天记录
            .expireAfterWrite(8, TimeUnit.MINUTES)
            .build();

    public String responseToUser(String userInput, String deviceId) {
        Device device = R1IotUtils.getDeviceMap().get(deviceId);
        IAIService ai = aiMap.get(device.getAiConfig().getChoice());

        // 获取缓存中的消息列表
        List<Message> msgs = CACHES.getIfPresent(deviceId);
        if (msgs == null) {
            msgs = new LinkedList<>();
        }

        String aiReply = ai.responseToUser(ai.buildRequest(userInput, msgs, device.getAiConfig().getSystemPrompt()),
                device.getAiConfig().getKey());
        // 创建新的消息
        Message newMessage = new Message("user", userInput);
        msgs.add(newMessage);
        msgs.add(new Message("assistant", aiReply));

        // 保证消息列表不超过最大值（LRU 策略）
        if (msgs.size() > device.getAiConfig().getChatHistoryNum()) {
            msgs.remove(0);
            msgs.remove(0);
        }

        CACHES.put(deviceId, msgs);
        return aiReply;
    }

    public IotAiResp hassByAi(String deviceId, String userInput, JsonNode hassEntities) {
        Device device = R1IotUtils.getDeviceMap().get(deviceId);
        IAIService aiSvc = aiMap.get(device.getAiConfig().getChoice());

        return aiSvc.askHass(userInput, hassEntities, device.getAiConfig().getKey(), IotAiResp.class);
    }

    public MusicAiResp musicByAi(String deviceId, String userInput) {
        Device device = R1IotUtils.getDeviceMap().get(deviceId);

        IAIService aiSvc = aiMap.get(device.getAiConfig().getChoice());

        List<Message> messages = new ArrayList<>();
        messages.add(new Message("system", """
                帮忙抽取歌曲的作者author，歌曲的名称musicName，都没有的话，就抽取关键字keyword。抽不到就留空
                """));
        messages.add(new Message("user", userInput));

        return aiSvc.structureResponse(messages, device.getAiConfig().getKey(), MusicAiResp.class);
    }

}
