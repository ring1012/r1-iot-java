package huan.diy.r1iot.service;

import huan.diy.r1iot.dao.LocalDeviceDao;
import huan.diy.r1iot.model.Device;
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

}
