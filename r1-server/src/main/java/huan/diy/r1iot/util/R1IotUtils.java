package huan.diy.r1iot.util;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.UtilityClass;

@UtilityClass
public class R1IotUtils {

    @Setter
    @Getter
    private String currentDeviceId;

    @Setter
    @Getter
    private String authToken;

}
