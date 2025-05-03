package huan.diy.r1iot.service.radio;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import huan.diy.r1iot.model.Channel;
import huan.diy.r1iot.model.Device;
import huan.diy.r1iot.util.R1IotUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Service("defaultRadio")
@Slf4j
public class DefaultRadioServiceImpl implements IRadioService {

    private static final Cache<String, String> urlCache = CacheBuilder.newBuilder()
            .expireAfterWrite(25, TimeUnit.MINUTES)
            .maximumSize(1000)
            .build();

    @Autowired
    @Qualifier("radios")
    private List<Channel> radios;



    @Autowired
    private RestTemplate restTemplate;
    @Qualifier("objectMapper")
    @Autowired
    private ObjectMapper objectMapper;


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

        String id = mostSimilarChannel.id();
        String link = null;
        try {
            link = urlCache.get(id, () -> fetchM3u8Url(id));
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
        return R1IotUtils.streamRespSample(link);

    }
    private final String API_URL = "https://ytapi.radio.cn/ytsrv/srv/interactive/program/list";

    private String fetchM3u8Url(String broadcastId) {

        try (CloseableHttpClient client = HttpClients.createDefault()) {

            // 2. 准备 POST 请求
            HttpPost post = new HttpPost(API_URL);

            // 3. 构造表单参数
            List<NameValuePair> form = new ArrayList<>();
            form.add(new BasicNameValuePair("startdate", ""));  // 如果服务端不接受空，可去掉或传默认
            form.add(new BasicNameValuePair("enddate", LocalDate.now().toString()));
            form.add(new BasicNameValuePair("broadCastId", broadcastId));

            // 4. 将表单参数编码到请求体
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(form);
            post.setEntity(entity);

            // 5. 设置请求头
            post.addHeader("Content-Type", "application/x-www-form-urlencoded");
            post.addHeader("Accept", "*/*");
            post.addHeader("equipmentsource", "WEB");
            // 可视情况添加 User-Agent、Referer 等

            URI uri = new URI(API_URL);

            HttpHost target = new HttpHost(uri.getScheme(), uri.getHost(), uri.getPort());

            // 6. 执行请求
            ClassicHttpResponse response = client.executeOpen(target, post, null);

            int status = response.getCode();
            String body = null;
            HttpEntity respEntity = response.getEntity();
            if (respEntity != null) {
                body = EntityUtils.toString(respEntity, StandardCharsets.UTF_8);
            }

            // 解析播放地址
            JsonNode responseBody = objectMapper.readTree(body);

            return parsePlayUrl(responseBody);

        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }


    private String parsePlayUrl(JsonNode data) {
        // 优先检查 broadcastPlayUrlHighMp3
        if (data.has("broadcastPlayUrlHighMp3")) {
            JsonNode node = data.path("broadcastPlayUrlHighMp3");
            if (!node.isMissingNode() && node.asText().contains("m3u8")) {
                return node.asText();
            }
        }

        // 次选 playUrlHigh
        if (data.has("playUrlHigh")) {
            JsonNode node = data.path("playUrlHigh");
            if (!node.isMissingNode() && !node.asText().isBlank()) {
                return node.asText();
            }
        }

        // 最后回退
        return "";
    }

}
