package huan.diy.r1iot.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class DataUtil {
    private static final Logger logger = LoggerFactory.getLogger(DataUtil.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private Map<String, Object> httpInfo = new HashMap<>();
    private int bodyLength = 0;

    public DataUtil(String content) {
        parse(content);
    }

    private void parse(String content) {
        // 分割HTTP头和主体
        String[] parts = content.split("\r\n\r\n", 2);
        String header = parts[0] + "\r\n\r\n";
        JsonNode body;
        try {
            body = objectMapper.readTree(parts[1]);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse JSON body", e);
        }

        // 存储解析结果
        httpInfo.put("header", header);
        httpInfo.put("body", body);
        httpInfo.put("origin", content);
    }

    public static Map<String, Object> convertToMap(JsonNode jsonNode) {
        return objectMapper.convertValue(jsonNode, Map.class);
    }

    public Map<String, Object>  getBody() {
        return convertToMap((JsonNode)httpInfo.get("body"));
    }

    public void setBody(JsonNode body) {
        httpInfo.put("body", body);
    }

    public void generateCodeBody() {
        logger.info("generateCodeBody");
        String jsonStr = "{\"code\":\"ANSWER\",\"originIntent\":{\"nluSlotInfos\":[]},\"history\":\"cn.yunzhisheng.chat\",\"source\":\"krc\",\"uniCarRet\":{\"result\":[],\"returnCode\":609,\"message\":\"http post reuqest error\"},\"asr_recongize\":\"12345。\",\"rc\":0,\"general\":{\"resourceId\":\"bdzd_123_3693158\",\"style\":\"CQA_baidu_zhidao\",\"text\":\"上山打老虎\"},\"returnCode\":0,\"audioUrl\":\"http://asrv3.hivoice.cn/trafficRouter/r/YZvVpq\",\"retTag\":\"nlu\",\"service\":\"cn.yunzhisheng.chat\",\"nluProcessTime\":\"320\",\"text\":\"12345\",\"responseId\":\"dc950b037aa24edbb35d6a89dc74c8ea\"}";
        try {
            JsonNode body = objectMapper.readTree(jsonStr);
            httpInfo.put("body", body);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to generate code body", e);
        }
    }

    public void generateSemanticBody(String keyWord) {
        logger.info("generateSemanticBody");
        String jsonStr = "{\"semantic\":{\"intent\":{\"artist\":\"游鸿明\",\"keyword\":\"游鸿明\"}},\"code\":\"SEARCH_CATEGORY\",\"data\":{\"result\":{\"count\":1,\"musicinfo\":[{\"id\":4384379,\"errorCode\":0,\"duration\":3511309,\"lyric\":\"\",\"album\":\"\",\"title\":\"游鸿明3\",\"artist\":\"游鸿明\",\"hdImgUrl\":\"\",\"isCollected\":false,\"url\":\"http://nas.ku8.fun:8888/music/./MP3/游鸿明 - 下沙.mp3\"}],\"totalTime\":10,\"pagesize\":1,\"errorCode\":0,\"page\":\"1\",\"source\":1,\"dataSourceName\":\"我的音乐\"}},\"originIntent\":{\"nluSlotInfos\":[]},\"history\":\"cn.yunzhisheng.music\",\"source\":\"nlu\",\"uniCarRet\":{\"result\":[],\"returnCode\":609,\"message\":\"aios-home.hivoice.cn\"},\"asr_recongize\":\"游鸿明\",\"rc\":0,\"general\":{\"actionAble\":\"true\",\"quitDialog\":\"true\",\"text\":\"游鸿明\",\"type\":\"T\"},\"returnCode\":0,\"audioUrl\":\"http://asrv3.hivoice.cn/trafficRouter/r/wMgclE\",\"retTag\":\"nlu\",\"service\":\"cn.yunzhisheng.music\",\"nluProcessTime\":\"106\",\"text\":\"游鸿明\",\"responseId\":\"e490d9576c5b438c8283a6e71cdba997\"}";
        try {
            ObjectNode body = (ObjectNode) objectMapper.readTree(jsonStr);
            body.put("text", keyWord);
            body.put("asr_recongize", keyWord);
            httpInfo.put("body", body);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to generate semantic body", e);
        }
    }

    public String build() {
        String bodyStr;
        try {
            bodyStr = httpInfo.get("body") != null ? objectMapper.writeValueAsString(httpInfo.get("body")) : "";
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize body to JSON", e);
        }
        int bodyLen = bodyStr.length();

        // 更新Content-Length
        String header = (String) httpInfo.get("header");
        header = header.replaceAll("Content-Length: \\d+\r\n\r\n", "Content-Length: " + bodyLen + "\r\n\r\n");

        return header + bodyStr;
    }
}