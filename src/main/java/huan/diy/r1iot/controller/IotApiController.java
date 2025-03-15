package huan.diy.r1iot.controller;

import huan.diy.r1iot.model.IotResp;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Enumeration;

@RestController
public class IotApiController {

    @Autowired
    private HttpServletRequest request;

    @PostMapping("/trafficRouter/**")
    public IotResp uploadBinaryFile(HttpServletRequest request) {
        System.out.println(new Date());
        printRequestHeaders();
        // 获取输入流
        try (InputStream inputStream = request.getInputStream()) {
            // 定义文件保存路径
            Path filePath = Paths.get("uploads/uploaded_file.bin");

            // 确保上传目录存在
            Files.createDirectories(filePath.getParent());

            // 将输入流写入文件
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);

            // 返回响应
            IotResp resp = new IotResp();
            resp.setText("文件上传成功");
            resp.setStatus(200);
            resp.setMessage("ok");
            return resp;
        } catch (IOException e) {
            e.printStackTrace();
            // 返回响应
            IotResp resp = new IotResp();
            resp.setText("文件上传成功");
            resp.setStatus(200);
            resp.setMessage("ok");
            return resp;
        }
    }


    private void printDate(){
        // 获取当前时间
        LocalTime currentTime = LocalTime.now();

        // 定义格式化器
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

        // 格式化并打印
        String formattedTime = currentTime.format(formatter);
        System.out.println("当前时间: " + formattedTime);
    }

    private void printRequestHeaders() {
        System.out.println("===== 请求头信息 =====");
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            System.out.println(headerName + ": " + headerValue);
        }
        System.out.println("====================");
    }

    @GetMapping("/**")
    public String getHello(){
        System.out.println("get");
        return "hello";
    }

    @GetMapping("/hello")
    public String hello(){
        return "hello";
    }

}
