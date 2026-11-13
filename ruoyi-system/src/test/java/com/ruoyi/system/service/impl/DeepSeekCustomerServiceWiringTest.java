package com.ruoyi.system.service.impl;

import static org.junit.Assert.assertNotNull;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.MapPropertySource;
import com.ruoyi.system.service.ICustomerService;

public class DeepSeekCustomerServiceWiringTest
{
    @Configuration
    static class Config
    {
        @Bean
        static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer()
        {
            return new PropertySourcesPlaceholderConfigurer();
        }
    }

    @Test
    public void beanIsInstantiableBySpringWithoutAnyDeepSeekProperties()
    {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        Map<String, Object> properties = new HashMap<String, Object>();
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("wiring-test", properties));
        context.register(Config.class, DeepSeekCustomerServiceImpl.class);
        context.refresh();

        ICustomerService service = context.getBean(ICustomerService.class);
        assertNotNull(service);
        context.close();
    }
}
