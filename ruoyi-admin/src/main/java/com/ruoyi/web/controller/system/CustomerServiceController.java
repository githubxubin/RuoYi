package com.ruoyi.web.controller.system;

import java.util.Collections;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.system.domain.customer.CustomerServiceRequest;
import com.ruoyi.system.service.CustomerServiceException;
import com.ruoyi.system.service.ICustomerService;

/**
 * 客服对话
 */
@RestController
@RequestMapping("/system/customer-service")
public class CustomerServiceController
{
    private static final String LAST_REQUEST_TIME = CustomerServiceController.class.getName() + ".lastRequestTime";

    private static final long MIN_REQUEST_INTERVAL_MILLIS = 1000L;

    @Autowired
    private ICustomerService customerService;

    @PostMapping("/chat")
    public AjaxResult chat(@RequestBody(required = false) CustomerServiceRequest request, HttpSession session)
    {
        synchronized (session)
        {
            long now = System.currentTimeMillis();
            Long lastRequestTime = (Long) session.getAttribute(LAST_REQUEST_TIME);
            if (lastRequestTime != null && now - lastRequestTime < MIN_REQUEST_INTERVAL_MILLIS)
            {
                return AjaxResult.error("请求过于频繁，请稍后再试");
            }
            session.setAttribute(LAST_REQUEST_TIME, now);
        }
        try
        {
            String answer = customerService.chat(request == null ? null : request.getMessages());
            return AjaxResult.success(Collections.singletonMap("answer", answer));
        }
        catch (CustomerServiceException e)
        {
            return AjaxResult.error(e.getMessage());
        }
    }
}
