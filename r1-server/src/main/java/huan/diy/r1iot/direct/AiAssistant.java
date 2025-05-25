package huan.diy.r1iot.direct;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.data.message.*;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.tool.DefaultToolExecutor;
import dev.langchain4j.service.tool.ToolExecutor;
import huan.diy.r1iot.util.R1IotUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static dev.langchain4j.data.message.UserMessage.userMessage;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class AiAssistant {

    private ChatLanguageModel openAiModel;
    private String systemPrompt;
    private BoxDecision boxDecision;
    private ChatMemory chatMemory;


    public String chat(String text) {
        List<ToolSpecification> toolSpecifications = ToolSpecifications.toolSpecificationsFrom(boxDecision);
        List<ChatMessage> chatMessages = chatMemory.messages();
        UserMessage userMessage = userMessage(text);

        List<ChatMessage> reqMessages = new ArrayList<>();
        reqMessages.add(userMessage);
        reqMessages.add(new SystemMessage(systemPrompt + """
                
                注意：
                你是一个中文助手百科全书，用简体中文回答用户的提问！
                """));
        reqMessages.addAll(chatMessages);

//        reqMessages.addFirst(userMessage);
        ChatRequest chatRequest = ChatRequest.builder()
                .messages(reqMessages)
                .parameters(ChatRequestParameters.builder()
                        .toolSpecifications(toolSpecifications)
                        .build())
                .build();


        ChatResponse chatResponse = openAiModel.chat(chatRequest);
        chatMessages.add(userMessage);

        AiMessage aiMessage = chatResponse.aiMessage();

        List<ToolExecutionRequest> toolExecutionRequests = aiMessage.toolExecutionRequests();

        if (CollectionUtils.isEmpty(toolExecutionRequests)) {
            chatMessages.add(aiMessage);
            return aiMessage.text();
        }
        var toolExecutionRequest = toolExecutionRequests.get(0);
        ToolExecutor toolExecutor = new DefaultToolExecutor(boxDecision, toolExecutionRequest);
        String result = toolExecutor.execute(toolExecutionRequest, UUID.randomUUID().toString());
        ToolExecutionResultMessage toolExecutionResultMessage = ToolExecutionResultMessage.from(toolExecutionRequest, result);

        log.info(toolExecutionResultMessage.toString());
//        chatMessages.add(toolExecutionResultMessage);

        return null;

    }

}
