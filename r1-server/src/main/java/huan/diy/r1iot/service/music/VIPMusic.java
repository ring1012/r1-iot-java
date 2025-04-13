package huan.diy.r1iot.service.music;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import huan.diy.r1iot.model.Device;
import huan.diy.r1iot.model.MusicAiResp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;


@Service("VIP")
@Slf4j
public class VIPMusic implements IMusicService {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RestTemplate restTemplate;

    @Override
    public JsonNode fetchMusics(MusicAiResp musicAiResp, Device device) {

        StringBuilder sb = new StringBuilder();
        sb.append(StringUtils.hasLength(musicAiResp.getAuthor()) ? musicAiResp.getAuthor() : "");
        sb.append(" ");
        sb.append(StringUtils.hasLength(musicAiResp.getMusicName()) ? musicAiResp.getMusicName() : "");
        String keyword = sb.toString().trim();
        if (keyword.isEmpty()) {
            keyword = musicAiResp.getKeyword();
        }
        JsonNode searchRet = searchByKeyword(keyword, device);
        ArrayNode arrayNode = (ArrayNode) searchRet.get("data");

        ArrayNode musicInfo = objectMapper.createArrayNode();
        for (JsonNode node : arrayNode) {
            try {
                ObjectNode music = objectMapper.createObjectNode();
                Long id = node.get("id").asLong();
                music.put("id", id);
                music.put("title", node.get("name").asText());
                music.put("artist", node.get("artists").get(0).get("name").asText());
                music.put("album", node.get("album").get("name").asText());
                music.put("url", node.get("url").asText());
                musicInfo.add(music);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }

        }


        ObjectNode result = objectMapper.createObjectNode();

        ObjectNode ret = objectMapper.createObjectNode();
        ret.put("count", arrayNode.size());
        ret.set("musicinfo", musicInfo);
        ret.put("pagesize", String.valueOf(arrayNode.size()));
        ret.put("errorCode", 0);
        ret.put("page", "1");
        ret.put("source", 1);

        result.set("result", ret);

        return result;
    }


    public JsonNode searchByKeyword(String keyword, Device device) {
        String endpoint = device.getMusicConfig().getEndpoint();
        endpoint = endpoint.endsWith("/") ? endpoint : (endpoint + "/");

        ResponseEntity<JsonNode> response = restTemplate.getForEntity(
                endpoint + "/api/search?keyword=" + keyword,
                JsonNode.class
        );

        return response.getBody();
    }

    @Override
    public String getAlias() {
        return "VIP解锁";
    }

}
