package huan.diy.r1iot.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import huan.diy.r1iot.service.IR1Service;
import org.springframework.stereotype.Service;


@Service("default")
public class DefaultServiceImpl implements IR1Service {
    @Override
    public JsonNode replaceOutPut(JsonNode jsonNode, String deviceId) {
        return jsonNode;
    }
}
