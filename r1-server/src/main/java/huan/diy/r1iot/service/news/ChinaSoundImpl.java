package huan.diy.r1iot.service.news;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import huan.diy.r1iot.model.Device;
import huan.diy.r1iot.service.IWebAlias;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service("chinaSound")
@Slf4j
public class ChinaSoundImpl implements INewsService, IWebAlias {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public String getAlias() {
        return "中国之声";
    }

    @Override
    public JsonNode fetchNews(String userInput, Device device) {
        // API地址
        String url = "https://apppc.cnr.cn/cnr45609411d2c5a16/e281277129d478c12c2ed58e84ca906b/f76a0411ae1ff31be9f9e28f0b51348b";

        // 构建请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("chanId", "64");  // 中国之声频道ID
        requestBody.put("pageIndex", 1);  // 第一页
        requestBody.put("perPage", 40);   // 每页20条
        requestBody.put("lastNewsId", "0");
        requestBody.put("docPubTime", "");

        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 创建请求实体
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
        JsonNode directNews;
        try {
            // 发送POST请求
            ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);

            // 将响应内容转换为JsonNode
            directNews = objectMapper.readTree(response.getBody());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        ArrayNode arrayNode = (ArrayNode) directNews.get("data").get("categories").get(0).get("detail");

        ArrayNode musicInfo = objectMapper.createArrayNode();
        int index = 0;
        for (JsonNode node : arrayNode) {
            try {
                String link = node.get("other_info9").asText();
                if (link.contains("m3u8")) {
                    continue;
                }
                ObjectNode music = objectMapper.createObjectNode();
                music.put("id", index++);
                music.put("title", "新闻");
                music.put("artist", "中国之声");
                music.put("url", link);
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
}
