package com.ruoyi.system.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.system.domain.customer.CustomerServiceMessage;
import com.ruoyi.system.service.CustomerServiceException;
import com.ruoyi.system.service.ICustomerService;

/**
 * DeepSeek 客服 服务层实现
 */
@Service
public class DeepSeekCustomerServiceImpl implements ICustomerService
{
    private static final int MAX_MESSAGE_COUNT = 20;

    private static final int MAX_MESSAGE_LENGTH = 2000;

    private static final int MAX_TOTAL_LENGTH = 12000;

    private static final int MAX_RESPONSE_BYTES = 1024 * 1024;

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final String apiKey;

    private final URI endpoint;

    private final String model;

    private final HttpClient httpClient;

    private final Duration requestTimeout;

    @Autowired
    public DeepSeekCustomerServiceImpl(@Value("${DEEPSEEK_API_KEY:}") String apiKey,
            @Value("${DEEPSEEK_BASE_URL:https://api.deepseek.com}") String baseUrl,
            @Value("${DEEPSEEK_MODEL:deepseek-flash}") String model)
    {
        this(apiKey, baseUrl, model, CONNECT_TIMEOUT, REQUEST_TIMEOUT);
    }

    DeepSeekCustomerServiceImpl(String apiKey, String baseUrl, String model, Duration connectTimeout,
            Duration requestTimeout)
    {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.endpoint = createEndpoint(baseUrl);
        this.model = model;
        this.httpClient = HttpClient.newBuilder().connectTimeout(connectTimeout).build();
        this.requestTimeout = requestTimeout;
    }

    @Override
    public String chat(List<CustomerServiceMessage> messages)
    {
        validate(messages);
        if (apiKey.isEmpty())
        {
            throw new CustomerServiceException("客服服务未配置");
        }

        Map<String, Object> requestBody = new LinkedHashMap<String, Object>();
        requestBody.put("model", model);
        requestBody.put("messages", messages);
        requestBody.put("stream", false);
        requestBody.put("max_tokens", 1024);

        try
        {
            HttpRequest request = HttpRequest.newBuilder(endpoint)
                    .timeout(requestTimeout)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(JSON.toJSONString(requestBody), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            return handleResponse(response);
        }
        catch (HttpTimeoutException e)
        {
            throw new CustomerServiceException("客服服务请求超时");
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new CustomerServiceException("客服服务暂时不可用");
        }
        catch (IOException | IllegalArgumentException e)
        {
            throw new CustomerServiceException("客服服务暂时不可用");
        }
    }

    private String handleResponse(HttpResponse<InputStream> response)
    {
        try (InputStream body = response.body())
        {
            int statusCode = response.statusCode();
            if (statusCode == 401)
            {
                throw new CustomerServiceException("客服服务认证失败");
            }
            if (statusCode == 429)
            {
                throw new CustomerServiceException("客服服务请求过于频繁");
            }
            if (statusCode < 200 || statusCode >= 300)
            {
                throw new CustomerServiceException("客服服务暂时不可用");
            }

            byte[] responseBytes = body.readNBytes(MAX_RESPONSE_BYTES + 1);
            if (responseBytes.length > MAX_RESPONSE_BYTES)
            {
                throw new CustomerServiceException("客服服务返回无效响应");
            }
            JSONObject root = JSON.parseObject(new String(responseBytes, StandardCharsets.UTF_8));
            JSONArray choices = root.getJSONArray("choices");
            if (choices == null || choices.isEmpty())
            {
                throw new CustomerServiceException("客服服务返回无效响应");
            }
            JSONObject message = choices.getJSONObject(0).getJSONObject("message");
            String content = message == null ? null : message.getString("content");
            if (content == null || content.trim().isEmpty())
            {
                throw new CustomerServiceException("客服服务返回无效响应");
            }
            return content;
        }
        catch (CustomerServiceException e)
        {
            throw e;
        }
        catch (IOException | RuntimeException e)
        {
            throw new CustomerServiceException("客服服务返回无效响应");
        }
    }

    private void validate(List<CustomerServiceMessage> messages)
    {
        if (messages == null || messages.isEmpty())
        {
            throw new CustomerServiceException("问题不能为空");
        }
        if (messages.size() > MAX_MESSAGE_COUNT)
        {
            throw new CustomerServiceException("对话消息数量超过限制");
        }

        int totalLength = 0;
        for (CustomerServiceMessage message : messages)
        {
            if (message == null || !("user".equals(message.getRole()) || "assistant".equals(message.getRole())))
            {
                throw new CustomerServiceException("对话消息角色无效");
            }
            String content = message.getContent();
            if (content == null || content.trim().isEmpty())
            {
                throw new CustomerServiceException("问题不能为空");
            }
            if (content.length() > MAX_MESSAGE_LENGTH)
            {
                throw new CustomerServiceException("单条消息长度超过限制");
            }
            totalLength += content.length();
            if (totalLength > MAX_TOTAL_LENGTH)
            {
                throw new CustomerServiceException("对话消息总长度超过限制");
            }
        }
        CustomerServiceMessage lastMessage = messages.get(messages.size() - 1);
        if (!"user".equals(lastMessage.getRole()))
        {
            throw new CustomerServiceException("最后一条必须为用户消息");
        }
    }

    private static URI createEndpoint(String baseUrl)
    {
        String value = baseUrl == null ? "" : baseUrl.trim();
        while (value.endsWith("/"))
        {
            value = value.substring(0, value.length() - 1);
        }
        try
        {
            URI baseUri = URI.create(value);
            String scheme = baseUri.getScheme();
            if (!baseUri.isAbsolute() || baseUri.getHost() == null
                    || !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)))
            {
                throw new CustomerServiceException("客服服务配置无效");
            }
            return URI.create(value + "/chat/completions");
        }
        catch (IllegalArgumentException e)
        {
            throw new CustomerServiceException("客服服务配置无效");
        }
    }
}
