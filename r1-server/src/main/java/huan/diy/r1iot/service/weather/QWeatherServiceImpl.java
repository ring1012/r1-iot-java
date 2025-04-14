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
            CompletableFuture<JsonNode> weatherFuture = forecast(device, locationId, offsetDay);
            CompletableFuture<JsonNode> airQualityFuture = getAirQuality(device, latitude, longitude, offsetDay);
            CompletableFuture<JsonNode> indicesFuture = getIndices(device, locationId, offsetDay);
            CompletableFuture<JsonNode> warningFuture = getWarnings(device, locationId);

            // 等待所有请求完成
            CompletableFuture.allOf(weatherFuture, airQualityFuture, indicesFuture).join();

            // 4. 聚合结果
            ObjectNode result = objectMapper.createObjectNode();
            result.set("forecast", weatherFuture.get());
            result.set("airQuality", airQualityFuture.get());
            result.set("indices", indicesFuture.get());
            result.set("warnings", warningFuture.get());

            return locationName + result.toString();
        } catch (Exception e) {
            return null;
        }


    }


    private CompletableFuture<JsonNode> getWarnings(Device device, String locationId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = device.getWeatherConfig().getEndpoint() + "/v7/warning/now?location=" + locationId;
                HttpHeaders headers = createHeadersWithApiKey(device.getWeatherConfig().getKey());
                HttpEntity<String> entity = new HttpEntity<>(headers);

                ResponseEntity<byte[]> response = gzipRestTemplate.exchange(
                        url, HttpMethod.GET, entity, byte[].class);


                return objectMapper.readTree(decompressGzip(response.getBody())).get("warning");
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return null;
            }


        }, taskExecutor);
    }

    private CompletableFuture<JsonNode> forecast(Device device, String locationId, int offsetDay) {
        return CompletableFuture.supplyAsync(() -> {

            try {
                String url = device.getWeatherConfig().getEndpoint() + "/v7/weather/7d?location=" + locationId;
                HttpHeaders headers = createHeadersWithApiKey(device.getWeatherConfig().getKey());
                HttpEntity<String> entity = new HttpEntity<>(headers);

                ResponseEntity<byte[]> response = gzipRestTemplate.exchange(
                        url, HttpMethod.GET, entity, byte[].class);

                JsonNode resp = objectMapper.readTree(decompressGzip(response.getBody()));
                return resp.get("daily").get(offsetDay);
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

    private CompletableFuture<JsonNode> getAirQuality(Device device, double latitude, double longitude, int offsetDay) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = String.format("%s/airquality/v1/daily/%.4f/%.4f",
                        device.getWeatherConfig().getEndpoint(), latitude, longitude);
                HttpHeaders headers = createHeadersWithApiKey(device.getWeatherConfig().getKey());
                HttpEntity<String> entity = new HttpEntity<>(headers);

                ResponseEntity<byte[]> response = gzipRestTemplate.exchange(
                        url, HttpMethod.GET, entity, byte[].class);

                JsonNode resp = objectMapper.readTree(decompressGzip(response.getBody()));
                return resp.get("days").get(offsetDay);

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

    private CompletableFuture<JsonNode> getIndices(Device device, String locationId, int offsetDay) {
        return CompletableFuture.supplyAsync(() -> {

            try {
                String url = device.getWeatherConfig().getEndpoint() + "/v7/indices/3d?type=0&location=" + locationId;
                HttpHeaders headers = createHeadersWithApiKey(device.getWeatherConfig().getKey());
                HttpEntity<String> entity = new HttpEntity<>(headers);

                ResponseEntity<byte[]> response = gzipRestTemplate.exchange(
                        url, HttpMethod.GET, entity, byte[].class);

                JsonNode node = objectMapper.readTree(decompressGzip(response.getBody()));

                String dayString = addDaysToIsoDate(node.get("updateTime").asText(), offsetDay);

                ArrayNode ret = objectMapper.createArrayNode();
                for (JsonNode each : node.get("daily")) {
                    if (each.get("date").asText().equals(dayString)) {
                        ret.add(each);
                    }
                }


                return ret;
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
