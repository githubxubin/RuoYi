package com.ruoyi.system.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.After;
import org.junit.Test;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.ruoyi.system.domain.customer.CustomerServiceMessage;
import com.ruoyi.system.service.CustomerServiceException;

public class DeepSeekCustomerServiceImplTest
{
    private HttpServer server;

    @After
    public void tearDown()
    {
        if (server != null)
        {
            server.stop(0);
        }
    }

    @Test
    public void returnsAnswerAndSendsAuthorizationHeader() throws Exception
    {
        AtomicBoolean authorizationPresent = new AtomicBoolean(false);
        AtomicBoolean requestBodyValid = new AtomicBoolean(false);
        startServer(exchange -> {
            authorizationPresent.set(exchange.getRequestHeaders().getFirst("Authorization") != null);
            JSONObject body = JSON.parseObject(new String(exchange.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8));
            JSONArray requestMessages = body.getJSONArray("messages");
            requestBodyValid.set("model".equals(body.getString("model"))
                    && Boolean.FALSE.equals(body.getBoolean("stream"))
                    && body.getIntValue("max_tokens") == 1024
                    && requestMessages != null && requestMessages.size() == 1
                    && "user".equals(requestMessages.getJSONObject(0).getString("role"))
                    && "question".equals(requestMessages.getJSONObject(0).getString("content")));
            respond(exchange, 200, "{\"choices\":[{\"message\":{\"content\":\"hello\"}}]}");
        });

        String answer = service().chat(messages(message("user", "question")));

        assertEquals("hello", answer);
        assertTrue(authorizationPresent.get());
        assertTrue(requestBodyValid.get());
    }

    @Test
    public void rejectsMissingApiKey()
    {
        DeepSeekCustomerServiceImpl service = new DeepSeekCustomerServiceImpl("", "http://localhost", "model",
                Duration.ofSeconds(1), Duration.ofSeconds(1));

        assertError(service, messages(message("user", "question")), "客服服务未配置");
    }

    @Test
    public void rejectsInvalidInput()
    {
        DeepSeekCustomerServiceImpl service = new DeepSeekCustomerServiceImpl("key", "http://localhost", "model",
                Duration.ofSeconds(1), Duration.ofSeconds(1));
        assertError(service, Collections.emptyList(), "问题不能为空");
        assertError(service, messages(message("system", "question")), "对话消息角色无效");
        assertError(service, messages(message("user", "   ")), "问题不能为空");
        assertError(service, messages(message("user", "x".repeat(2001))), "单条消息长度超过限制");
        assertError(service, messages(message("user", "question"), message("assistant", "answer")),
                "最后一条必须为用户消息");

        List<CustomerServiceMessage> tooMany = new ArrayList<CustomerServiceMessage>();
        for (int i = 0; i < 21; i++)
        {
            tooMany.add(message("user", "x"));
        }
        assertError(service, tooMany, "对话消息数量超过限制");

        List<CustomerServiceMessage> tooLong = messages(message("user", "x".repeat(2000)),
                message("assistant", "x".repeat(2000)), message("user", "x".repeat(2000)),
                message("assistant", "x".repeat(2000)), message("user", "x".repeat(2000)),
                message("assistant", "x".repeat(2000)), message("user", "x"));
        assertError(service, tooLong, "对话消息总长度超过限制");
    }

    @Test
    public void mapsUnauthorizedResponse() throws Exception
    {
        startServer(exchange -> respond(exchange, 401, "unauthorized"));
        assertError(service(), messages(message("user", "question")), "客服服务认证失败");
    }

    @Test
    public void mapsRateLimitResponse() throws Exception
    {
        startServer(exchange -> respond(exchange, 429, "rate limited"));
        assertError(service(), messages(message("user", "question")), "客服服务请求过于频繁");
    }

    @Test
    public void mapsServerErrorResponse() throws Exception
    {
        startServer(exchange -> respond(exchange, 500, "upstream details"));
        assertError(service(), messages(message("user", "question")), "客服服务暂时不可用");
    }

    @Test
    public void mapsTimeout() throws Exception
    {
        startServer(exchange -> {
            try
            {
                Thread.sleep(300);
                respond(exchange, 200, "{\"choices\":[{\"message\":{\"content\":\"late\"}}]}");
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
            catch (IOException ignored)
            {
            }
        });
        DeepSeekCustomerServiceImpl service = new DeepSeekCustomerServiceImpl("key", baseUrl(), "model",
                Duration.ofSeconds(1), Duration.ofMillis(50));

        assertError(service, messages(message("user", "question")), "客服服务请求超时");
    }

    @Test
    public void rejectsMalformedResponse() throws Exception
    {
        startServer(exchange -> respond(exchange, 200, "{\"choices\":[]}"));
        assertError(service(), messages(message("user", "question")), "客服服务返回无效响应");
    }

    @Test
    public void rejectsOversizedResponse() throws Exception
    {
        startServer(exchange -> respond(exchange, 200, "x".repeat(1024 * 1024 + 1)));
        assertError(service(), messages(message("user", "question")), "客服服务返回无效响应");
    }

    @Test
    public void rejectsInvalidBaseUrl()
    {
        assertBaseUrlError("localhost:8080");
        assertBaseUrlError("file:///tmp/deepseek");
    }

    private DeepSeekCustomerServiceImpl service()
    {
        return new DeepSeekCustomerServiceImpl("key", baseUrl(), "model", Duration.ofSeconds(1),
                Duration.ofSeconds(1));
    }

    private void startServer(Handler handler) throws IOException
    {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/chat/completions", exchange -> handler.handle(exchange));
        server.start();
    }

    private String baseUrl()
    {
        assertNotNull(server);
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException
    {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private static CustomerServiceMessage message(String role, String content)
    {
        CustomerServiceMessage message = new CustomerServiceMessage();
        message.setRole(role);
        message.setContent(content);
        return message;
    }

    private static List<CustomerServiceMessage> messages(CustomerServiceMessage... messages)
    {
        return List.of(messages);
    }

    private static void assertError(DeepSeekCustomerServiceImpl service, List<CustomerServiceMessage> messages,
            String expectedMessage)
    {
        try
        {
            service.chat(messages);
            fail("Expected CustomerServiceException");
        }
        catch (CustomerServiceException e)
        {
            assertEquals(expectedMessage, e.getMessage());
        }
    }

    private static void assertBaseUrlError(String baseUrl)
    {
        try
        {
            new DeepSeekCustomerServiceImpl("key", baseUrl, "model", Duration.ofSeconds(1), Duration.ofSeconds(1));
            fail("Expected CustomerServiceException");
        }
        catch (CustomerServiceException e)
        {
            assertEquals("客服服务配置无效", e.getMessage());
        }
    }

    private interface Handler
    {
        void handle(HttpExchange exchange) throws IOException;
    }
}
