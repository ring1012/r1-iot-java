package huan.diy.r1iot.model;

import huan.diy.r1iot.anno.AIDescription;
import huan.diy.r1iot.anno.AIEnums;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IotAiResp {

    @AIDescription("entityId of the target")
    private String entityId;


    @AIDescription("action intention of user")
    @AIEnums({"ON", "OFF", "QUERY"})
    private String action;

    @AIDescription("such as: temperature")
    private String parameter;


}
