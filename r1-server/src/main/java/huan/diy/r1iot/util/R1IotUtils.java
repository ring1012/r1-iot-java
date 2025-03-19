package huan.diy.r1iot.util;

import huan.diy.r1iot.model.Device;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.UtilityClass;

import java.util.Map;

@UtilityClass
public class R1IotUtils {

    @Setter
    @Getter
    private String currentDeviceId;

    @Setter
    @Getter
    private String authToken;

    @Getter
    @Setter
    private Map<String, Device> deviceMap;

}
