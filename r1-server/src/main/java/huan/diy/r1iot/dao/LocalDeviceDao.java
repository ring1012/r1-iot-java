package huan.diy.r1iot.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import huan.diy.r1iot.model.Device;
import huan.diy.r1iot.util.R1IotUtils;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Slf4j
public class LocalDeviceDao {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String DEVICE_CONFIG_PATH = Optional.ofNullable(System.getenv("DEVICE_CONFIG_PATH"))
            .orElse(System.getProperty("user.home") + "/.r1-iot");

    @PostConstruct
    public void init() {
        // 创建 Path 对象
        Path path = Paths.get(DEVICE_CONFIG_PATH);

        // 检查路径是否存在，不存在则创建
        if (!Files.exists(path)) {
            try {
                // 如果路径不存在，则创建该目录及其父目录
                Files.createDirectories(path);
                System.out.println("Device config path created: " + path);
            } catch (IOException e) {
                log.error("create path failed", e);
                System.err.println("Failed to create device config path: " + path);
            }
        } else {
            System.out.println("Device config path already exists: " + path);
        }

        refresh();
    }

    private void refresh() {
        List<Device> devices = listAll();
        R1IotUtils.setDeviceMap(devices.stream()
                .collect(Collectors.toMap(Device::getId, device -> device)));
    }


    public List<Device> listAll() {
        List<Device> devices = new ArrayList<>();
        File folder = new File(DEVICE_CONFIG_PATH);
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".json"));

        if (files != null) {
            for (File file : files) {
                try {
                    // 将每个文件的内容转换成 Device 对象
                    Device device = objectMapper.readValue(file, Device.class);
                    devices.add(device);
                } catch (IOException e) {
                    log.error("read device failed", e);
                    System.err.println("Failed to read device from file: " + file.getName());
                }
            }
        }
        return devices;
    }


    public int upInsert(Device device) {
        enrichDeviceUrl(device);
        // 获取文件路径，文件名就是 device.getId() + ".json"
        File deviceFile = new File(DEVICE_CONFIG_PATH, device.getId() + ".json");

        // 如果文件存在，先删除它
        if (deviceFile.exists()) {
            if (deviceFile.delete()) {
                log.info("Deleted old device file: " + deviceFile.getName());
            } else {
                log.error("Failed to delete old device file: " + deviceFile.getName());
                return 0; // 删除失败，返回失败
            }
        }

        // 创建新文件并写入设备数据
        try {
            objectMapper.writeValue(deviceFile, device);
            log.info("Inserted/Updated device into file: " + deviceFile.getName());
            return 1; // 成功
        } catch (IOException e) {
            log.error("Failed to write device to file", e);
            return 0; // 写入失败，返回失败
        } finally {
            refresh();
        }

    }

    private void enrichDeviceUrl(Device device) {
        Optional.ofNullable(device.getHassConfig()).ifPresent(a -> a.setEndpoint(httpSchema(a.getEndpoint())));
        Optional.ofNullable(device.getMusicConfig()).ifPresent(a -> a.setEndpoint(httpSchema(a.getEndpoint())));
    }

    private String httpSchema(String input) {
        if (!StringUtils.hasLength(input)) {
            return "";
        }
        if (input.trim().startsWith("http")) {
            return input.trim();
        }
        return "http://" + input.trim();
    }


}
