package huan.diy.r1iot.service.hass;

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
import huan.diy.r1iot.util.R1IotUtils;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.similarity.LevenshteinDistance;
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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class HassServiceImpl {

    private static final Set<String> WHITE_LIST_PREFIX = Set.of("sensor", "automation", "switch", "light", "climate");

    private static final ScheduledExecutorService refreshExecutor = Executors.newSingleThreadScheduledExecutor();

    @Autowired
    private RestTemplate restTemplateTemp;

    private static final ObjectMapper objectMapper = R1IotUtils.getObjectMapper();

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

    public String controlHass(String target, String parameter, String actValue, Device device) {
        try {
            String deviceId = device.getId();
            String entityId = findHassEntity(target, HASS_CACHE.get(deviceId));
            IotAiResp aiIot = new IotAiResp(entityId, actValue, parameter);

            String ttsContent = "SUCCESS";

            String action = aiIot.getAction().trim().toLowerCase();
            switch (action) {
                case "on":
                    switchOperation(deviceId, aiIot.getEntityId(), true);
                    break;

                case "off":
                    switchOperation(deviceId, aiIot.getEntityId(), false);
                    break;
                case "query":
                    ttsContent = queryStatus(deviceId, aiIot.getEntityId());
                    break;
                case "set":
                    // todo
                default:

            }

            return ttsContent;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private String findHassEntity(String target, JsonNode jsonNode) {

        JsonNode mostSimilarChannel = null;
        int minDistance = Integer.MAX_VALUE;

        LevenshteinDistance levenshtein = new LevenshteinDistance();

        for (JsonNode entity : jsonNode) {
            String tvgName = entity.get("name").asText();

            // 计算编辑距离（越小越相似）
            int distance = levenshtein.apply(target, tvgName);

            if (distance < minDistance) {
                minDistance = distance;
                mostSimilarChannel = entity;
            }
        }


        return mostSimilarChannel.get("entity_id").textValue();

    }

    private void switchOperation(String deviceId, String entityId, boolean on) {
        new Thread(() -> {

            if (entityId.startsWith("light")) {
                JsonNode resp = stateQuery(deviceId, entityId);
                String val = resp.get("state").textValue();
                if (val.equals("on") && on) {
                    return;
                }
                if (val.equals("off") && !on) {
                    return;
                }
            }
            String url = R1IotUtils.getDeviceMap().get(deviceId).getHassConfig().getEndpoint();
            url = url.endsWith("/") ? url : (url + "/");
            url = buildOperationUrl(entityId, url, on);

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

    private String buildOperationUrl(String entityId, String url, boolean on) {
        if (entityId.startsWith("switch")) {
            return url + "api/services/switch/" + (on ? "turn_on" : "turn_off");
        } else if (entityId.startsWith("light")) {
            return url + "api/services/light/toggle";
        }
        return null;
    }

    private String queryStatus(String deviceId, String entityId) {
        JsonNode resp = stateQuery(deviceId, entityId);
        String name = resp.get("attributes").get("friendly_name").textValue();
        String val = resp.get("state").textValue();
        return name + "是" + val;
    }

    private JsonNode stateQuery(String deviceId, String entityId) {
        String url = R1IotUtils.getDeviceMap().get(deviceId).getHassConfig().getEndpoint();
        url = url.endsWith("/") ? url : (url + "/") + "api/states/" + entityId;
        Device device = R1IotUtils.getDeviceMap().get(deviceId);
        Device.HASSConfig hassConfig = device.getHassConfig();

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
        return response.getBody();
    }

}
