package huan.diy.r1iot;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.charset.StandardCharsets;

class R1IotApplicationTests {

    @Test
    void contextLoads() {
        String str = "{\"code\":\"ANSWER\",\"matchType\":\"NOT_UNDERSTAND\",\"originIntent\":{\"nluSlotInfos\":[]},\"confidence\":1.0,\"modelIntentClsScore\":{},\"history\":\"cn.yunzhisheng.chat\",\"source\":\"krc\",\"uniCarRet\":{\"result\":{},\"returnCode\":609,\"message\":\"http post reuqest error\"},\"asr_recongize\":\"是圆的。\",\"rc\":0,\"general\":{\"resourceId\":\"904754\",\"style\":\"CQA_common_customized\",\"text\":\"地球之所以是圆的，主要有以下几个原因：\\n\\n*   **引力作用：** 地球上的所有物质都受到万有引力的作用，引力将物质向中心拉拢，最终形成球体。\\n*   **旋转：** 地球的自转产生离心力，进一步促使地球趋向球形。\\n\\n简单的说，引力和旋转共同作用，使得地球成为了一个近似球体的形状。\"},\"returnCode\":0,\"audioUrl\":\"http://asrv3.hivoice.cn/trafficRouter/r/qBQByE\",\"retTag\":\"nlu\",\"service\":\"cn.yunzhisheng.chat\",\"nluProcessTime\":\"520\",\"text\":\"地球为什么是圆的\",\"responseId\":\"49cdc031afac4622849191cbe89bc65d\"}";
        System.out.println(str.getBytes(StandardCharsets.UTF_8).length);
    }

}
