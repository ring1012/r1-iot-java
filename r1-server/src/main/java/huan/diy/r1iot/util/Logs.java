package huan.diy.r1iot.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Logs {
    private static final Logger logger = LoggerFactory.getLogger(Logs.class);

    public static void log(Object obj){
        logger.info( obj.toString() );
    }

}
