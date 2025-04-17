package huan.diy.r1iot.service.weather;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import huan.diy.r1iot.model.CityLocation;
import huan.diy.r1iot.model.Device;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.zip.GZIPInputStream;

@Slf4j
@Service("QWeatherService")
public class QWeatherServiceImpl implements IWeatherService {

    @Autowired
    private List<CityLocation> cityLocations;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    @Qualifier("gzipRestTemplate")
    private RestTemplate gzipRestTemplate;

    @Qualifier("taskExecutor")
    @Autowired
    private TaskExecutor taskExecutor;


    @Override
    public String getWeather(String locationName, int offsetDay, Device device) {

        String locationId;
        CityLocation mostSimilarCity = null;

        if (StringUtils.hasLength(locationName)) {
            int minDistance = Integer.MAX_VALUE;

            LevenshteinDistance levenshtein = new LevenshteinDistance();

            for (CityLocation cityLocation : cityLocations) {
                String cityName = cityLocation.getCityName();
                // 计算编辑距离（越小越相似）
                int distance = levenshtein.apply(locationName, cityName);

                if (distance < minDistance) {
                    minDistance = distance;
                    mostSimilarCity = cityLocation;
                }
            }

            locationId = mostSimilarCity.getLocationId();

        } else {
            locationId = device.getWeatherConfig().getLocationId();
            for (CityLocation cityLocation : cityLocations) {
                if (cityLocation.getLocationId().equals(locationId)) {
                    locationName = cityLocation.getCityName();
                }
            }
        }

        if (mostSimilarCity == null) {
            mostSimilarCity = cityLocations.stream()
                    .filter(l -> l.getLocationId().equals(locationId))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Location not found"));
        }
        double latitude = mostSimilarCity.getLatitude();
        double longitude = mostSimilarCity.getLongitude();

        try {

            // 3. 并发调用三个API
            CompletableFuture<String> weatherFuture = forecast(device, locationId, offsetDay);
            CompletableFuture<String> airQualityFuture = getAirQuality(device, latitude, longitude, offsetDay);
            CompletableFuture<String> indicesFuture = getIndices(device, locationId, offsetDay);
            CompletableFuture<String> warningFuture = getWarnings(device, locationId);

            // 等待所有请求完成
            CompletableFuture.allOf(weatherFuture, airQualityFuture, indicesFuture).join();

            // 4. 聚合结果
            String dayStr = "";
            if (offsetDay == 0) {
                dayStr = "今天";
            } else if (offsetDay == 1) {
                dayStr = "明天";
            } else if (offsetDay == 2) {
                dayStr = "后天";
            }
            return locationName + dayStr + "的天气情况是：" + weatherFuture.get() + airQualityFuture.get() + indicesFuture.get() + warningFuture.get();
        } catch (Exception e) {
            return null;
        }


    }


    private CompletableFuture<String> getWarnings(Device device, String locationId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = device.getWeatherConfig().getEndpoint() + "/v7/warning/now?location=" + locationId;
                HttpHeaders headers = createHeadersWithApiKey(device.getWeatherConfig().getKey());
                HttpEntity<String> entity = new HttpEntity<>(headers);

                ResponseEntity<byte[]> response = gzipRestTemplate.exchange(
                        url, HttpMethod.GET, entity, byte[].class);


                JsonNode data = objectMapper.readTree(decompressGzip(response.getBody())).get("warning");
                StringBuilder sb = new StringBuilder();
                for (JsonNode node : data) {
                    sb.append(node.get("text").asText()).append("。");
                }
                return sb.toString();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return null;
            }


        }, taskExecutor);
    }

    private CompletableFuture<String> forecast(Device device, String locationId, int offsetDay) {
        return CompletableFuture.supplyAsync(() -> {

            try {
                String url = device.getWeatherConfig().getEndpoint() + "/v7/weather/7d?location=" + locationId;
                HttpHeaders headers = createHeadersWithApiKey(device.getWeatherConfig().getKey());
                HttpEntity<String> entity = new HttpEntity<>(headers);

                ResponseEntity<byte[]> response = gzipRestTemplate.exchange(
                        url, HttpMethod.GET, entity, byte[].class);

                JsonNode resp = objectMapper.readTree(decompressGzip(response.getBody()));
                JsonNode data = resp.get("daily").get(offsetDay);
                return "温度为" + data.get("tempMin").asText() + "度到" + data.get("tempMax").asText() + "度，" +
                        "白天" + data.get("textDay").asText() + "，晚上" + data.get("textNight").asText() + "，" +
                        data.get("windDirDay").asText() + "，风力" + data.get("windScaleDay").asText() + "级。";

            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return null;
            }


        }, taskExecutor);
    }

    private byte[] decompressGzip(byte[] compressed) throws IOException {
//        try (ByteArrayInputStream bis = new ByteArrayInputStream(compressed);
//             GZIPInputStream gis = new GZIPInputStream(bis);
//             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
//            byte[] buffer = new byte[1024];
//            int len;
//            while ((len = gis.read(buffer)) > 0) {
//                bos.write(buffer, 0, len);
//            }
//            return bos.toByteArray();
//        }
        return compressed;
    }

    private CompletableFuture<String> getAirQuality(Device device, double latitude, double longitude, int offsetDay) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = String.format("%s/airquality/v1/daily/%.4f/%.4f",
                        device.getWeatherConfig().getEndpoint(), latitude, longitude);
                HttpHeaders headers = createHeadersWithApiKey(device.getWeatherConfig().getKey());
                HttpEntity<String> entity = new HttpEntity<>(headers);

                ResponseEntity<byte[]> response = gzipRestTemplate.exchange(
                        url, HttpMethod.GET, entity, byte[].class);

                JsonNode resp = objectMapper.readTree(decompressGzip(response.getBody()));
                JsonNode data = resp.get("days").get(offsetDay);
                return ("空气质量" + data.get("indexes").get(0).get("category").asText()) + "。";

            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return null;
            }

        }, taskExecutor);
    }

    public String addDaysToIsoDate(String isoDate, int offsetDays) {
        // 解析带时区的日期时间
        OffsetDateTime dateTime = OffsetDateTime.parse(isoDate);

        // 加上指定天数
        OffsetDateTime newDateTime = dateTime.plusDays(offsetDays);

        // 格式化为"yyyy-MM-dd"
        return newDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    private CompletableFuture<String> getIndices(Device device, String locationId, int offsetDay) {
        return CompletableFuture.supplyAsync(() -> {

            try {
                String url = device.getWeatherConfig().getEndpoint() + "/v7/indices/3d?type=1,3,6&location=" + locationId;
                HttpHeaders headers = createHeadersWithApiKey(device.getWeatherConfig().getKey());
                HttpEntity<String> entity = new HttpEntity<>(headers);

                ResponseEntity<byte[]> response = gzipRestTemplate.exchange(
                        url, HttpMethod.GET, entity, byte[].class);

                JsonNode node = objectMapper.readTree(decompressGzip(response.getBody()));

                String dayString = addDaysToIsoDate(node.get("updateTime").asText(), offsetDay);

                StringBuilder sb = new StringBuilder();
                for (JsonNode each : node.get("daily")) {
                    if (each.get("date").asText().equals(dayString)) {
                        sb.append(each.get("text").asText()).append("。");
                    }
                }


                return sb.toString();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return null;
            }

        }, taskExecutor);
    }

    private HttpHeaders createHeadersWithApiKey(String apiKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-QW-Api-Key", apiKey);
        return headers;
    }

    @Override
    public String getAlias() {
        return "和风天气";
    }
}
