package huan.diy.r1iot.model;

public enum AsrHandleType {
    /**
     * no wanted content, write back to client
     */
    DROPPED,
    /**
     * ASR snippet
     */
    SKIP,
    /**
     * append directly
     */
    APPEND,
    /**
     * pre asr
     */
    PREFIX,
    /**
     * reply to client
     */
    END;
}
