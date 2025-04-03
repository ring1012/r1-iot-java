package huan.diy.r1iot;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.junit.jupiter.api.Test;

import java.util.function.Function;

public class AITest {


    static class BoxDecision {

        @Tool("""
                用于回答用户的一般提问
                """)
        String questionAnswer(@P("用户输入") String userInput) {
            System.out.println("Called questionAnswer with userInput=" + userInput);
            return userInput;
        }

        @Tool("""
                用于处理播放音乐请求
                """)
        void playMusic(@P(value = "歌曲作者，可以为空字符串", required = false) String author,
                       @P(value = "歌曲名称，可以为空字符串", required = false) String songName,
                       @P(value = "歌曲搜索关键词，可以为空字符串", required = false) String keyword) {
            System.out.println("Called playMusic with author=" + author + ", songName=" + songName + ", keyword=" + keyword);
        }

        @Tool("""
                音箱一般设置：氛围灯，音量，停止，休眠等等
                """)
        void voiceBoxSetting(@P("用户输入") String userInput) {
            System.out.println("Called voiceBoxSetting with userInput=" + userInput);
        }

        @Tool("""
                智能家居控制，比如打开灯、热得快，空调，调节温度，查询湿度，等等
                """)
        void homeassistant(String actionCommand) {
            System.out.println("Called homeassistant with  actionCommand=" + actionCommand );
        }

        @Tool("""
                用于播放新闻，比如体育、财经、科技、娱乐等等
                """)
        void playNews(@P("用户输入") String userInput) {
            System.out.println("Called playNews with userInput=" + userInput);
        }

        @Tool("""
                用于播放故事、广播等
                """)
        void playAudio(@P("关键词") String keyword) {
            System.out.println("Called playAudio with userInput=" + keyword);
        }


    }

    interface Assistant {

        String chat(String userMessage);
    }

    @Test
    public void mainTest() {

        Function<Object, String> systemMessageProvider = (context) -> {
            return """
                你是一个智能音箱助手，负责处理用户的语音指令。
                
                注意：
                你每次只需要使用一个功能，不要同时使用多个功能。
                """;
        };

        ChatLanguageModel model = OpenAiChatModel.builder()
                .baseUrl("https://api.x.ai/v1")
                .apiKey("xai-MAPslh")
                .modelName("grok-2-latest")
                .strictTools(false)
                .build();

        Assistant assistant = AiServices.builder(Assistant.class)
                .chatLanguageModel(model)

                .tools(new BoxDecision())
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .systemMessageProvider(systemMessageProvider) // 使用 Provider 方式设置 SystemMessage

                .build();

        String question = "小白兔白又白是个啥？";

        String answer = assistant.chat(question);
         answer = assistant.chat("播放它");

        System.out.println(answer);
        // The square root of the sum of the number of letters in the words "hello" and "world" is approximately 3.162.
    }


}
