package huan.diy.r1iot.free;

import huan.diy.r1iot.direct.AIDirect;
import huan.diy.r1iot.service.YoutubeService;
import huan.diy.r1iot.util.R1IotUtils;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@CrossOrigin(origins = "*")
public class NoAuthController {

    @Autowired
    private AIDirect aidirect;

    @Autowired
    private YoutubeService youtubeService;

    @PostMapping("/auth")
    public String login(@RequestBody final Map<String, String> map) {
        String password = map.get("password");
        String envPass = System.getenv("password");
        if (password.equals(envPass)) {
            String token = UUID.randomUUID().toString();
            R1IotUtils.setAuthToken(token);
            return token;
        } else {
            throw new RuntimeException("password does not match");
        }
    }


    @GetMapping("/audio/play/{vId}.m4a")
    public void streamAudio(@PathVariable String vId,
                            @RequestHeader(value = "Range", required = false) String rangeHeader,
                            HttpServletResponse response) throws Exception {
        youtubeService.streamAudio(vId, rangeHeader, response);
    }

    @GetMapping("/test")
    public String test(@RequestParam String deviceId) {

        String resp = aidirect.getAssistants().get(deviceId).getAssistant().chat("明天天气");

        System.out.println(resp);

        return "success";
    }
}
