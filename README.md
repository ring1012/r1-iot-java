## TODO

- chat：admin配置页面，配置ai key以及选择ai平台. ChatHistory by client id
- music：音乐admin配置页面，允许个性化登录音乐平台。获取音乐源。切换音乐源。
- news：抓新闻源，切换多个新闻源
- iot：和hass的API集成。可以考虑引入AI，解析用户的语言成为hass的api input。如果有hacs插件那就更好了。
- 探索接受原始音频数据，切换asr服务商（long term）。


### music

```json

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

```

### chat

```json
{
    "code": "ANSWER",
    "matchType": "NOT_UNDERSTAND",
    "originIntent": {
        "nluSlotInfos": []
    },
    "confidence": 0.088038474,
    "modelIntentClsScore": {},
    "history": "cn.yunzhisheng.chat",
    "source": "krc",
    "uniCarRet": {
        "result": {},
        "returnCode": 609,
        "message": "http post reuqest error"
    },
    "asr_recongize": "地球半径。",
    "rc": 0,
    "general": {
        "mood": "中性",
        "style": "LOW_QUALITY",
        "text": "半径。一根香蕉？"
    },
    "returnCode": 0,
    "audioUrl": "http://asrv3.hivoice.cn/trafficRouter/r/uuXMl6",
    "retTag": "nlu",
    "service": "cn.yunzhisheng.chat",
    "nluProcessTime": "359",
    "text": "地球半径",
    "responseId": "9e4a90b596564f93bcbced18a45e0b8c"
}

```


### news

```json
{
  "semantic": {
    "intent": {
      "keyword": "HEADLINE"
    }
  },
  "code": "SEARCH",
  "data": {
    "result": {
      "count": 1,
      "dataSourceName": "NewsAPI",
      "errorCode": 0,
      "news": [
        {
          "audioUrl": "https://ting8.yymp3.com/new18/murongxx2/5.mp3",
          "createdTime": "2023-10-01T12:34:56Z",
          "duration": 120.5,
          "humanTime": "2 hours ago",
          "id": "news-12345",
          "imageUrl": "https://example.com/image/123",
          "summary": "This is a summary of the news article.",
          "tags": "technology, AI",
          "text": "This is the full text of the news article about advancements in AI technology.",
          "title": "AI Breakthrough in 2023",
          "updatedTime": "2023-10-01T13:00:00Z"
        }
      ],
      "searchType": "latest",
      "totalTime": 300,
      "tts": "This is a text-to-speech representation of the news."
    }
  },
  "matchType": "ONLY_NEED_WORD_STRING_TEMPLATE",
  "originIntent": {
    "nluSlotInfos": []
  },
  "confidence": 0.99,
  "modelIntentClsScore": {},
  "history": "cn.yunzhisheng.news",
  "source": "nlu",
  "uniCarRet": {
    "result": {},
    "returnCode": 609,
    "message": "http post reuqest error"
  },
  "asr_recongize": "播放新闻。",
  "rc": 0,
  "general": {
    "urlAlt": "新闻链接",
    "type": "U",
    "url": "http://sina.cn/nc.php?vt=4&pos=108"
  },
  "returnCode": 0,
  "audioUrl": "http://asrv3.hivoice.cn/trafficRouter/r/ykdnSi",
  "retTag": "nlu",
  "service": "cn.yunzhisheng.news",
  "nluProcessTime": "91",
  "text": "播放新闻",
  "responseId": "f7b40550cf474698909a50a92498374e"
}

```


### iot

```json
{
  "semantic": {
    "intent": {
      "operations": [
        {
          "deviceType": "OBJ_AC",
          "deviceExpr": "空调",
          "roomExpr": "主卧",
          "operator": "ACT_OPEN",
          "roomType": "BED_MASTER_ROOM"
        }
      ]
    }
  },
  "code": "SETTING_EXEC",
  "matchType": "ONLY_NEED_WORD_STRING_TEMPLATE",
  "originIntent": {
    "nluSlotInfos": []
  },
  "confidence": 0.99,
  "modelIntentClsScore": {},
  "history": "cn.yunzhisheng.setting",
  "source": "nlu",
  "uniCarRet": {
    "result": {},
    "returnCode": 609,
    "message": "http post reuqest error"
  },
  "asr_recongize": "打开主卧的空调。",
  "rc": 0,
  "general": {
    "actionAble": "true",
    "quitDialog": "true",
    "text": "好的，已为您打开主卧空调",
    "type": "T"
  },
  "returnCode": 0,
  "audioUrl": "http://asrv3.hivoice.cn/trafficRouter/r/35WEyE",
  "retTag": "nlu",
  "service": "cn.yunzhisheng.setting",
  "nluProcessTime": "157",
  "text": "打开主卧的\ufffd\ufffd\ufffd调",
  "responseId": "376ca36a690449d7b9c4388b1aca3838"
}

```

### 教程

基于官网3448，如果你root了，或者刷机了，请保证所有的apk都是官方的。如果你安装了其他免root升级apk，请卸载，防止接口不兼容。


#### 步骤
1. 找一台带docker的服务器，可以是家里的某台nas/windows机，最好能访问外网（因为部分大模型需要外网环境）
2. docker run --restart=always --name="r1"  -it -d -p 80:80 -p 8080:8080 -e password=123456 registry.cn-hangzhou.aliyuncs.com/ring1012/r1:2025-03-28  请把123456替换成你喜欢的密码
3. 家里的路由器，找到自定义Hosts，添加一行 **192.168.2.66 asrv3.hivoice.cn** 其中192.168.2.66是你的服务器ip，可以是局域网ip，也可以是公网ip
   如果是运行在旁路由上：docker run --restart=always --name="r1" -it -d --network=host -e password=123456 registry.cn-hangzhou.aliyuncs.com/ring1012/r1:2025-03-28
4. 打开192.168.2.66:8080, 输入你上面配置的密码。
5. 喊一句小讯小讯，刷新一下页面，设备号自动添加。
6. 配置自己喜欢的ai服务，enjoy


### 卢恩
如果你觉得我的方案对你有帮助，感谢打赏！


<p align="center">
  <img src="pics/wx.jpg" width="45%" />
  <img src="pics/zfb.jpg" width="45%" />
</p>



