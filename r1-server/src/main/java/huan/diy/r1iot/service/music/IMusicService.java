package huan.diy.r1iot.service.music;

import com.fasterxml.jackson.databind.JsonNode;
import huan.diy.r1iot.model.Device;
import huan.diy.r1iot.model.MusicAiResp;

public interface IMusicService {

    JsonNode fetchMusics(MusicAiResp musicAiResp, Device device);

}

