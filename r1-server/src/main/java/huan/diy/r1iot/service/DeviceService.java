package huan.diy.r1iot.service;

import huan.diy.r1iot.dao.LocalDeviceDao;
import huan.diy.r1iot.model.Device;
import huan.diy.r1iot.model.R1GlobalConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DeviceService {

    @Autowired
    private LocalDeviceDao deviceDao;

    public List<Device> listAll() {
        return deviceDao.listAll();
    }

    public int upInsert(Device device) {
        return deviceDao.upInsert(device);
    }

    public int upInsertGlobalConfig(R1GlobalConfig config) {
        return deviceDao.upInsertGlobalConfig(config);
    }

}
