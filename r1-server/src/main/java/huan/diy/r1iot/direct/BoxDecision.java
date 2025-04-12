package huan.diy.r1iot.direct;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import huan.diy.r1iot.model.Device;
import huan.diy.r1iot.model.MusicAiResp;
import huan.diy.r1iot.service.audio.IAudioService;
import huan.diy.r1iot.service.box.BoxControllerService;
import huan.diy.r1iot.service.news.INewsService;
import huan.diy.r1iot.service.hass.HassServiceImpl;
import huan.diy.r1iot.service.music.IMusicService;
import huan.diy.r1iot.service.radio.IRadioService;
import huan.diy.r1iot.service.weather.IWeatherService;
import huan.diy.r1iot.util.R1IotUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.Map;

@Slf4j
@Data
public class BoxDecision {


    private static final ObjectMapper objectMapper;

    private static final ObjectNode intent;

    static {
        objectMapper = R1IotUtils.getObjectMapper();
        intent = objectMapper.createObjectNode();

        ObjectNode operationsObj = objectMapper.createObjectNode();
        ArrayNode operations = objectMapper.createArrayNode();
        operationsObj.set("operations", operations);
        intent.set("intent", operationsObj);
        ObjectNode operator = objectMapper.createObjectNode();
        operations.add(operator);

        operator.put("operator", "ACT_PLAY");

    }

    public BoxDecision(Device device,
                       Map<String, IMusicService> musicServiceMap,
                       Map<String, INewsService> newsServiceMap,
                       Map<String, IAudioService> audioServiceMap,
                       Map<String, IWeatherService> weatherServiceMap,
                       HassServiceImpl iotService,
                       BoxControllerService boxControllerService,
                       IRadioService radioService) {
        this.device = device;
        this.musicServiceMap = musicServiceMap;
        this.newsServiceMap = newsServiceMap;
        this.audioServiceMap = audioServiceMap;
        this.weatherServiceMap = weatherServiceMap;
        this.iotService = iotService;
        this.boxControllerService = boxControllerService;
        this.radioService = radioService;
    }

    private Device device;
    private Map<String, IMusicService> musicServiceMap;
    private Map<String, INewsService> newsServiceMap;
    private Map<String, IAudioService> audioServiceMap;
    private Map<String, IWeatherService> weatherServiceMap;
    private HassServiceImpl iotService;
    private BoxControllerService boxControllerService;
    private IRadioService radioService;

    private boolean asked() {
        if (R1IotUtils.ONLY_ONCE.get() == Boolean.TRUE) {
            return true;
        }
        R1IotUtils.ONLY_ONCE.set(true);
        return false;
    }

    @Tool("""
            用于处理播放音乐请求，比如流行歌曲，儿歌等等.
            samples: 我想听刀郎的歌，播放夜曲
            """)
    void playMusic(@P(value = "歌曲作者，可以为空字符串", required = false) String author,
                   @P(value = "歌曲名称，可以为空字符串", required = false) String songName,
                   @P(value = "歌曲搜索关键词，可以为空字符串", required = false) String keyword) {
        if (asked()) {
            return;
        }
        log.info("author: {}, songName: {}, keyword: {}", author, songName, keyword);
        JsonNode musicResp = musicServiceMap.get(device.getMusicConfig().getChoice()).fetchMusics(new MusicAiResp(author, songName, keyword), device);
        JsonNode jsonNode = R1IotUtils.JSON_RET.get();
        ObjectNode ret = ((ObjectNode) jsonNode);
        ret.set("data", musicResp);
        ret.set("semantic", intent);
        ret.put("code", "SETTING_EXEC");
        ret.put("matchType", "FUZZY");

        ObjectNode general = objectMapper.createObjectNode();
        general.put("text", "好的，已为您播放");
        general.put("type", "T");
        ret.set("general", general);
        ret.put("service", "cn.yunzhisheng.music");


        ret.remove("taskName");
        R1IotUtils.JSON_RET.set(ret);
    }

    @Tool("""
            音箱一般设置：切换氛围灯，音量，停止，休眠等等
            """)
    void voiceBoxSetting(@P(value = "控制对象：氛围灯(lamp)，快进(faster)，快退(slower)，跳到时间(jump)，输出英文", required = false) String target,
                         @P(value = "执行动作, 比如打开(on)，关闭(off)，切换效果(change)，时间(数值，单位：秒)，输出英文", required = false) String action) {
        log.info("target: {}, action: {}", target, action);
        if (asked()) {
            return;
        }
        if (!StringUtils.hasLength(R1IotUtils.CLIENT_IP.get())) {
            return;
        }

        if (!StringUtils.hasLength(target)) {
            return;
        }

        boolean handled = boxControllerService.control(R1IotUtils.CLIENT_IP.get(), target, action);
        if (!handled) {
            return;
        }

        R1IotUtils.REPLACE_ANSWER.set(true);


    }

