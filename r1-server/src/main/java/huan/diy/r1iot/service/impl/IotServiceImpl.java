package huan.diy.r1iot.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import huan.diy.r1iot.model.Device;
import huan.diy.r1iot.model.IotAiResp;
import huan.diy.r1iot.service.IR1Service;
import huan.diy.r1iot.service.ai.AiFactory;
import huan.diy.r1iot.util.R1IotUtils;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service("cn.yunzhisheng.setting")
@Slf4j
public class IotServiceImpl implements IR1Service {

    private static final Set<String> WHITE_LIST_PREFIX = Set.of("sensor", "automation", "switch", "light", "climate");

    private static final ScheduledExecutorService refreshExecutor = Executors.newSingleThreadScheduledExecutor();

    @Autowired
    private RestTemplate restTemplateTemp;

    @Autowired
    private AiFactory aiFactory;

    private static RestTemplate restTemplate;

    @PostConstruct
    public void init() {
        restTemplate = this.restTemplateTemp;
    }

    private static final LoadingCache<String, JsonNode> HASS_CACHE = CacheBuilder.newBuilder()
            .refreshAfterWrite(10, TimeUnit.MINUTES) // 每次访问，若数据超过10分钟则异步刷新
            .build(new CacheLoader<>() {
                @Override
                public JsonNode load(String deviceId) {
                    return fetchFromApi(deviceId);
                }

                @Override
                public ListenableFuture<JsonNode> reload(String deviceId, JsonNode oldValue) {
                    return Futures.submit(() -> fetchFromApi(deviceId), refreshExecutor);
                }
            });

    private static JsonNode fetchFromApi(String deviceId) {
        Device device = R1IotUtils.getDeviceMap().get(deviceId);
        Device.HASSConfig hassConfig = device.getHassConfig();
        String url = hassConfig.getEndpoint();
        url = (url.endsWith("/") ? url : (url + "/")) + "api/states";
        String token = hassConfig.getToken();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token.trim());
        HttpEntity<JsonNode> entity = new HttpEntity<>(headers);
        ResponseEntity<JsonNode> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                JsonNode.class
        );
        JsonNode node = response.getBody();
        return filterEntities(node);

    }

    private static JsonNode filterEntities(JsonNode node) {
        ObjectMapper objectMapper = new ObjectMapper();
        ArrayNode filteredEntities = objectMapper.createArrayNode();  // 创建一个数组节点，用来存储过滤后的实体

        for (JsonNode entity : node) {
            // 获取 entity_id 前缀部分
            String entityPrefix = entity.get("entity_id").textValue().split("\\.")[0];

            // 获取友好名称
            String friendlyName = entity.get("attributes").get("friendly_name").textValue();

            if (StringUtils.hasLength(friendlyName) && WHITE_LIST_PREFIX.contains(entityPrefix)) {
                ObjectNode filteredEntity = objectMapper.createObjectNode();

                // 假设我们需要返回 "entity_id" 和 "name"
                filteredEntity.put("entity_id", entity.get("entity_id").textValue());
                filteredEntity.put("name", friendlyName);  // 设置为 friendly_name 或根据需要修改

                // 将过滤后的实体添加到结果数组
                filteredEntities.add(filteredEntity);
            }
        }

        return filteredEntities;  // 返回过滤后的实体列表
    }

    @Override
    public JsonNode replaceOutPut(JsonNode input, String deviceId) {

        String asrText = input.get("text").asText();
        IotAiResp aiIot;
        try {
            aiIot = aiFactory.hassByAi(deviceId, asrText, HASS_CACHE.get(deviceId));
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }

        String action = aiIot.getAction().trim().toLowerCase();
        switch (action) {
            case "on":
                switchOperation(deviceId, aiIot.getEntityId(), true);
                break;
            case "off":
                switchOperation(deviceId, aiIot.getEntityId(), false);
                break;
            case "query":
                break;
            case "set":
                // todo
            default:

        }

        return input;
    }

    private void switchOperation(String deviceId, String entityId, boolean on) {
        new Thread(() -> {
            String operation = on ? "turn_on" : "turn_off";
            String url = R1IotUtils.getDeviceMap().get(deviceId).getHassConfig().getEndpoint();
            url = url.endsWith("/") ? url : (url + "/") + "api/services/switch/" + operation;
            Map<String, String> entityMap = Map.of("entity_id", entityId);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + R1IotUtils.getDeviceMap().get(deviceId).getHassConfig().getToken());
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(entityMap, headers);
            ResponseEntity<String> exchange = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );
            log.info("iot 执行HTTP 返回码：{}", exchange.getStatusCode().toString());

        }).start();


    }

}
