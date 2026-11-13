package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.customer.CustomerServiceMessage;

/**
 * 客服 服务层
 */
public interface ICustomerService
{
    /**
     * 获取客服回答
     *
     * @param messages 对话消息
     * @return 客服回答
     */
    String chat(List<CustomerServiceMessage> messages);
}