    @Tool("""
            智能家居控制，比如打开灯、热得快，空调，调节温度，查询湿度，等等
            """)
    String homeassistant(String actionCommand) {
        if (asked()) {
            return "SUCCESS";
        }
        log.info("Called homeassistant with  actionCommand={}", actionCommand);

        R1IotUtils.REPLACE_ANSWER.set(true);

        String tts = iotService.replaceOutPut(R1IotUtils.JSON_RET.get(), device.getId());
        if (tts.isEmpty()) {
            return actionCommand;
        }
        R1IotUtils.JSON_RET.set(R1IotUtils.sampleChatResp(tts));

        return tts;

    }

    @Tool("""
            用于播放新闻，比如体育、财经、科技、娱乐等等。
            samples: 播放新闻
            """)
    void playNews(@P("用户输入") String userInput) {
        if (asked()) {
            return;
        }

        INewsService newsService = newsServiceMap.getOrDefault(device.getNewsConfig().getChoice(), newsServiceMap.get("chinaSound"));
        JsonNode musicResp = newsService.fetchNews(userInput, device);
        JsonNode jsonNode = R1IotUtils.JSON_RET.get();
        ObjectNode ret = ((ObjectNode) jsonNode);
        ret.set("data", musicResp);
        ret.set("semantic", intent);
        ret.put("code", "SETTING_EXEC");
        ret.put("matchType", "FUZZY");

        ObjectNode general = objectMapper.createObjectNode();
        general.put("text", "好的，已为您播放");
        general.put("type", "T");
        ret.set("general", general);
        ret.put("service", "cn.yunzhisheng.music");


        ret.remove("taskName");
        R1IotUtils.JSON_RET.set(ret);

        log.info("Called playNews with userInput={}", userInput);
    }

    @Tool("""
            用于播放故事、视频、有声读物等。
            samples: 我想看三体，播放三体有声读物
            """)
    void playAudio(@P("关键词") String keyword, @P(value = "动作，是否是看？", required = false) boolean look) {
        if (asked()) {
            return;
        }

        log.info("Called playAudio with keyword={}", keyword);
        JsonNode musicResp = audioServiceMap.get(device.getAudioConfig().getChoice()).search(keyword, look, device);
        JsonNode jsonNode = R1IotUtils.JSON_RET.get();
        ObjectNode ret = ((ObjectNode) jsonNode);
        ret.set("data", musicResp);
        ret.set("semantic", intent);
        ret.put("code", "SETTING_EXEC");
        ret.put("matchType", "FUZZY");

        ObjectNode general = objectMapper.createObjectNode();
        general.put("text", "好的，已为您播放");
        general.put("type", "T");
        ret.set("general", general);
        ret.put("service", "cn.yunzhisheng.music");


        ret.remove("taskName");
        R1IotUtils.JSON_RET.set(ret);

    }


    @Tool("""
            用于播放广播
            samples: 我想听上海交通广播
            """)
    void playRadio(@P("广播名称") String radioName, @P(value = "省份", required = false) String province) {
        if (asked()) {
            return;
        }


        log.info("Called playAudio with radioName={}, province={}", radioName, province == null ? "" : province);

        JsonNode ret = radioService.fetchRadio(radioName, province, device);


        R1IotUtils.JSON_RET.set(ret);
    }


    @Tool("""
            用于查询天气，有默认位置，可以不用说城市
            samples: 上海浦东后天什么天气
            AI: locationName=浦东 offsetDay=2
            """)
    String queryWeather(@P(value = "位置名", required = false) String locationName, @P(value = "offsetDay", required = false) int offsetDay) {
        if (asked()) {
            return "SUCCESS";
        }

        log.info("Called queryWeather with locationName={}, offsetDay={}", locationName, offsetDay);

        String ret = weatherServiceMap.get(device.getWeatherConfig().getChoice()).getWeather(locationName, offsetDay, device);
        R1IotUtils.JSON_RET.set(R1IotUtils.sampleChatResp(""));
        R1IotUtils.REPLACE_ANSWER.set(true);

        return "你是专业的天气预报播音员，做一次详细的汇报！注意请说出城市名，温度，风，空气质量，穿衣、户外，气象预警 \n\n" + ret;


    }

}