package huan.diy.r1iot.service.ai;

import dev.langchain4j.model.chat.ChatLanguageModel;
import huan.diy.r1iot.model.Device;
import huan.diy.r1iot.service.IWebAlias;

public interface IAIService extends IWebAlias {

    ChatLanguageModel buildModel(Device device);
}
