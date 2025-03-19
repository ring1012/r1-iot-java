package huan.diy.r1iot.configure;

import huan.diy.r1iot.model.R1Resources;
import huan.diy.r1iot.service.IWebAlias;
import huan.diy.r1iot.service.ai.IAIService;
import huan.diy.r1iot.service.audio.IAudioService;
import huan.diy.r1iot.service.music.IMusicService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Configuration
@EnableAspectJAutoProxy
public class R1IotConfigure {

    @Bean
    public R1Resources r1Resources(@Autowired List<IAIService> aiServices,
                                   @Autowired List<IMusicService>musicServices,
                                   @Autowired List<IAudioService> audioServices) {
        return new R1Resources(aiServices.stream().map(a->(IWebAlias)a).map(IWebAlias::serviceAliasName).toList(),
                musicServices.stream().map(a->(IWebAlias)a).map(IWebAlias::serviceAliasName).toList(),
                audioServices.stream().map(a->(IWebAlias)a).map(IWebAlias::serviceAliasName).toList()
                );
    }


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
