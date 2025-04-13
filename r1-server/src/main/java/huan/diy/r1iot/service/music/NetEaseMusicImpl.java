package huan.diy.r1iot.service.music;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import huan.diy.r1iot.model.Device;
import huan.diy.r1iot.model.MusicAiResp;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service("NetEaseMusic")
@Slf4j
public class NetEaseMusicImpl implements IMusicService {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RestTemplate restTemplate;

    @Override
    public String getAlias() {
        return "网易云音乐Nodejs API";
    }

    /**
     * {
     * "result": {
     * "count": 1,
     * "musicinfo": [
     * {
     * "id": "123456",
     * "title": "夜曲",
     * "artist": "周杰伦",
     * "album": "十一月的萧邦",
     * "duration": 240,
     * "url": "https://ting8.yymp3.com/new18/murongxx2/5.mp3",
     * "imgUrl": "https://example.com/images/123456.jpg",
     * "hdImgUrl": "https://example.com/images/123456_hd.jpg",
     * "isCollected": false
     * }
     * ],
     * "totalTime": 240,
     * "pagesize": "1",
     * "errorCode": 0,
     * "page": "1",
     * "source": 1,
     * "dataSourceName": "我的音乐"
     * }
     * }
     *
     * @param musicAiResp
     * @param device
     * @return
     */
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
        ArrayNode arrayNode = (ArrayNode) searchRet.get("result").get("songs");

        ArrayNode musicInfo = objectMapper.createArrayNode();
        Map<Long, ObjectNode> idMap = new LinkedHashMap<>();
        for (JsonNode node : arrayNode) {
            try {
                ObjectNode music = objectMapper.createObjectNode();
                Long id = node.get("id").asLong();
                music.put("id", id);
                music.put("title", node.get("name").asText());
                music.put("artist", node.get("ar").get(0).get("name").asText());
                music.put("album", node.get("al").get("name").asText());
                music.put("imgUrl", node.get("al").get("picUrl").asText());
                idMap.put(id, music);
                musicInfo.add(music);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }

        }

        Map<Long, String> urlMap = getMusicUrl(idMap.keySet(), device);
        for (Long id : idMap.keySet()) {
            ObjectNode node = idMap.get(id);
            node.put("url", urlMap.get(id));
        }

        ObjectNode result = objectMapper.createObjectNode();

        ObjectNode ret = objectMapper.createObjectNode();
        ret.put("count", idMap.size());
        ret.set("musicinfo", musicInfo);
        ret.put("pagesize", String.valueOf(idMap.size()));
        ret.put("errorCode", 0);
        ret.put("page", "1");
        ret.put("source", 1);

        result.set("result", ret);

        return result;
    }

    @Override
    public void streamMusic(String songId, HttpServletResponse response) {

    }

    public JsonNode searchByKeyword(String keyword, Device device) {
        String endpoint = device.getMusicConfig().getEndpoint();
        endpoint = endpoint.endsWith("/") ? endpoint : (endpoint + "/");

        ResponseEntity<JsonNode> response = restTemplate.getForEntity(
                endpoint + "cloudsearch?keywords=" + keyword,
                JsonNode.class
        );

        return response.getBody();
    }

    public Map<Long, String> getMusicUrl(Set<Long> ids, Device device) {
        String endpoint = device.getMusicConfig().getEndpoint();
        endpoint = endpoint.endsWith("/") ? endpoint : (endpoint + "/");
        ResponseEntity<JsonNode> response = restTemplate.getForEntity(
                endpoint + "song/url/v1?id=" + ids.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse("") + "&level=exhigh",
                JsonNode.class
        );
        Map<Long, String> ret = new HashMap<>();
        ArrayNode data = (ArrayNode) response.getBody().get("data");
        for (JsonNode node : data) {
            ret.put(node.get("id").asLong(), node.get("url").asText());
        }
        return ret;
    }

}
