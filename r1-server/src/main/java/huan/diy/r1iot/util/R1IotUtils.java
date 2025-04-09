package huan.diy.r1iot.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import huan.diy.r1iot.model.Device;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

@UtilityClass
@Slf4j
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

    public static final String CHINESE = "[^\u4e00-\u9fa5\uFF00-\uFFEF\u3000-\u303F"
            + "\u0020-\u007E\u00A0-\u00FF\u2000-\u206F]";

    public static final String DEVICE_CONFIG_PATH = System.getProperty("user.home") + "/.r1-iot";

    public ThreadLocal<JsonNode> JSON_RET = new ThreadLocal<>();
    public ThreadLocal<Boolean> REPLACE_ANSWER = new ThreadLocal<>();
    public ThreadLocal<Boolean> ONLY_ONCE = new ThreadLocal<>();
    public ThreadLocal<String> CLIENT_IP = new ThreadLocal<>();


    public void remove() {
        JSON_RET.remove();
        REPLACE_ANSWER.remove();
        ONLY_ONCE.remove();
        CLIENT_IP.remove();
    }

    @Getter
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


    public void cfInstall(String serviceId) {
        String scriptPath = "/manage_cloudflared.sh"; // 脚本路径

        try {
            // 执行命令
            ProcessBuilder processBuilder = new ProcessBuilder(scriptPath, serviceId);
            processBuilder.redirectErrorStream(true); // 合并标准错误和标准输出
            Process process = processBuilder.start();


            // 读取命令输出
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                log.info(line);
            }

            // 读取错误输出
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            while ((line = errorReader.readLine()) != null) {
                log.error(line);
            }

            // 等待命令执行完成
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                log.info("Cloudflared service installed successfully!");
            } else {
                log.error("Failed to install cloudflared service. Exit code: " + exitCode);
            }
        } catch (IOException | InterruptedException e) {
            log.error("Error executing command: " + e.getMessage(), e);
        }
    }


}
