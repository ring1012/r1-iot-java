package huan.diy.r1iot.configure;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    // 配置静态资源路径
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 映射 / 下的所有请求到 static 目录
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(3600); // 可选：设置缓存时间（单位：秒）
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // 将所有未知路径重定向到 index.html
        registry.addViewController("/{path:[^\\.]*}")
                .setViewName("forward:/index.html");
    }

}