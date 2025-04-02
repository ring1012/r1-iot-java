package huan.diy.r1iot.service.music;

import com.fasterxml.jackson.databind.JsonNode;
import huan.diy.r1iot.model.Device;
import huan.diy.r1iot.model.MusicAiResp;
import huan.diy.r1iot.service.IWebAlias;
import org.springframework.stereotype.Service;

@Service("QQ")
public class TencentMusicImpl implements IMusicService, IWebAlias {

    @Override
    public String getAlias() {
        return "QQ音乐";
    }

    @Override
    public JsonNode fetchMusics(MusicAiResp musicAiResp, Device device) {
        return null;
    }
}
