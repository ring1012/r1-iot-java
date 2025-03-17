package huan.diy.r1iot.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/admin")
public class AdminController {
    @GetMapping(value = "/hello")
    public String redirect() {
        return "hello";
    }
}
