package huan.diy.r1iot.service.weather;

import huan.diy.r1iot.model.Device;
import huan.diy.r1iot.service.IWebAlias;

public interface IWeatherService extends IWebAlias {

    String getWeather(String locationName, int offsetDay, Device device);

}
