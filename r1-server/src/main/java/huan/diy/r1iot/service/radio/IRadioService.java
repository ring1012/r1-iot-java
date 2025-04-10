package huan.diy.r1iot.service.radio;

import com.fasterxml.jackson.databind.JsonNode;
import huan.diy.r1iot.model.Device;

public interface IRadioService {

    JsonNode fetchRadio(String radioName, String province, Device device);


}
