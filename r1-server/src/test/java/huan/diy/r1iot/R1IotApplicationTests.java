package huan.diy.r1iot;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.charset.StandardCharsets;

class R1IotApplicationTests {

    @Test
    void contextLoads() {
        String str = "UI:bjHCdGTCdCoCcCT9cGouhHLrdHosgqksd9";
        System.out.println(str.split("UI:").length);
    }

}
