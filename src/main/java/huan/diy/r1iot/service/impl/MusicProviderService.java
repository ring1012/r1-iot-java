package huan.diy.r1iot.service.impl;


import org.springframework.stereotype.Component;

@Component
public class MusicProviderService {
//    private DataUtil dataUtil;
//    private String keyword;
//
//    public void onRecvData(String data) {
//        this.dataUtil = new DataUtil(data);
//    }
//
//    public String buildData() {
//        return dataUtil.build();
//    }
//
//    public boolean isMusic() {
//        Map<String, Object> body = dataUtil.getBody();
//        if (body == null || !body.containsKey("code") || !body.containsKey("text")) {
//            return false;
//        }
//
//        String text = (String) body.get("text");
//
//        // 正则匹配逻辑
//        if (text.matches("^播放(.*)的歌$")) {
//            this.keyword = text.replaceAll("^播放(.*)的歌$", "$1");
//            return true;
//        }
//
//        if (text.matches("^播放(.*?)的(.*)")) {
//            Matcher matcher = Pattern.compile("^播放(.*?)的(.*)").matcher(text);
//            if (matcher.find()) {
//                this.keyword = matcher.group(2) + " " + matcher.group(1);
//                return true;
//            }
//        }
//
//        if (text.matches("^播放(.*?)")) {
//            this.keyword = text.replaceAll("^播放(.*?)", "$1");
//            return true;
//        }
//
//        return false;
//    }
//
//    public boolean processNasCmd() {
//        Map<String, Object> body = dataUtil.getBody();
//        if (!body.containsKey("semantic")) {
//            return false;
//        }
//
//        Format format = new Format();
//        format.setSemantic((Map<String, Object>) body.get("semantic"));
//
//        boolean res = nasMedia.processCtrlCommand((String) body.get("text"), format);
//        if (res) {
//            // dataUtil.setBody(format.getData());
//        }
//        return res;
//    }
//
//    public boolean searchNasMedia() {
//        Map<String, Object> body = dataUtil.getBody();
//        if (!body.containsKey("semantic")) {
//            String text = (String) body.get("text");
//            if (text != null && text.matches("^播放(.*)")) {
//                dataUtil.generateSemanticBody(text);
//                body = dataUtil.getBody();
//            } else {
//                return false;
//            }
//        }
//
//        Format format = new Format();
//        format.setSemantic((Map<String, Object>) body.get("semantic"));
//
//        boolean res = nasMedia.processPlayCommand((String) body.get("text"), format);
//        if (res) {
//            dataUtil.setBody(format.getData());
//        }
//        return res;
//    }
//
//    public void search() {
//        Logs.log("search internet with key word: " + keyword);
//        if (isMusic() && keyword != null && !keyword.isEmpty()) {
//            Map<String, Object> body = dataUtil.getBody();
//            Format format = new Format();
//            format.setSemantic((Map<String, Object>) body.get("semantic"));
//            format.setText(keyword).setAsrText(keyword);
//            new Netease().search(keyword, format);
//            dataUtil.setBody(format.getData());
//        } else {
//            Logs.log("search from internet, not music command or keyword is empty.");
//        }
//    }
//
//    public void testCommand(String cmd) {
//        Logs.log("testCommand: " + cmd);
//        Format format = new Format();
//        boolean res = nasMedia.processPlayCommand(cmd, format);
//        Logs.log("testCommand res: " + res);
//    }
}