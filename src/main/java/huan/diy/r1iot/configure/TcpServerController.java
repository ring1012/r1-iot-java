package huan.diy.r1iot.configure;

import huan.diy.r1iot.asr.AsrClient;
import huan.diy.r1iot.impl.MusicProviderService;
import huan.diy.r1iot.util.Logs;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class TcpServerController {
    private static final Logger logger = LoggerFactory.getLogger(TcpServerController.class);

    @Autowired
    private MusicProviderService musicProviderService;

    @PostConstruct
    public void startServer() {
        ExecutorService executorService = Executors.newCachedThreadPool(); // 使用缓存线程池
        executorService.submit(() -> {
            try (ServerSocket serverSocket = new ServerSocket(80)) {
                logger.info("Local server listened on port 80");
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    logger.info("Local Server: Connect from XiaoXun");
                    // 为每个客户端连接创建一个新的 ClientHandler
                    new ClientHandler(clientSocket, musicProviderService).start();
                }
            } catch (IOException e) {
                logger.error("Server error: ", e);
            }
        });
    }
}
class ClientHandler extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);
    private final Socket clientSocket;
    private final MusicProviderService musicProviderService;

    public ClientHandler(Socket socket, MusicProviderService musicProviderService) {
        this.clientSocket = socket;
        this.musicProviderService = musicProviderService;
    }

    @Override
    public void run() {
        try {
            // 读取客户端数据
            byte[] buffer = new byte[1024 * 1024 * 100]; // 1MB buffer
            int bytesRead = clientSocket.getInputStream().read(buffer);
            byte[] data = new byte[bytesRead];
            System.arraycopy(buffer, 0, data, 0, bytesRead);

            // 处理数据
            byte[] response = connectAsrServer(data);
            System.err.println("Asr Response: " + new String(response));

            // 发送响应
            clientSocket.getOutputStream().write(response);
        } catch (IOException e) {
            logger.error("Client handler error: ", e);
        } finally {
            // 确保客户端连接关闭
            try {
                clientSocket.close();
            } catch (IOException e) {
                logger.error("Failed to close client socket: ", e);
            }
        }
    }

    private byte[] connectAsrServer(byte[] data) {
        // 每次处理请求时创建一个新的 AsrClient
        try (AsrClient asrClient = new AsrClient("asrv3.hivoice.cn", 80)) {
            if (!asrClient.connect()) {
                Logs.log("Failed to connect to ASR server asrv3.hivoice.cn:80");
                return new byte[0];
            }

            // 发送数据到 ASR 服务器
            System.out.println("senddata (bytes): " + data.length);
            asrClient.send(data);

            // 接收 ASR 服务器的响应
            return asrClient.receive().getBytes();
        }
    }
}