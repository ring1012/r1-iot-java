package huan.diy.r1iot.helper;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
public class KeepClientTCPAlive {

    @Async("taskExecutor")
    public Future<?> startKeepAliveTask(String httpResponse, Channel clientChannel, AtomicInteger delayMs) {
        KeepAliveTask task = new KeepAliveTask(httpResponse, clientChannel, delayMs);
        return CompletableFuture.runAsync(task);
    }

    public static class KeepAliveTask implements Runnable {
        private final Channel clientChannel;
        private final String httpResponse;
        private final AtomicInteger delayMs;
        private final PnState state = new PnState();

        public KeepAliveTask(String httpResponse, Channel clientChannel, AtomicInteger delayMs) {
            this.clientChannel = clientChannel;
            this.httpResponse = httpResponse;
            this.delayMs = delayMs;
        }

        @Override
        public void run() {
            try {
                while (!(state.pnPrefix.equals("r") && state.currentChar == 'y')) {
                    TimeUnit.MILLISECONDS.sleep(delayMs.get());
                    String updatedResponse = httpResponse.replaceAll(
                            "PN: .*",
                            "PN: " + (state.pnPrefix.isEmpty() ? state.currentChar : state.pnPrefix + state.currentChar)
                    );
//                    log.info("PN response: {}", updatedResponse);
                    ByteBuf buf = clientChannel.alloc().buffer().writeBytes(updatedResponse.getBytes(StandardCharsets.UTF_8));
                    clientChannel.writeAndFlush(buf);
                    updatePnSequence(state);

                }
            } catch (InterruptedException e) {
                System.out.println("[任务] 已响应中断请求，安全退出");
                Thread.currentThread().interrupt();
            }
        }

        private void updatePnSequence(PnState state) {
            if (state.pnPrefix.isEmpty()) {
                if (state.currentChar < 'z') state.currentChar++; // 单字母阶段到 'z'
                else {
                    state.pnPrefix = "r"; // 进入双字母阶段
                    state.currentChar = 'q'; // 从 'q' 开始
                }
            } else {
                if (state.currentChar < 'y') state.currentChar++; // 双字母阶段到 'x'
                else {
                    state.pnPrefix = ""; // 重置回单字母阶段
                    state.currentChar = 'r'; // 重新从 'r' 开始
                }
            }
        }

        public static class PnState {
            String pnPrefix = "";
            char currentChar = 'r';
        }
    }
}