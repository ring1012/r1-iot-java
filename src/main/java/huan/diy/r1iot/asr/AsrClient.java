package huan.diy.r1iot.asr;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class AsrClient implements AutoCloseable {
    private String host;
    private int port;
    private Socket socket;
    private InputStream inputStream;
    private OutputStream outputStream;

    public AsrClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * 连接到 ASR 服务器
     */
    public boolean connect() {
        try {
            socket = new Socket(host, port);
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();
            return true;
        } catch (IOException e) {
            System.err.println("Failed to connect to ASR server: " + e.getMessage());
            return false;
        }
    }

    /**
     * 发送数据到 ASR 服务器
     */
    public void send(byte[] data) {
        try {
            outputStream.write(data);
            outputStream.flush();
        } catch (IOException e) {
            System.err.println("Failed to send data to ASR server: " + e.getMessage());
        }
    }

    /**
     * 从 ASR 服务器接收数据
     */
    public String receive() {
        try {
            byte[] buffer = new byte[1024 * 1024]; // 1MB buffer
            int bytesRead = inputStream.read(buffer);
            return new String(buffer, 0, bytesRead);
        } catch (IOException e) {
            System.err.println("Failed to receive data from ASR server: " + e.getMessage());
            return "";
        }
    }

    /**
     * 关闭连接
     */
    @Override
    public void close() {
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Failed to close ASR client connection: " + e.getMessage());
        }
    }
}