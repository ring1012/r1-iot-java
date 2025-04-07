package huan.diy.r1iot.configure;

import com.fasterxml.jackson.databind.ObjectMapper;
import huan.diy.r1iot.model.R1GlobalConfig;
import huan.diy.r1iot.model.R1Resources;
import huan.diy.r1iot.service.IWebAlias;
import huan.diy.r1iot.service.ai.IAIService;
import huan.diy.r1iot.service.audio.IAudioService;
import huan.diy.r1iot.service.news.INewsService;
import huan.diy.r1iot.service.music.IMusicService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

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
                return new ObjectMapper().readValue(content, R1GlobalConfig.class);
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

}
