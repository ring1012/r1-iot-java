package huan.diy.r1iot.model;


public class IotResp {
    // {"status":200,"message":"ok","text":"tts内容"}
    private String text;
    private int status=200;
    private String message="ok";


    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
