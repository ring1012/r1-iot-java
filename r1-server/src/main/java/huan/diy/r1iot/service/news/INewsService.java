package huan.diy.r1iot.service.news;

import com.fasterxml.jackson.databind.JsonNode;
import huan.diy.r1iot.model.Device;

public interface INewsService {
    JsonNode fetchNews(String userInput, Device device);
}
