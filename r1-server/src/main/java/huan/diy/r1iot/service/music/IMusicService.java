package huan.diy.r1iot.service.music;

import com.fasterxml.jackson.databind.JsonNode;
import huan.diy.r1iot.model.Device;
import huan.diy.r1iot.model.MusicAiResp;
import huan.diy.r1iot.service.IWebAlias;
import jakarta.servlet.http.HttpServletResponse;

public interface IMusicService extends IWebAlias {

    JsonNode fetchMusics(MusicAiResp musicAiResp, Device device);

    default void streamMusic(String songId, HttpServletResponse response){}
}

