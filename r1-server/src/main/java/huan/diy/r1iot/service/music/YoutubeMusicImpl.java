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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.TimeUnit;

@Service("YoutubeMusic")
@Slf4j
public class YoutubeMusicImpl implements IMusicService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private R1GlobalConfig globalConfig;

    private static final String MUSIC_SEARCH = "https://music.youtube.com/youtubei/v1/search?prettyPrint=false";

    private static final String SAMPLE_PAYLOAD = """
            {"context":{"client":{"hl":"en","gl":"SG","remoteHost":"202.6.40.167","deviceMake":"Apple","deviceModel":"","visitorData":"Cgs4QXVOYVV4OUp0TSj28rfABjIKCgJDThIEGgAgSA%3D%3D","userAgent":"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36,gzip(gfe)","clientName":"WEB_REMIX","clientVersion":"1.20250423.01.00","osName":"Macintosh","osVersion":"10_15_7","originalUrl":"https://music.youtube.com/search?q=%E5%91%A8%E6%9D%B0%E4%BC%A6","platform":"DESKTOP","clientFormFactor":"UNKNOWN_FORM_FACTOR","configInfo":{"appInstallData":"CPbyt8AGENPhrwUQ3rzOHBDJ968FEPPWzhwQ_vP_EhDw7M4cELvZzhwQ4OXOHBC36v4SEODczhwQ4YKAExDiuLAFEParsAUQ5Of_EhC9mbAFEJr0zhwQv5LPHBCIhM8cEI-HgBMQntCwBRCJp84cEL22rgUQioKAExCG284cEPirsQUQ37jOHBDLic8cENb1zhwQ6YWAExCk784cEIHNzhwQu5DPHBCJsM4cELjkzhwQ29rOHBC52c4cEPaOzxwQj4WAExD8ss4cEIeszhwQzN-uBRCU_rAFEKj_zhwQiIewBRDJ5rAFEJmYsQUQvoqwBRCI468FEOvo_hIQ4OD_EhDw4s4cEIiQzxwQxYHPHBCZjbEFEPCcsAUQ0YrPHBDt-M4cEKvwzhwQ0MjOHBCa-M4cKixDQU1TR3hVUW9MMndETkhrQnBTQ0V0WFM2Z3Vya3dYVzBBWEozQVVkQnc9PQ%3D%3D","coldConfigData":"CPbyt8AGGjJBT2pGb3gwdzgzRjFsMnM4YmFwYVJVekxTZkNNT19ST01fTV83M05qREt6TGpQTjZnQSIyQU9qRm94MHc4M0YxbDJzOGJhcGFSVXpMU2ZDTU9fUk9NX01fNzNOakRLekxqUE42Z0E%3D","coldHashData":"CPbyt8AGEhM4MzcyMjg4Nzg1MDY2MDg0NzkyGPbyt8AGMjJBT2pGb3gwdzgzRjFsMnM4YmFwYVJVekxTZkNNT19ST01fTV83M05qREt6TGpQTjZnQToyQU9qRm94MHc4M0YxbDJzOGJhcGFSVXpMU2ZDTU9fUk9NX01fNzNOakRLekxqUE42Z0E%3D","hotHashData":"CPbyt8AGEhIyMzE1NTI2MTEwMzY5MjU2MDAY9vK3wAYyMkFPakZveDB3ODNGMWwyczhiYXBhUlV6TFNmQ01PX1JPTV9NXzczTmpES3pMalBONmdBOjJBT2pGb3gwdzgzRjFsMnM4YmFwYVJVekxTZkNNT19ST01fTV83M05qREt6TGpQTjZnQQ%3D%3D"},"browserName":"Chrome","browserVersion":"135.0.0.0","acceptHeader":"text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7","deviceExperimentId":"ChxOelE1TnpreU16STBNREUwT1Rnek5qZzNOdz09EPbyt8AGGPbyt8AG","rolloutToken":"CKzf38j4gM3zBRDRgrPjmPeMAxjG2PTlmPeMAw%3D%3D","screenWidthPoints":1920,"screenHeightPoints":840,"screenPixelDensity":2,"screenDensityFloat":2,"utcOffsetMinutes":480,"userInterfaceTheme":"USER_INTERFACE_THEME_LIGHT","timeZone":"Asia/Shanghai","musicAppInfo":{"pwaInstallabilityStatus":"PWA_INSTALLABILITY_STATUS_CAN_BE_INSTALLED","webDisplayMode":"WEB_DISPLAY_MODE_BROWSER","storeDigitalGoodsApiSupportStatus":{"playStoreDigitalGoodsApiSupportStatus":"DIGITAL_GOODS_API_SUPPORT_STATUS_UNSUPPORTED"}}},"user":{"lockedSafetyMode":false},"request":{"useSsl":true,"internalExperimentFlags":[],"consistencyTokenJars":[]},"clickTracking":{"clickTrackingParams":"CIgCEPleGAEiEwjO_vrd8_eMAxUun0sFHdL2Fkg="},"adSignalsInfo":{"params":[{"key":"dt","value":"1745746294431"},{"key":"flash","value":"0"},{"key":"frm","value":"0"},{"key":"u_tz","value":"480"},{"key":"u_his","value":"4"},{"key":"u_h","value":"1080"},{"key":"u_w","value":"1920"},{"key":"u_ah","value":"961"},{"key":"u_aw","value":"1920"},{"key":"u_cd","value":"24"},{"key":"bc","value":"31"},{"key":"bih","value":"840"},{"key":"biw","value":"1905"},{"key":"brdim","value":"0,25,0,25,1920,25,1920,961,1920,840"},{"key":"vis","value":"1"},{"key":"wgl","value":"true"},{"key":"ca_type","value":"image"}],"bid":"ANyPxKo_j-9-19NGLusRqC-ENaLXSN4LYAnX7H877KgtKCuxX2XXLTbOEEh-WJt09tLMau0HJkfwQQ5LxSE-8aEP8F73sNHNbQ"}},"query":"","params":"EgWKAQIIAWoSEAMQBBAJEA4QChAFEBEQEBAV"}
            """;

    private static final Cache<String, String> urlCache = CacheBuilder.newBuilder()
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .maximumSize(1000)
            .build();

    @Override
    public JsonNode fetchMusics(MusicAiResp musicAiResp, Device device) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(StringUtils.hasLength(musicAiResp.getAuthor()) ? musicAiResp.getAuthor() : "");
            sb.append(" ");
            sb.append(StringUtils.hasLength(musicAiResp.getMusicName()) ? musicAiResp.getMusicName() : "");
            String keyword = sb.toString().trim();
            if (keyword.isEmpty()) {
                keyword = musicAiResp.getKeyword();
            }

            ObjectNode req = (ObjectNode) objectMapper.readTree(SAMPLE_PAYLOAD);
            req.put("query", keyword);

            JsonNode resp = restTemplate.postForEntity(MUSIC_SEARCH, req, JsonNode.class).getBody();
            // contents.tabbedSearchResultsRenderer.tabs.tabRenderer.content.sectionListRenderer.contents[0].musicShelfRenderer.contents[].flexColumns[0].musicResponsiveListItemFlexColumnRenderer.text.runs {text, videoId}
            ArrayNode arrayNode = (ArrayNode) resp.get("contents").get("tabbedSearchResultsRenderer").get("tabs").get(0)
                    .get("tabRenderer").get("content").get("sectionListRenderer").get("contents").get(0).get("musicShelfRenderer").get("contents");

            ArrayNode musicInfo = objectMapper.createArrayNode();

            for (JsonNode node : arrayNode) {
                try {
                    ObjectNode music = objectMapper.createObjectNode();
                    // flexColumns[0].musicResponsiveListItemFlexColumnRenderer.text.runs {text, videoId}
                    JsonNode data = node.get("musicResponsiveListItemRenderer").get("flexColumns").get(0).get("musicResponsiveListItemFlexColumnRenderer").get("text").get("runs").get(0);
                    String id = data.get("navigationEndpoint").get("watchEndpoint").get("videoId").asText();
                    music.put("id", id);
                    music.put("title", data.get("text").asText());
                    music.put("artist", node.get("musicResponsiveListItemRenderer").get("flexColumns").get(1).get("musicResponsiveListItemFlexColumnRenderer")
                            .get("text").get("runs").get(0).get("text").asText());
                    music.put("url", globalConfig.getHostIp() + "/audio/play/" + id + ".m4a");
                    musicInfo.add(music);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }

            }

            ObjectNode result = objectMapper.createObjectNode();

            ObjectNode ret = objectMapper.createObjectNode();
            ret.put("count", arrayNode.size());
            ret.set("musicinfo", musicInfo);
            ret.put("pagesize", String.valueOf(arrayNode.size()));
            ret.put("errorCode", 0);
            ret.put("page", "1");
            ret.put("source", 1);

            result.set("result", ret);

            return result;

        } catch (Exception e) {

        }


        return null;
    }


    @Override
    public String getAlias() {
        return "万能的Youtube";
    }
}
