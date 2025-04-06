package huan.diy.r1iot.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class R1AdminData {
    private R1Resources r1Resources;
    private List<Device> devices;
    private String currentDeviceId;
    private R1GlobalConfig r1GlobalConfig;
}
