package huan.diy.r1iot.configure;

import huan.diy.r1iot.util.R1IotUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;


@Aspect
@Component
public class AuthorizationAspect {


    private final HttpServletRequest request;

    // 构造函数注入 HttpServletRequest
    public AuthorizationAspect(HttpServletRequest request) {
        this.request = request;
    }

    // 定义一个切入点，所有标注为 @RestController 的方法都会被拦截
    @Pointcut("execution(public * huan.diy.r1iot.controller..*(..))")
    public void restControllerMethods() {}

    // 在执行方法之前执行此切面，验证Authorization头
    @Before("restControllerMethods()")
    public void checkAuthorization() {
        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader == null || !authorizationHeader.equals(R1IotUtils.getAuthToken())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unauthorized");
        }
    }
}
