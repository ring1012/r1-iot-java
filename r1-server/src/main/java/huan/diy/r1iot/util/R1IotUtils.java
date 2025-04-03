package huan.diy.r1iot.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import huan.diy.r1iot.model.Device;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.UtilityClass;

import java.util.Map;

@UtilityClass
public class R1IotUtils {

    @Setter
    @Getter
    private String currentDeviceId;

    @Setter
    @Getter
    private String authToken;

    @Getter
    @Setter
    private Map<String, Device> deviceMap;

    public ThreadLocal<JsonNode> JSON_RET = new ThreadLocal<>();
    public ThreadLocal<Boolean> REPLACE_ANSWER = new ThreadLocal<>();


    public ObjectMapper objectMapper = new ObjectMapper();

    public JsonNode sampleChatResp(String ttsContent) {
        ObjectNode objectNode = objectMapper.createObjectNode();
        objectNode.put("code", "ANSWER");
        objectNode.put("matchType", "NOT_UNDERSTAND");
        objectNode.put("confidence", 0.8);
        objectNode.put("history", "cn.yunzhisheng.chat");
        objectNode.put("source", "nlu");
        objectNode.put("asr_recongize", ttsContent);
        objectNode.put("rc", 0);

        ObjectNode general = objectMapper.createObjectNode();
        objectNode.set("general", general);

        general.put("style", "CQA_common_customized");
        general.put("text", ttsContent);
        general.put("type", "T");
        general.put("resourceId", "904757");

        objectNode.put("returnCode", 0);
        objectNode.put("audioUrl", "http://asrv3.hivoice.cn/trafficRouter/r/TRdECS");
        objectNode.put("retTag", "nlu");
        objectNode.put("service", "cn.yunzhisheng.chat");
        objectNode.put("nluProcessTime", "717");
        objectNode.put("text", ttsContent);
        objectNode.put("responseId", "9a83414b09024d9d85df88aa07cad8c9");

        return objectNode;
    }




}
