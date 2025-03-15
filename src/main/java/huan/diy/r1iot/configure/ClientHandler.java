package huan.diy.r1iot.configure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;

public class ClientHandler implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);

    private Socket clientSocket;
    private String asrServerHost;
    private int asrServerPort;

    public ClientHandler(Socket clientSocket, String asrServerHost, int asrServerPort) {
        this.clientSocket = clientSocket;
        this.asrServerHost = asrServerHost;
        this.asrServerPort = asrServerPort;
    }

    @Override
    public void run() {
        try (InputStream clientInput = clientSocket.getInputStream();
             OutputStream clientOutput = clientSocket.getOutputStream()) {

            // Read data from the client
            byte[] data = new byte[1024 * 1024 * 5]; // 5MB buffer
            int bytesRead = clientInput.read(data);
            if (bytesRead == -1) {
                logger.info("No data received from client.");
                return;
            }

            // Trim the buffer to the actual size received
            byte[] actualData = Arrays.copyOf(data, bytesRead);
            System.err.println(new String(actualData));


            // Forward data to the ASR server
            try (Socket asrSocket = new Socket(asrServerHost, asrServerPort);
                 OutputStream asrOutput = asrSocket.getOutputStream();
                 InputStream asrInput = asrSocket.getInputStream()) {

                logger.info("Connected to ASR server at " + asrServerHost + ":" + asrServerPort);
//                asrOutput.write(actualData, 0, bytesRead);
//                asrOutput.flush();
//
//                // Read response from ASR server
//                ByteArrayOutputStream responseBuffer = new ByteArrayOutputStream();
//                int asrBytesRead;
//                byte[] buffer = new byte[10240];
//                while ((asrBytesRead = asrInput.read(buffer)) != -1) {
//                    responseBuffer.write(buffer, 0, asrBytesRead);
//                }
                byte[] responseData = """
                        {
                        	"semantic": {
                        		"intent": {
                        			"operations": [{
                        				"operator": "ACT_OPEN",
                        				"operands": "AmbientLight"
                        			}]
                        		}
                        	},
                        	"source": "nlu",
                        	"general": {
                        		"quitDialog": "true",
                        		"text": "好的，已为您打开灯",
                        		"type": "T",
                        		"actionAble": "true"
                        	},
                        	"displayProcessRecord": {
                        		"nluSlotInfos": [],
                        		"matchResult": {
                        			"operations": [{
                        				"operator": "ACT_OPEN",
                        				"operands": "OBJ_LIGHT"
                        			}],
                        			"service": "cn.yunzhisheng.setting",
                        			"code": "SETTING_EXEC"
                        		},
                        		"timeCosts": {
                        			"to do slot filling ": "0",
                        			"createResultFromMatch": "18",
                        			"prepare parameters": "0",
                        			"modify nlp resource": "0",
                        			"total time cost": "1173",
                        			"nlp doAnalysis": "22",
                        			"preprocess": "0",
                        			"nlp post": "12",
                        			"time cost in nlu exec": "1191",
                        			"createNlpRequest": "9",
                        			"prepare praram for nlu exec": "0",
                        			"executeNlu": "1162",
                        			"dispatch": "56",
                        			"modify and filter score": "0",
                        			"post process origin ner result": "0",
                        			"text rewriter": "1042",
                        			"create analyzed request": "0",
                        			"split": "0",
                        			"writeContext": "11"
                        		},
                        		"confidence": "0.99",
                        		"otherRecord": {
                        			"OriginalNER": "[打开 氛围 灯\\ttype=0\\tscore=0.9745968833525908, 打开/song_ler 氛围/video 灯\\ttype=1\\tscore=0.9374625, 打开 氛围灯/device_type\\ttype=1\\tscore=0.890596875, 打开 氛围/musicTag 灯/song\\ttype=1\\tscore=0.890596875, 打开 氛围灯/song_ler\\ttype=1\\tscore=0.890596875, 打开 氛围/musicTag 灯\\ttype=1\\tscore=0.84373125, 打开 氛围/video 灯\\ttype=1\\tscore=0.84373125, 打开/song_ler 氛围 灯\\ttype=1\\tscore=0.84373125, 打开 氛围 灯/device_type\\ttype=1\\tscore=0.796865625, 打开 氛围 灯/song\\ttype=1\\tscore=0.796865625, 打开 氛围/video 灯\\ttype=0\\tscore=0.91111, 打开 氛围/musicTag 灯/song\\ttype=2\\tscore=0.4]",
                        			"category": "[ScoredCategory [category=setting, score=0.9999069, type=CNN], ScoredCategory [category=聊天, score=0.402, ], ScoredCategory [category=应用, score=0.344, ], ScoredCategory [category=others, score=7.716967E-5, type=CNN], ScoredCategory [category=appmgr, score=1.5644857E-5, type=CNN]]",
                        			"NluPriority": "10",
                        			"MedicalNer": "{}",
                        			"NER": "[打开 氛围 灯\\ttype=-1\\tscore=1.0, 打开 氛围 灯\\ttype=0\\tscore=0.8771371950173317, 打开/song_ler 氛围/video 灯\\ttype=1\\tscore=0.84371625, 打开 氛围/video 灯\\ttype=0\\tscore=0.819999, 打开 氛围灯/device_type\\ttype=1\\tscore=0.8015371875, 打开 氛围/musicTag 灯/song\\ttype=1\\tscore=0.8015371875, 打开 氛围灯/song_ler\\ttype=1\\tscore=0.8015371875, 打开 氛围/musicTag 灯\\ttype=1\\tscore=0.7593581250000001, 打开/song_ler 氛围 灯\\ttype=1\\tscore=0.7593581250000001, 打开 氛围 灯/device_type\\ttype=1\\tscore=0.7171790625000001, 打开 氛围 灯/song\\ttype=1\\tscore=0.7171790625000001]",
                        			"useResult": "NLU",
                        			"NonRepeatText": "打开 氛围 灯",
                        			"weightedScoreMap": "{IDIOM_SOLITAIRE=.00,SPECIFIED_LAYER_1=.00,ONLY_NEED_WORD_STRING_TEMPLATE=.99}",
                        			"matchedNlgValueTemplateInfos": "1:intent~[operator:ACT_OPEN][operands:OBJ_LIGHT]\\t"
                        		},
                        		"matchedType": "ONLY_NEED_WORD_STRING_TEMPLATE",
                        		"matchedPatternId": "cn.yunzhisheng.setting:OPEN_LIGHT@1_4",
                        		"matchedInput": "打开氛围灯"
                        	},
                        	"responseId": "edab706a8a9b430aacbb884bfc846120",
                        	"history": "cn.yunzhisheng.setting",
                        	"text": "打开氛围灯",
                        	"originIntent": {
                        		"nluSlotInfos": []
                        	},
                        	"service": "cn.yunzhisheng.setting",
                        	"asr_recongize": "打开氛围灯。",
                        	"code": "SETTING_EXEC",
                        	"rc": 0
                        }
                        """.getBytes();
                logger.info("Received response from ASR server" + new String(responseData));

                // Send the response back to the client
                clientOutput.write(responseData);
                clientOutput.flush();

            } catch (IOException e) {
                logger.error( "Error communicating with ASR server", e);
            }

        } catch (IOException e) {
            logger.error( "Error handling client connection", e);
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                logger.error( "Error closing client socket", e);
            }
        }
    }
}
