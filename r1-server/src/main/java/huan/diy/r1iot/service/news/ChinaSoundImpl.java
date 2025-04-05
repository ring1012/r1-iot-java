package huan.diy.r1iot.service.news;

import huan.diy.r1iot.service.IWebAlias;
import org.springframework.stereotype.Service;

@Service("chinaSound")
public class ChinaSoundImpl implements INewsService, IWebAlias {
    @Override
    public String getAlias() {
        return "中国之声";
    }
}
