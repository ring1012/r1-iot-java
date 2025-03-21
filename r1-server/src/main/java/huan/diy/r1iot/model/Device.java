package huan.diy.r1iot.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Device {
    private String id;
    private String name;
    private AIConfig aiConfig;
    private HASSConfig hassConfig;
    private NewsConfig newsConfig;


    // Static inner class for AIConfig
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AIConfig {
        private String choice;
        private String key;
        private String systemPrompt;
        private int chatHistoryNum;
    }

    // Static inner class for HASSConfig
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class HASSConfig {
        private String endpoint;
        private String token;
    }

    // Static inner class for NewsConfig
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class NewsConfig {
        private String choice;
    }
}
