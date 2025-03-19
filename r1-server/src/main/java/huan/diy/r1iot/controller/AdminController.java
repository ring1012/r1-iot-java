package huan.diy.r1iot.controller;

import huan.diy.r1iot.model.Device;
import huan.diy.r1iot.model.R1AdminData;
import huan.diy.r1iot.model.R1Resources;
import huan.diy.r1iot.service.DeviceService;
import huan.diy.r1iot.util.R1IotUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    @Autowired
    private R1Resources r1Resources;

    @Autowired
    private DeviceService deviceService;

    @GetMapping(value = "/resources")
    public R1AdminData redirect() {
        List<Device> devices = deviceService.listAll();

        return new R1AdminData(r1Resources, devices, R1IotUtils.getCurrentDeviceId());
    }

    @PostMapping("/device")
    public String deviceOne(@RequestBody final Device device) {
        int ret = deviceService.upInsert(device);
        if (ret == 0) {
            throw new RuntimeException("设备更新失败了");
        }else {
            return "success";
        }
    }


}
