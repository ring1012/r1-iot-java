package huan.diy.r1iot;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class R1IotApplicationTests {

    @Test
    void contextLoads() throws JsonProcessingException {
        String str = "\":{\"mood\":\"中性\",\"style\":\"HIGH_QUALITY\",\"text\":\"上海很美丽的你带我去看看吧\"},\"returnCode\":0,\"retTag\":\"nlu\",\"service\":\"cn.yunzhisheng.chat\",\"nluProcessTime\":\"526\",\"text\":\"上海交\",\"responseId\":\"c0fa3fc02a7d49c5bd14c26c83535fa9\"}";
        ObjectMapper objectMapper = new ObjectMapper();
        System.out.println(objectMapper.readTree(str));
    }

}
