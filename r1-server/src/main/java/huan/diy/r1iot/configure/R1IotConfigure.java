package huan.diy.r1iot.configure;

import com.fasterxml.jackson.databind.ObjectMapper;
import huan.diy.r1iot.model.Channel;
import huan.diy.r1iot.model.R1GlobalConfig;
import huan.diy.r1iot.model.R1Resources;
import huan.diy.r1iot.service.IWebAlias;
import huan.diy.r1iot.service.ai.IAIService;
import huan.diy.r1iot.service.audio.IAudioService;
import huan.diy.r1iot.service.news.INewsService;
import huan.diy.r1iot.service.music.IMusicService;
import huan.diy.r1iot.util.R1IotUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static huan.diy.r1iot.util.R1IotUtils.DEVICE_CONFIG_PATH;

@Configuration
@EnableAspectJAutoProxy
@EnableAsync
public class R1IotConfigure {

    @Bean
    public R1GlobalConfig r1GlobalConfig() {
        Path path = Paths.get(DEVICE_CONFIG_PATH, "global.conf");
        if (Files.exists(path)) {
            try {
                String content = Files.readString(path);
                R1GlobalConfig ret = new ObjectMapper().readValue(content, R1GlobalConfig.class);
                if (StringUtils.hasLength(ret.getCfServiceId())) {
                    new Thread(() -> R1IotUtils.cfInstall(ret.getCfServiceId())).start();
                }

                return ret;

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new R1GlobalConfig();
    }


    @Bean
    public R1Resources r1Resources(@Autowired List<IAIService> aiServices,
                                   @Autowired List<IMusicService> musicServices,
                                   @Autowired List<INewsService> newsServices,
                                   @Autowired List<IAudioService> audioServices) {

        return new R1Resources(aiServices.stream().map(a -> (IWebAlias) a).map(IWebAlias::serviceAliasName).toList(),
                musicServices.stream().map(a -> (IWebAlias) a).map(IWebAlias::serviceAliasName).toList(),
                newsServices.stream().map(a -> (IWebAlias) a).map(IWebAlias::serviceAliasName).toList(),
                audioServices.stream().map(a -> (IWebAlias) a).map(IWebAlias::serviceAliasName).toList()
        );
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000); // 连接超时
        factory.setReadTimeout(30000);       // 读取超时

        RestTemplate restTemplate = new RestTemplate(factory);

        // 忽略 SSL 证书验证（可选）
        try {
            javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[]{
                    new javax.net.ssl.X509TrustManager() {
                        public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                        }

                        public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                        }

                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new java.security.cert.X509Certificate[0];
                        }
                    }
            };

            javax.net.ssl.SSLContext sslContext = javax.net.ssl.SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

            javax.net.ssl.HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
            javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier((s, sslSession) -> true);
        } catch (Exception e) {
            throw new RuntimeException("Failed to disable SSL check", e);
        }

        return restTemplate;
    }


    @Bean("radios")
    public List<Channel> fetchAndParseM3U(@Autowired RestTemplate restTemplate) {
        try {
            // 1. 尝试从 GitHub 获取最新版本
            String remoteUrl = "https://raw.githubusercontent.com/fanmingming/live/main/radio/m3u/fm.m3u";
            String content = restTemplate.getForObject(remoteUrl, String.class);
            return parseM3UContent(content);

        } catch (Exception e) {
            // 2. 远程获取失败，回退到本地文件
            try {
                InputStream is = new ClassPathResource("fm.m3u").getInputStream();
                String localContent = new BufferedReader(new InputStreamReader(is))
                        .lines()
                        .reduce("", (a, b) -> a + "\n" + b);
                return parseM3UContent(localContent);

            } catch (Exception ex) {
                throw new RuntimeException("无法获取广播列表（远程和本地均失败）", ex);
            }
        }
    }

    private List<Channel> parseM3UContent(String content) {
        List<Channel> channels = new ArrayList<>();
        Pattern pattern = Pattern.compile(
                "#EXTINF:.*tvg-name=\"([^\"]*)\".*group-title=\"([^\"]*)\".*\\n(http[^\\s]*)"
        );

        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            Channel channel = new Channel(matcher.group(1), matcher.group(2), matcher.group(3));
            channels.add(channel);
        }
        return channels;
    }

}
