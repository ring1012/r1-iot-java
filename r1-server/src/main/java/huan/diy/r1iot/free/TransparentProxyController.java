package huan.diy.r1iot.free;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import huan.diy.r1iot.direct.AIDirect;
import huan.diy.r1iot.direct.AiAssistant;
import huan.diy.r1iot.util.R1IotUtils;
import huan.diy.r1iot.util.TcpChannelUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.DefaultHttpRequestRetryStrategy;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@RestController
@RequestMapping("/trafficRouter")
public class TransparentProxyController {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AIDirect aiDirect;

    private static final PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();

    // 配置连接池参数
    static {
        connectionManager.setMaxTotal(100);          // 总最大连接数
        connectionManager.setDefaultMaxPerRoute(20); // 每个路由的默认最大连接数
    }

    // 创建 HttpClient 实例
    private static final CloseableHttpClient httpClient = HttpClients.custom()
            .setConnectionManager(connectionManager) // 绑定连接池管理器
            .setDefaultRequestConfig(RequestConfig.custom()
                    .setConnectTimeout(Timeout.ofSeconds(5))     // 连接超时 5 秒
                    .setResponseTimeout(Timeout.ofSeconds(30))    // 响应超时 30 秒
                    .build())
            .setRetryStrategy(new DefaultHttpRequestRetryStrategy(3, Timeout.ofSeconds(1))) // 重试 3 次
            .build();

    private static final Cache<String, String> SID_DEVICE_CACHE = CacheBuilder.newBuilder()
            .expireAfterWrite(50, TimeUnit.SECONDS)
            .maximumSize(200)
            .build();

    private final Pattern SID_PATTERN = Pattern.compile("\\[(.*?)\\]");


    private Map<String, StringBuffer> ASR_MAP = new ConcurrentHashMap<>();
    private Map<String, String> DEVICE_IP = new ConcurrentHashMap<>();

    @PostMapping("/cs")
    public ResponseEntity<byte[]> proxyRequest(HttpServletRequest request) {
        try (
                InputStream inputStream = request.getInputStream();
                ByteArrayOutputStream baos = new ByteArrayOutputStream()
        ) {
            inputStream.transferTo(baos);
            byte[] requestBody = baos.toByteArray();
            // 创建代理 POST 请求
            HttpPost proxyPost = new HttpPost("http://" + TcpChannelUtils.REMOTE_HOST + "/trafficRouter/cs");

            // 获取 Content-Type
            String contentTypeHeader = request.getContentType();
            ContentType contentType = contentTypeHeader != null ? ContentType.parse(contentTypeHeader) : ContentType.APPLICATION_OCTET_STREAM;

            proxyPost.setEntity(new ByteArrayEntity(requestBody, contentType));

            // ✅ 完整复制所有请求头，包括 Host，但跳过自动处理的头部
            Enumeration<String> headerNames = request.getHeaderNames();
            String deviceId = request.getHeader("UI");
            String sidWrapper = request.getHeader("P");
            if (deviceId != null) {
                R1IotUtils.setCurrentDeviceId(deviceId);

                String clientIp = request.getRemoteAddr();
                DEVICE_IP.put(deviceId, clientIp);
                if (!R1IotUtils.getDeviceMap().containsKey(deviceId)) {
                    throw new RuntimeException("device not found: " + deviceId);
                }
                Matcher matcher = SID_PATTERN.matcher(sidWrapper);

                while (matcher.find()) {
                    SID_DEVICE_CACHE.put(matcher.group(1), deviceId);
                }
            }
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                if (headerName.equalsIgnoreCase("Content-Length") ||
                        headerName.equalsIgnoreCase("Transfer-Encoding") ||
                        headerName.equalsIgnoreCase("Content-Encoding")) {
                    continue;
                }

                Enumeration<String> values = request.getHeaders(headerName);
                while (values.hasMoreElements()) {
                    String value = values.nextElement();

                    if (headerName.equalsIgnoreCase("host")) {
                        proxyPost.setHeader("Host", value);
                    } else {
                        proxyPost.addHeader(headerName, value);
                    }
                }
            }

            // 发起代理请求
            try (CloseableHttpResponse proxyResponse = httpClient.execute(proxyPost)) {
                HttpEntity proxyEntity = proxyResponse.getEntity();
                byte[] body = proxyEntity != null ? proxyEntity.getContent().readAllBytes() : new byte[0];
                JsonNode jsonNode = objectMapper.readTree(body);
                String sid = "";

                for (Header header : proxyResponse.getHeaders()) {
                    String headerName = header.getName();
                    if (headerName.equalsIgnoreCase("SID")) {
                        sid = header.getValue();
                    }
                }

                if (!jsonNode.isEmpty()) {
                    if (!jsonNode.has("asr_recongize")) {
                        // init
                        ASR_MAP.put(sid, new StringBuffer());
                    } else {
                        ASR_MAP.computeIfAbsent(sid, k -> new StringBuffer())
                                .append(jsonNode.get("asr_recongize").asText());                    }

                }

                if (!jsonNode.has("responseId")) {

                    Thread.sleep(200);
                    HttpHeaders responseHeaders = new HttpHeaders();
                    for (Header header : proxyResponse.getHeaders()) {
                        responseHeaders.add(header.getName(), header.getValue());
                    }

                    return ResponseEntity
                            .status(proxyResponse.getCode())
                            .headers(responseHeaders)
                            .body(body);
                }


                log.info("\n==== FROM R1 ====\n {}", jsonNode);
                // end
                // 打印远端返回的 body
                try {
                    String storeDeviceId = SID_DEVICE_CACHE.get(sid, () -> null);
                    String asrResult = ASR_MAP.get(sid).toString();
                    R1IotUtils.JSON_RET.set(jsonNode);
                    R1IotUtils.CLIENT_IP.set(DEVICE_IP.get(storeDeviceId));
                    AiAssistant assistant = aiDirect.getAssistants().get(storeDeviceId);
                    String answer = assistant.chat(asrResult);
                    JsonNode fixedJsonNode = R1IotUtils.JSON_RET.get();
                    if (answer != null) {
                        fixedJsonNode = R1IotUtils.sampleChatResp(answer);
                    }

                    String responseString = objectMapper.writeValueAsString(fixedJsonNode);
                    log.info("\n==== FROM AI ====\n {}", responseString);

                    // ✅ 构建响应，拷贝所有响应头并重新设置 Content-Length
                    HttpHeaders responseHeaders = new HttpHeaders();
                    for (Header header : proxyResponse.getHeaders()) {
                        if (!header.getName().equalsIgnoreCase("Content-Length")) {
                            responseHeaders.add(header.getName(), header.getValue());
                        }
                    }
                    byte[] binary = responseString.getBytes(StandardCharsets.UTF_8);
                    responseHeaders.setContentLength(binary.length);

                    return ResponseEntity
                            .status(proxyResponse.getCode())
                            .headers(responseHeaders)
                            .body(binary);
                } catch (Exception e) {
                    return ResponseEntity
                            .status(500)
                            .body(("Proxy Error: " + e.getMessage()).getBytes());
                } finally {
                    R1IotUtils.remove();
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity
                    .status(500)
                    .body(("Proxy Error: " + e.getMessage()).getBytes());
        }
    }
}