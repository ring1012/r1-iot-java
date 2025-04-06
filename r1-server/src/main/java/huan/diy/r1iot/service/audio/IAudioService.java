package huan.diy.r1iot.service.audio;

import com.fasterxml.jackson.databind.JsonNode;
import huan.diy.r1iot.model.Device;
import huan.diy.r1iot.service.IWebAlias;


public interface IAudioService extends IWebAlias {

    JsonNode search(String keyword, boolean look,  Device device);


}
