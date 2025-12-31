package huan.diy.r1iot.direct;

import com.fasterxml.jackson.core.JsonProcessingException;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.data.message.*;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatRequestParameters;
import dev.langchain4j.service.tool.DefaultToolExecutor;
import dev.langchain4j.service.tool.ToolExecutor;
import huan.diy.r1iot.util.R1IotUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;

import static dev.langchain4j.data.message.UserMessage.userMessage;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class AiAssistant {

    private ChatModel openAiModel;
    private String systemPrompt;
    private BoxDecision boxDecision;
    private ChatMemory chatMemory;
    private boolean firstMsg;

    public static String now() {
        ZoneId zoneId = ZoneId.of("Asia/Shanghai");

        // 获取当前东八区时间
        ZonedDateTime now = ZonedDateTime.now(zoneId);

        // 定义日期时间格式
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH时mm分ss秒");

        // 格式化日期时间
        String formattedDateTime = now.format(formatter);

        // 获取星期几
        DayOfWeek dayOfWeek = now.getDayOfWeek();
        String weekDay = dayOfWeek.getDisplayName(TextStyle.FULL, Locale.CHINA);

        return "现在是" + formattedDateTime + ", " + weekDay;
    }

    public String chat(String text) {

        List<ToolSpecification> toolSpecifications = ToolSpecifications.toolSpecificationsFrom(boxDecision);
        List<ChatMessage> chatMessages = chatMemory.messages();
        UserMessage userMessage = userMessage(text);

        List<ChatMessage> reqMessages = new ArrayList<>();
        if (firstMsg) {
            reqMessages.add(userMessage);
        }
        reqMessages.add(new SystemMessage(now() + "\n" + systemPrompt + """
                
                        注意：你可以讲笑话，回答问题，翻译，等等，无所不能。
                        用简体中文回复问题，如果没有工具可选，请直接回答问题。
                """));
//        reqMessages.addAll(chatMessages);
        if (!firstMsg) {
            reqMessages.add(userMessage);
        }

        Map<String, Object> custom = new HashMap<>();
        String modelName = Optional.ofNullable(this.boxDecision.getDevice().getAiConfig().getModel()).orElse("");
        if (modelName.toLowerCase().contains("glm")) {
            Map<String, Object> thinking = new HashMap<>();
            thinking.put("type", "disabled");
            custom.put("thinking", thinking);
        }
        if (modelName.toLowerCase().contains("qwen")){
            custom.put("enable_thinking", false);
        }
        if(StringUtils.hasLength(this.boxDecision.getDevice().getAiConfig().getCdn())){
            custom.put("real", this.boxDecision.getDevice().getAiConfig().getEndpoint());
        }
        ChatRequest chatRequest = ChatRequest.builder()
                .messages(reqMessages)
                .parameters(OpenAiChatRequestParameters.builder()
                        .toolSpecifications(toolSpecifications)
                        .customParameters(custom)
                        .build())
                .build();

        ChatResponse chatResponse = openAiModel.chat(chatRequest);
        chatMessages.add(userMessage);

        AiMessage aiMessage = chatResponse.aiMessage();

        List<ToolExecutionRequest> toolExecutionRequests = aiMessage.toolExecutionRequests();

        if (CollectionUtils.isEmpty(toolExecutionRequests)) {
            chatMessages.add(aiMessage);
            return aiMessage.text().replaceAll(R1IotUtils.CHINESE, "").replaceAll("\\*\\*", "");
        }
        var toolExecutionRequest = toolExecutionRequests.get(0);
        log.info("ToolExecutionRequest: {}", (toolExecutionRequest.toString()));
        ToolExecutor toolExecutor = new DefaultToolExecutor(boxDecision, toolExecutionRequest);
        String result = toolExecutor.execute(toolExecutionRequest, UUID.randomUUID().toString());
        ToolExecutionResultMessage toolExecutionResultMessage = ToolExecutionResultMessage.from(toolExecutionRequest, result);

        log.info(toolExecutionResultMessage.toString());
//        chatMessages.add(toolExecutionResultMessage);

        return null;

    }

}
