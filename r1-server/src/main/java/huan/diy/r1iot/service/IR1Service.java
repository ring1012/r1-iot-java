package huan.diy.r1iot.service;

import com.fasterxml.jackson.databind.JsonNode;

public interface IR1Service {

    JsonNode replaceOutPut(JsonNode jsonNode, String deviceId);

}
