package huan.diy.r1iot;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import huan.diy.r1iot.util.R1IotUtils;
import org.junit.jupiter.api.Test;

class R1IotApplicationTests {

    @Test
    void contextLoads() throws JsonProcessingException {
        String str = "\":{\"mood\":\"中性\",\"style\":\"HIGH_QUALITY\",\"text\":\"上海很美丽的你带我去看看吧\"},\"returnCode\":0,\"retTag\":\"nlu\",\"service\":\"cn.yunzhisheng.chat\",\"nluProcessTime\":\"526\",\"text\":\"上海交\",\"responseId\":\"c0fa3fc02a7d49c5bd14c26c83535fa9\"}";
        ObjectMapper objectMapper = new ObjectMapper();
        System.out.println(objectMapper.readTree(str));
    }

    @Test
    public void chinese(){
        System.out.println("好的！很高兴跟你聊天。你想聊些什么呢？可以告诉我你的兴趣爱好，或者有什么问题想问？\uD83D\uDE0A"
                .replaceAll(R1IotUtils.CHINESE, ""));
    }
}
