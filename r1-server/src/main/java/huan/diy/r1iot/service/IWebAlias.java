package huan.diy.r1iot.service;

import huan.diy.r1iot.model.ServiceAliasName;
import org.springframework.stereotype.Service;

import java.util.Map;

public interface IWebAlias {

    default String getServiceName(){
        return this.getClass().getAnnotation(Service.class).value();
    }


    String getAlias();


    default ServiceAliasName serviceAliasName(){
        return new ServiceAliasName(getServiceName(), getAlias());
    }
}
