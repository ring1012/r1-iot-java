package huan.diy.r1iot.service.music;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import huan.diy.r1iot.model.Device;
import huan.diy.r1iot.model.MusicAiResp;
import huan.diy.r1iot.model.R1GlobalConfig;
import jakarta.servlet.http.HttpServletResponse;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Service("gequbao")
public class GequBaoMusicImpl implements IMusicService {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private R1GlobalConfig globalConfig;

    private static final Cache<String, String> urlCache = CacheBuilder.newBuilder()
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .maximumSize(1000)
            .build();


    private static final String regex = "window\\.play_id\\s*=\\s*'([^']+)'";
    private static final Pattern pattern = Pattern.compile(regex);

    @Override
    public JsonNode fetchMusics(MusicAiResp musicAiResp, Device device) {

        StringBuilder sb = new StringBuilder();
        sb.append(StringUtils.hasLength(musicAiResp.getAuthor()) ? musicAiResp.getAuthor() : "");
        sb.append(" ");
        sb.append(StringUtils.hasLength(musicAiResp.getMusicName()) ? musicAiResp.getMusicName() : "");

        // Construct the URL
        String url = "https://www.gequbao.com/s/" + sb;

        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);


        String html = response.getBody();
        Document doc = Jsoup.parse(html);

        ArrayNode musicInfo = objectMapper.createArrayNode();
        Elements songs = doc.select(".card .card-text .row .col-8.col-content a");
        for (Element song : songs) {

            String href = song.attr("href");
            String id = href.substring(href.lastIndexOf("/") + 1);

            ObjectNode music = objectMapper.createObjectNode();
            music.put("id", id);
            music.put("title", song.select(".music-title span").text());
            music.put("artist", song.select(".text-jade").text());
            music.put("url", globalConfig.getHostIp() + "/music/gequbao/" + id + ".mp3");
            musicInfo.add(music);
        }

        ObjectNode result = objectMapper.createObjectNode();

        ObjectNode ret = objectMapper.createObjectNode();
        ret.put("count", musicInfo.size());
        ret.set("musicinfo", musicInfo);
        ret.put("pagesize", String.valueOf(musicInfo.size()));
        ret.put("errorCode", 0);
        ret.put("page", "1");
        ret.put("source", 1);

        result.set("result", ret);

        return result;

    }

    private String findPlayUrl(String songId) {
        String url = "https://www.gequbao.com/music/" + songId;
        ResponseEntity<String> gequData = restTemplate.getForEntity(url, String.class);

        String playId;
        Matcher matcher = pattern.matcher(gequData.getBody());
        if (matcher.find()) {
            playId = matcher.group(1);
        } else {
            throw new RuntimeException("music not found");
        }

        // 2. 设置请求头（JSON 格式）
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        // 3. 构建请求体（JSON 格式）
        String requestBody = String.format("{\"id\":\"%s\"}", playId);

        // 4. 封装请求实体
        HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);

        // 5. 发送 POST 请求并获取响应
        ResponseEntity<JsonNode> musicUrlResp = restTemplate.exchange(
                "https://www.gequbao.com/api/play-url",
                HttpMethod.POST,
                requestEntity,
                JsonNode.class
        );
        return musicUrlResp.getBody().get("data").get("url").asText();
    }

    @Override
    public void streamMusic(String songId, HttpServletResponse response) {
        try {
            String audioUrl = urlCache.get(songId, () -> findPlayUrl(songId));
            ResponseEntity<Resource> audioResponse = restTemplate.getForEntity(audioUrl, Resource.class);

            response.setHeader("Content-Disposition", "inline");
            StreamUtils.copy(audioResponse.getBody().getInputStream(), response.getOutputStream());
        } catch (Exception e) {

        }
    }

    @Override
    public String getAlias() {
        return "爬虫";
    }

}
