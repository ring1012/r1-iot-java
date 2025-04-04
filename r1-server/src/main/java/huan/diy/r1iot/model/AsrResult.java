package huan.diy.r1iot.model;

import lombok.ToString;

@ToString
public class AsrResult {
    private AsrHandleType type;
    private String fixedData;
    private String originalData;

    public AsrResult(AsrHandleType type, String fixedData, String originalData) {
        this.type = type;
        this.fixedData = fixedData;
        this.originalData = originalData;
    }

    public AsrHandleType getType() {
        return type;
    }

    public void setType(AsrHandleType type) {
        this.type = type;
    }

    public String getFixedData() {
        return fixedData;
    }

    public void setFixedData(String fixedData) {
        this.fixedData = fixedData;
    }

    public String getOriginalData() {
        return originalData;
    }

    public void setOriginalData(String originalData) {
        this.originalData = originalData;
    }
}
