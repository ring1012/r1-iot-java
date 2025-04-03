package huan.diy.r1iot.tools;

import dev.langchain4j.agent.tool.Tool;

public class AiTools {

    @Tool
    public String hello(String name) {
        return "Hello " + name + "!";
    }

}
