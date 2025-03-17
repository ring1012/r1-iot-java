package huan.diy.r1iot.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class IotApiController {
    @GetMapping(value = "/hello")
    public String redirect() {
        return "hello";
    }
}
