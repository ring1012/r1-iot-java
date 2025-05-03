package huan.diy.r1iot.configure;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 只处理特定的静态资源类型
        registry.addResourceHandler(
                        "/",
                        "/server",
                        "/**/*.js",
                        "/**/*.css",
                        "/**/*.png",
                        "/**/*.jpg",
                        "/**/*.jpeg",
                        "/**/*.gif",
                        "/**/*.svg",
                        "/**/*.ico",
                        "/**/*.woff",
                        "/**/*.ttf",
                        "/**/*.map"
                ).addResourceLocations("classpath:/static/")
                .setCachePeriod(3600); // 缓存可选
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // 映射根路径到 index.html
        registry.addViewController("/").setViewName("forward:/index.html");
        // 映射 /server 到 index.html（或其他静态页面）
        registry.addViewController("/server").setViewName("forward:/index.html");
    }
}
