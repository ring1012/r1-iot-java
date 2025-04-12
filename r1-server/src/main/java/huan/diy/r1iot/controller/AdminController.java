package huan.diy.r1iot.controller;

import huan.diy.r1iot.direct.AIDirect;
import huan.diy.r1iot.model.Device;
import huan.diy.r1iot.model.R1AdminData;
import huan.diy.r1iot.model.R1GlobalConfig;
import huan.diy.r1iot.model.R1Resources;
import huan.diy.r1iot.service.DeviceService;
import huan.diy.r1iot.util.R1IotUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;


@RestController
@RequestMapping("/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    @Autowired
    private R1Resources r1Resources;

    @Autowired
    private R1GlobalConfig r1GlobalConfig;

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private AIDirect direct;

    @GetMapping(value = "/resources")
    public R1AdminData redirect() {
        List<Device> devices = deviceService.listAll();

        return new R1AdminData(r1Resources, devices, R1IotUtils.getCurrentDeviceId(), r1GlobalConfig);
    }

    @PostMapping("/device")
    public String deviceOne(@RequestBody final Device device) {
        int ret = deviceService.upInsert(device);
        if (ret == 0) {
            throw new RuntimeException("设备更新失败了");
        } else {
            return "success";
        }
    }


    @PostMapping("/globalConfig")
    public String deviceOne(@RequestBody final R1GlobalConfig req) {
        if (Optional.ofNullable(req.getCfServiceId()).map(a ->
                StringUtils.hasLength(a) && !a.equals(r1GlobalConfig.getCfServiceId())).orElse(false)) {
            new Thread(() -> R1IotUtils.cfInstall(req.getCfServiceId())).start();
        }
        r1GlobalConfig.setCfServiceId(req.getCfServiceId());
        r1GlobalConfig.setYtdlpEndpoint(req.getYtdlpEndpoint());
        r1GlobalConfig.setHostIp(req.getHostIp());
        deviceService.upInsertGlobalConfig(r1GlobalConfig);
        return "success";
    }




}
