package huan.diy.r1iot.configure;

import com.fasterxml.jackson.databind.ObjectMapper;
import huan.diy.r1iot.model.Channel;
import huan.diy.r1iot.model.CityLocation;
import huan.diy.r1iot.model.R1GlobalConfig;
import huan.diy.r1iot.model.R1Resources;
import huan.diy.r1iot.service.IWebAlias;
import huan.diy.r1iot.service.ai.IAIService;
import huan.diy.r1iot.service.audio.IAudioService;
import huan.diy.r1iot.service.news.INewsService;
import huan.diy.r1iot.service.music.IMusicService;
import huan.diy.r1iot.service.weather.IWeatherService;
import huan.diy.r1iot.util.R1IotUtils;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.DefaultClientConnectionReuseStrategy;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.util.TimeValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.*;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
                                   @Autowired List<IAudioService> audioServices,
                                   @Autowired List<IWeatherService> weatherServices,
                                   @Autowired @Qualifier("cityLocations") List<CityLocation> cityLocations) {

        return new R1Resources(aiServices.stream().map(a -> (IWebAlias) a).map(IWebAlias::serviceAliasName).toList(),
                musicServices.stream().map(a -> (IWebAlias) a).map(IWebAlias::serviceAliasName).toList(),
                newsServices.stream().map(a -> (IWebAlias) a).map(IWebAlias::serviceAliasName).toList(),
                audioServices.stream().map(a -> (IWebAlias) a).map(IWebAlias::serviceAliasName).toList(),
                weatherServices.stream().map(a -> (IWebAlias) a).map(IWebAlias::serviceAliasName).toList(), cityLocations
        );
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }


    @Bean
    public ClientHttpRequestFactory clientHttpRequestFactory() {
        // 1. 创建连接池管理器
        PoolingHttpClientConnectionManager connectionManager =
                new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(200); // 最大连接数
        connectionManager.setDefaultMaxPerRoute(20); // 每个路由基础连接数

        // 2. 创建HttpClient
        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .build();

        // 3. 使用HttpClient创建RestTemplate
        HttpComponentsClientHttpRequestFactory factory =
                new HttpComponentsClientHttpRequestFactory(httpClient);
        factory.setConnectTimeout(10000); // 连接超时(ms)
        factory.setReadTimeout(30000);    // 读取超时(ms)

        return factory;
    }

    @Bean("restTemplate")
    @Primary
    public RestTemplate restTemplate(@Autowired ClientHttpRequestFactory clientHttpRequestFactory) {

        RestTemplate restTemplate = new RestTemplate(clientHttpRequestFactory);

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

    public static class GzipHttpComponentsClientHttpRequestFactory extends HttpComponentsClientHttpRequestFactory {

        public GzipHttpComponentsClientHttpRequestFactory(HttpClient httpClient) {
            super(httpClient);
        }

        @Override
        protected HttpContext createHttpContext(HttpMethod httpMethod, URI uri) {
            HttpClientContext context = HttpClientContext.create();
            context.setRequestConfig(RequestConfig.custom()
                    .setRedirectsEnabled(true)
                    .build());
            return context;
        }
    }

    @Bean("gzipRestTemplate")
    public RestTemplate gzipRestTemplate() {
        // 1. 创建连接池管理器
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        // 总连接池大小
        connectionManager.setMaxTotal(200);
        // 每个主机的最大并行连接数
        connectionManager.setDefaultMaxPerRoute(50);

        // 2. 配置HttpClient
        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                // 启用连接复用（默认为true，可以省略）
                .setConnectionReuseStrategy(DefaultClientConnectionReuseStrategy.INSTANCE)
                // 连接存活时间（可选）
                .setKeepAliveStrategy((response, context) -> TimeValue.ofMinutes(30))
                .build();

        // 3. 创建支持GZIP的RestTemplate
        RestTemplate restTemplate = new RestTemplate();

        // 添加GZIP支持的拦截器
        restTemplate.setInterceptors(Collections.singletonList(new ClientHttpRequestInterceptor() {
            @Override
            public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                                ClientHttpRequestExecution execution) throws IOException {
                // 添加接受GZIP编码的请求头
                request.getHeaders().add("Accept-Encoding", "gzip");
                return execution.execute(request, body);
            }
        }));


        GzipHttpComponentsClientHttpRequestFactory requestFactory =
                new GzipHttpComponentsClientHttpRequestFactory(httpClient);
        requestFactory.setConnectTimeout(5000);
        requestFactory.setReadTimeout(30000);
        // 设置支持GZIP和连接池的ClientHttpRequestFactory
        restTemplate.setRequestFactory(requestFactory);

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

    @Bean("cityLocations")
    public List<CityLocation> fetchAndParseCityList(@Autowired RestTemplate restTemplate) {
        try {
            // 1. Try to fetch from GitHub first
            String remoteUrl = "https://raw.githubusercontent.com/qwd/LocationList/refs/heads/master/China-City-List-latest.csv";
            String content = restTemplate.getForObject(remoteUrl, String.class);
            return parseCsvContent(content);

        } catch (Exception e) {
            // 2. Fallback to local file if remote fetch fails
            try {
                InputStream is = new ClassPathResource("China-City-List-latest.csv").getInputStream();
                String localContent = new BufferedReader(new InputStreamReader(is))
                        .lines()
                        .collect(Collectors.joining("\n"));
                return parseCsvContent(localContent);

            } catch (Exception ex) {
                throw new RuntimeException("无法获取城市列表数据（远程和本地均失败）", ex);
            }
        }
    }

    private List<CityLocation> parseCsvContent(String content) {
        if (content == null || content.isEmpty()) {
            return Collections.emptyList();
        }
        List<CityLocation> cities = new ArrayList<>();
        try {
            // Skip the first line (header comment) and parse from second line
            String[] lines = content.split("\n");


            // Start from line 1 (skip line 0 which is the comment)
            for (int i = 2; i < lines.length; i++) {
                String[] columns = lines[i].split(",");
                if (columns.length >= 3) {
                    CityLocation city = new CityLocation();
                    city.setLocationId(columns[0]);
                    city.setCityName(columns[2]);
                    city.setLatitude(Double.parseDouble(columns[columns.length - 3]));
                    city.setLongitude(Double.parseDouble(columns[columns.length - 2]));
                    cities.add(city);
                }
            }
            return cities;

        } catch (Exception e) {
            e.printStackTrace();
            return cities;
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
