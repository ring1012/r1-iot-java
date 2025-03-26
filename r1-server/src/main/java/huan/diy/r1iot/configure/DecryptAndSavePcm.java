package huan.diy.r1iot.configure;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import org.concentus.OpusDecoder; // 需要添加依赖：concentus库
import org.concentus.OpusException;

public class DecryptAndSavePcm {
    public static void main(String[] args) {
        try {
            // 读取整个请求文件
            byte[] requestBytes = Files.readAllBytes(Paths.get("E:\\workspace\\backend\\r1-iot\\samples\\1742820146473.raw"));

            // 找到body的起始位置（空行后）
//            int bodyStart = 0;
            int bodyStart = findBodyStart(requestBytes);
            if (bodyStart == -1) {
                System.out.println("无法找到body起始位置");
                return;
            }

            // 提取加密的body（根据Content-Length:311）
            int contentLength =  requestBytes.length - bodyStart;
            byte[] encryptedBytes = new byte[contentLength];
            System.arraycopy(requestBytes, bodyStart, encryptedBytes, 0, contentLength);

            // 解密得到Opus数据
            byte[] opusData = (encryptedBytes);

            // 解码Opus为PCM
            byte[] pcmData = mainDecode(opusData);
//            byte[] pcmData = decodeOpusToPcm(opusData);

            // 保存为PCM文件
            String pcmFilePath = "output.pcm";
            Files.deleteIfExists(Paths.get(pcmFilePath));
            Files.write(Paths.get(pcmFilePath), pcmData);
            System.out.println("PCM文件已保存到: " + pcmFilePath);

            // 调试：打印解码后的前几个字节
            System.out.print("PCM前10字节: ");
            for (int i = 0; i < Math.min(10, pcmData.length); i++) {
                System.out.printf("%02x ", pcmData[i]);
            }
            System.out.println();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 查找body起始位置（空行后）
    private static int findBodyStart(byte[] data) {
        for (int i = 0; i < data.length - 3; i++) {
            if (data[i] == '\r' && data[i + 1] == '\n' && data[i + 2] == '\r' && data[i + 3] == '\n') {
                return i + 4;
            }
        }
        return -1;
    }

    public static byte[] mainDecode(byte[] opusFrame) throws OpusException {
        int sampleRate = 16000;
        int channels = 1;
        int frameTimeWindow = 20; // 单帧 20ms，与编码端匹配

        // 创建解码器
        OpusDecoder decoder = new OpusDecoder(sampleRate, channels);

        // 单帧最大样本数（60ms 以防万一）
        int maxFrameSize = sampleRate * frameTimeWindow / 1000;
        short[] pcmBuffer = new short[maxFrameSize * channels];

        // 用于累积所有帧的 PCM 数据
        ByteBuffer pcmTotal = ByteBuffer.allocate(16000 * channels * 2) // 假设 0.5 秒
                .order(ByteOrder.LITTLE_ENDIAN);

        int offset = 0;

        // 循环解码所有帧
        while (offset < opusFrame.length) {
            // 检查剩余长度是否够读取头部
            if (offset + 2 > opusFrame.length) {
                throw new OpusException("Incomplete frame header at offset " + offset);
            }

            // 读取长度（小端序）
            int len = (Byte.toUnsignedInt(opusFrame[offset])) |
                    (Byte.toUnsignedInt(opusFrame[offset + 1]) << 8);
            offset += 2;

            // 检查数据是否完整
            if (offset + len > opusFrame.length) {
                throw new OpusException("Incomplete Opus data at offset " + offset);
            }

            // 提取 Opus 数据
            byte[] opusData = new byte[len];
            System.arraycopy(opusFrame, offset, opusData, 0, len);
            offset += len;

            // 解码单帧
            int samplesDecoded = decoder.decode(opusData, 0, len, pcmBuffer, 0, maxFrameSize, false);
            if (samplesDecoded < 0) {
                throw new OpusException("Decode failed: " + samplesDecoded);
            }

            // 将解码后的 PCM 数据追加到总缓冲区（直接操作ByteBuffer）
            for (int i = 0; i < samplesDecoded * channels; i++) {
                pcmTotal.putShort(pcmBuffer[i]);
            }
        }

        // 获取完整 PCM 数据
        byte[] pcmBytes = new byte[pcmTotal.position()];
        pcmTotal.rewind();
        pcmTotal.get(pcmBytes);
        return pcmBytes;
    }

    public static byte[] decodeOpusToPcm(byte[] opusData) throws Exception {
        // 参数配置
        final int SAMPLE_RATE = 16000;
        final int CHANNELS = 1;
        final int MAX_FRAME_SIZE = 640; // 最大支持 120ms 帧（最多5760样本）

        // 创建解码器
        OpusDecoder decoder = new OpusDecoder(SAMPLE_RATE, CHANNELS);

        // 解码输出 short[] 缓冲区
        short[] pcmSamples = new short[MAX_FRAME_SIZE * CHANNELS];
        int decodedSamples;

        try {
            // 解码（输入数据是一帧）
            decodedSamples = decoder.decode(
                    opusData,
                    0,
                    opusData.length,
                    pcmSamples,
                    0,
                    MAX_FRAME_SIZE,
                    false
            );

            if (decodedSamples <= 0) {
                throw new Exception("Decoding failed, no samples decoded");
            }

            // 转换 short[] → byte[]（16-bit little-endian PCM）
            byte[] pcmBytes = new byte[decodedSamples * CHANNELS * 2];
            for (int i = 0; i < decodedSamples * CHANNELS; i++) {
                pcmBytes[i * 2] = (byte) (pcmSamples[i] & 0xFF);
                pcmBytes[i * 2 + 1] = (byte) ((pcmSamples[i] >> 8) & 0xFF);
            }

            return pcmBytes;

        } catch (OpusException e) {
            throw new Exception("Opus decoding error: " + e.getMessage(), e);
        }
    }
}