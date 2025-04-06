package huan.diy.r1iot.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class R1GlobalConfig {
    private String hostIp;
    private String ytdlpEndpoint;
    private String cfServiceId;
}
