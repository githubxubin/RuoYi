package com.ruoyi.web.controller.system;

import static org.junit.Assert.assertEquals;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import jakarta.servlet.http.HttpSession;
import org.junit.Test;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.system.domain.customer.CustomerServiceMessage;
import com.ruoyi.system.domain.customer.CustomerServiceRequest;
import com.ruoyi.system.service.ICustomerService;

public class CustomerServiceControllerTest
{
    @Test
    public void rateLimitsEachSessionWithoutCallingUpstream() throws Exception
    {
        AtomicInteger calls = new AtomicInteger();
        CustomerServiceController controller = new CustomerServiceController();
        setCustomerService(controller, messages -> {
            calls.incrementAndGet();
            return "answer";
        });

        HttpSession rateLimitedSession = session();
        AjaxResult accepted = controller.chat(request(), rateLimitedSession);
        AjaxResult limited = controller.chat(request(), rateLimitedSession);
        AjaxResult otherSession = controller.chat(request(), session());

        assertEquals(0, accepted.get(AjaxResult.CODE_TAG));
        assertEquals(500, limited.get(AjaxResult.CODE_TAG));
        assertEquals("请求过于频繁，请稍后再试", limited.get(AjaxResult.MSG_TAG));
        assertEquals(0, otherSession.get(AjaxResult.CODE_TAG));
        assertEquals(2, calls.get());
    }

    private static CustomerServiceRequest request()
    {
        CustomerServiceMessage message = new CustomerServiceMessage();
        message.setRole("user");
        message.setContent("question");
        CustomerServiceRequest request = new CustomerServiceRequest();
        request.setMessages(java.util.List.of(message));
        return request;
    }

    private static HttpSession session()
    {
        Map<String, Object> attributes = new HashMap<String, Object>();
        return (HttpSession) Proxy.newProxyInstance(HttpSession.class.getClassLoader(), new Class<?>[] { HttpSession.class },
                (proxy, method, args) -> {
                    if ("getAttribute".equals(method.getName()))
                    {
                        return attributes.get(args[0]);
                    }
                    if ("setAttribute".equals(method.getName()))
                    {
                        attributes.put((String) args[0], args[1]);
                    }
                    return null;
                });
    }

    private static void setCustomerService(CustomerServiceController controller, ICustomerService service)
            throws Exception
    {
        Field field = CustomerServiceController.class.getDeclaredField("customerService");
        field.setAccessible(true);
        field.set(controller, service);
    }
}
