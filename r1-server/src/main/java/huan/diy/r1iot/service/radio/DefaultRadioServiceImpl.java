package huan.diy.r1iot.service.radio;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import huan.diy.r1iot.model.Channel;
import huan.diy.r1iot.model.Device;
import huan.diy.r1iot.util.R1IotUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.similarity.CosineDistance;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

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

    @Override
    public JsonNode fetchRadio(String radioName, String province, Device device) {
        try {
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

                return R1IotUtils.streamRespSample(urlCache.get(url, () -> get302Url(url)));

            }
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
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
