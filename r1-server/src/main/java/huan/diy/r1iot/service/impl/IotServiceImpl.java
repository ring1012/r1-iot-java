package huan.diy.r1iot.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import huan.diy.r1iot.service.IR1Service;
import org.springframework.stereotype.Service;

@Service("cn.yunzhisheng.setting")
public class IotServiceImpl implements IR1Service {
    @Override
    public JsonNode replaceOutPut(JsonNode jsonNode) {
        return jsonNode;
    }
}
