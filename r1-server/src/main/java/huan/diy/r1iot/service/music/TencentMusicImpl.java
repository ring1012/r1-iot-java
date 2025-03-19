package huan.diy.r1iot.service.music;

import huan.diy.r1iot.service.IWebAlias;
import org.springframework.stereotype.Service;

@Service("QQ")
public class TencentMusicImpl implements IMusicService, IWebAlias {

    @Override
    public String getAlias() {
        return "QQ音乐";
    }
}
