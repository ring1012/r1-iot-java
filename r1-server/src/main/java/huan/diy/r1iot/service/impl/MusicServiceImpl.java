package huan.diy.r1iot.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import huan.diy.r1iot.service.IR1Service;
import org.springframework.stereotype.Service;

@Service("cn.yunzhisheng.music")
public class MusicServiceImpl implements IR1Service {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    @Override
    public JsonNode replaceOutPut(JsonNode jsonNode) {
        try {
            return objectMapper.readTree("""
                    
                    {
                        "semantic": {
                            "intent": {
                                "operations": [
                                    {
                                        "operator": "ACT_PLAY"
                                    }
                                ]
                            }
                        },
                        "code": "SETTING_EXEC",
                        "matchType": "FUZZY",
                        "originIntent": {
                            "nluSlotInfos": []
                        },
                        "data": {
                            "result": {
                                "count": 1,
                                "musicinfo": [
                                    {
                                        "id": "123456",
                                        "title": "夜曲",
                                        "artist": "周杰伦",
                                        "album": "十一月的萧邦",
                                        "duration": 240,
                                        "url": "https://ting8.yymp3.com/new18/murongxx2/5.mp3",
                                        "imgUrl": "https://example.com/images/123456.jpg",
                                        "hdImgUrl": "https://example.com/images/123456_hd.jpg",
                                        "isCollected": false
                                    }
                                ],
                                "totalTime": 240,
                                "pagesize": "1",
                                "errorCode": 0,
                                "page": "1",
                                "source": 1,
                                "dataSourceName": "我的音乐"
                            }
                        },
                        "confidence": 0.6313702287003818,
                        "modelIntentClsScore": {},
                        "history": "cn.yunzhisheng.setting.mp",
                        "source": "nlu",
                        "uniCarRet": {
                            "result": {},
                            "returnCode": 609,
                            "message": "http post reuqest error"
                        },
                        "asr_recongize": "播放夜曲。",
                        "rc": 0,
                        "general": {
                            "text": "好的，已为您播放",
                            "type": "T"
                        },
                        "returnCode": 0,
                        "audioUrl": "http://asrv3.hivoice.cn/trafficRouter/r/yxOMl6",
                        "retTag": "nlu",
                        "service": "cn.yunzhisheng.music",
                        "nluProcessTime": "255",
                        "text": "播放夜曲",
                        "responseId": "a29c3e44b24f4794afb3f50f1e5a5ab7"
                    }
                    """);
        } catch (JsonProcessingException e) {
            return null;

        }
    }
}
