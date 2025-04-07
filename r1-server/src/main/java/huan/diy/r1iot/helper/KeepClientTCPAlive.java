package huan.diy.r1iot.helper;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import java.util.concurrent.atomic.AtomicReference;

@Component
@Slf4j
public class KeepClientTCPAlive {

    @Async("taskExecutor")
    public Future<?> startKeepAliveTask(String httpResponse, Channel clientChannel, AtomicInteger delayMs, AtomicReference<String> stopPrefixRef) {
        KeepAliveTask task = new KeepAliveTask(httpResponse, clientChannel, delayMs, stopPrefixRef);
        return CompletableFuture.runAsync(task);
    }

    public static class KeepAliveTask implements Runnable {
        private final Channel clientChannel;
        private final String httpResponse;
        private final AtomicInteger delayMs;
        private final AtomicReference<String> stopPrefixRef;
        private final PnState state = new PnState();

        public KeepAliveTask(String httpResponse, Channel clientChannel, AtomicInteger delayMs, AtomicReference<String> stopPrefixRef) {
            this.clientChannel = clientChannel;
            this.httpResponse = httpResponse;
            this.delayMs = delayMs;
            this.stopPrefixRef = stopPrefixRef;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    TimeUnit.MILLISECONDS.sleep(delayMs.get());

                    String pnValue = state.pnPrefix.isEmpty()
                            ? String.valueOf(state.currentChar)
                            : state.pnPrefix +  state.currentChar ;

                    String stopPrefix = stopPrefixRef.get();
                    if (pnValue.equals(stopPrefix) || pnValue.equals("zz")) {
//                        log.info("达到终止条件: {}", pnValue);
                        break;
                    }

                    String updatedResponse = httpResponse.replaceAll("PN: .*", "PN: " + pnValue);
//                    log.info("PN response: {}", updatedResponse);

                    ByteBuf buf = clientChannel.alloc().buffer().writeBytes(updatedResponse.getBytes());
                    clientChannel.writeAndFlush(buf);

                    updatePnSequence(state);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        private void updatePnSequence(PnState state) {
            if (state.pnPrefix.isEmpty()) {
                // 单字符阶段，从 'r' 到 'z'
                if (state.currentChar < 'z') {
                    state.currentChar++;
                } else {
                    // 进入双字符阶段，从 'r' 开始，第二个字符从 'q'
                    state.pnPrefix = "r";
                    state.currentChar = 'q';
                }
            } else {
                // 双字符阶段
                if (state.currentChar < 'z') {
                    state.currentChar++;
                } else {
                    // 第二个字符已到 'z'，循环回 'q'
                    state.currentChar = 'q';

                    char nextPrefixChar = (char) (state.pnPrefix.charAt(0) + 1);
                    if (nextPrefixChar <= 'z') {
                        state.pnPrefix = String.valueOf(nextPrefixChar);
                    } else {
                        // 到了 'zz'，可以选择终止或重置（你可以根据需求改这里）
                        log.info("pnPrefix 超过 z，已到达循环结束。");
                        state.pnPrefix = "z";
                        state.currentChar = 'z';
                    }
                }
            }
        }


        public static class PnState {
            String pnPrefix = "";
            char currentChar = 'r';
        }
    }
}

