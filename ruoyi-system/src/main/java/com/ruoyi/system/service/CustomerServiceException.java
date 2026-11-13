package com.ruoyi.system.service;

/**
 * 客服服务异常
 */
public class CustomerServiceException extends RuntimeException
{
    private static final long serialVersionUID = 1L;

    public CustomerServiceException(String message)
    {
        super(message);
    }
}
