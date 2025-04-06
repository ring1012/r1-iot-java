package huan.diy.r1iot.service.audio;

import com.fasterxml.jackson.databind.JsonNode;
import huan.diy.r1iot.model.Device;
import huan.diy.r1iot.service.YoutubeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service("Youtube")
@Slf4j
public class YoutubeAudioServiceImpl implements IAudioService {

    private String suffix;

    public YoutubeAudioServiceImpl() {
        this.suffix = "有声";
    }

    @Autowired
    private YoutubeService youtubeService;

    @Override
    public String getAlias() {
        return "万能的Youtube";
    }

    @Override
    public JsonNode search(String keyword, boolean look, Device device) {
        try {
            return youtubeService.search(keyword, look ? "" : suffix);
        } catch (Exception e) {
            log.error("YoutubeAudioServiceImpl search error", e);
            return null;
        }
    }
}

