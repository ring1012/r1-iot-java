package huan.diy.r1iot.configure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class R1IotConfigure {

    @Bean
    public RestTemplate restTemplate() {
        // 创建 HttpComponentsClientHttpRequestFactory 实例
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();

        // 设置 read timeout 为 20 秒
        factory.setReadTimeout(20000);
        factory.setConnectTimeout(1000);

        // 创建 RestTemplate 实例并设置自定义的 RequestFactory

        return new RestTemplate(factory);
    }

}
