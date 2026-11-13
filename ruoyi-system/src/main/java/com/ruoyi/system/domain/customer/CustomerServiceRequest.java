package com.ruoyi.system.domain.customer;

import java.util.List;

/**
 * 客服对话请求
 */
public class CustomerServiceRequest
{
    private List<CustomerServiceMessage> messages;

    public List<CustomerServiceMessage> getMessages()
    {
        return messages;
    }

    public void setMessages(List<CustomerServiceMessage> messages)
    {
        this.messages = messages;
    }
}
