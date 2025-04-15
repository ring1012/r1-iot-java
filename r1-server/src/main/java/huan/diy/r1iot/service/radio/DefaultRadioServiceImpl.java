package huan.diy.r1iot.service.radio;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import huan.diy.r1iot.model.Channel;
import huan.diy.r1iot.model.Device;
import huan.diy.r1iot.model.R1GlobalConfig;
import huan.diy.r1iot.util.R1IotUtils;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service("defaultRadio")
@Slf4j
public class DefaultRadioServiceImpl implements IRadioService {

    private static final Cache<String, String> urlCache = CacheBuilder.newBuilder()
            .expireAfterWrite(25, TimeUnit.MINUTES)  // 写入后6hours过期
            .maximumSize(1000)                       // 最大缓存1000个条目
            .build();

    @Autowired
    @Qualifier("radios")
    private List<Channel> radios;

    @Autowired
    private R1GlobalConfig globalConfig;

    @Autowired
    private RestTemplate restTemplate;

    @Override
    public JsonNode fetchRadio(String radioName, String province, Device device) {
        Channel mostSimilarChannel = null;
        int minDistance = Integer.MAX_VALUE; // LevenshteinDistance 越小越相似

        LevenshteinDistance levenshtein = new LevenshteinDistance();

        for (Channel channel : radios) {
            String tvgName = channel.groupTitle() + " " + channel.tvgName();

            // 计算编辑距离（越小越相似）
            int distance = levenshtein.apply(province + " " + radioName, tvgName);

            if (distance < minDistance) {
                minDistance = distance;
                mostSimilarChannel = channel;
            }
        }

        String url = mostSimilarChannel.url();
        if (url.contains("m3u8")) {
            return R1IotUtils.streamRespSample(url);
        } else {
            String link = globalConfig.getHostIp() + "/stream/radio/" +
                    Base64.getUrlEncoder().withoutPadding().encodeToString(url.getBytes(StandardCharsets.UTF_8))
                    + ".m3u8";
            return R1IotUtils.streamRespSample(link);

        }

    }

    @Override
    public void streamRadio(String resourceUrl, HttpServletResponse response) {
        try {
            byte[] decodedBytes = Base64.getUrlDecoder().decode(resourceUrl);
            String url = new String(decodedBytes, StandardCharsets.UTF_8);

            String m3u8Url = urlCache.get(url, () -> get302Url(url));

// 1. 获取 M3U8 文件内容（字符串形式）
            ResponseEntity<String> m3u8Response = restTemplate.getForEntity(m3u8Url, String.class);
            String m3u8Content = m3u8Response.getBody();

// 2. 解析 M3U8 的 base URL（去掉文件名部分）
            URI uri = new URI(m3u8Url);
            String baseUrl = uri.resolve(".").toString(); // 如：http://example.com/path/

// 3. 处理 M3U8 内容：
//    - 替换相对路径的 .ts 文件为完整 URL
//    - 修改 #EXT-X-MEDIA-SEQUENCE 值（+2）
            String processedM3U8 = m3u8Content.lines()
                    .map(line -> {
                        // 处理 #EXT-X-MEDIA-SEQUENCE 行
                        if (line.startsWith("#EXT-X-MEDIA-SEQUENCE:")) {
                            // 提取数值并 +2
                            String[] parts = line.split(":");
                            if (parts.length == 2) {
                                try {
                                    long sequenceNum = Long.parseLong(parts[1].trim());
                                    sequenceNum += 2;
                                    return "#EXT-X-MEDIA-SEQUENCE:" + sequenceNum;
                                } catch (NumberFormatException e) {
                                    // 如果解析失败，保持原样
                                    return line;
                                }
                            }
                        }
                        // 处理非标签行（TS 文件路径）
                        else if (!line.startsWith("#")) {
                            // 如果已经是完整 URL，则跳过
                            if (line.startsWith("http://") || line.startsWith("https://")) {
                                return line;
                            }
                            // 否则拼接 baseUrl + TS 文件名
                            return baseUrl + line;
                        }
                        // 其他情况（注释行、其他标签）保持不变
                        return line;
                    })
                    .collect(Collectors.joining("\n")); // 重新拼接成字符串

// 4. 返回修改后的 M3U8 内容
            response.setContentType("application/vnd.apple.mpegurl"); // M3U8 的 Content-Type
            response.getWriter().write(processedM3U8);
        } catch (Exception e) {
            log.error("Failed to stream radio: {}", resourceUrl, e);
        }

    }

    private String get302Url(String url) {

        try {
            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NEVER) // 禁止自动重定向
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());

            if (response.statusCode() == 302) {
                return response.headers().firstValue("Location").orElse("");
            }
        } catch (Exception e) {
            log.error("Failed to fetch URL: {}", url, e);
        }

        return url;
    }

}
