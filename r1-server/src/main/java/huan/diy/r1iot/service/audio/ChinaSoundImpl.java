package huan.diy.r1iot.service.audio;

import huan.diy.r1iot.service.IWebAlias;
import org.springframework.stereotype.Service;

@Service("chinaSound")
public class ChinaSoundImpl implements IAudioService, IWebAlias {
    @Override
    public String getAlias() {
        return "中国之声";
    }
}
