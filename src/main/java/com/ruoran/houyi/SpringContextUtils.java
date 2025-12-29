package com.ruoran.houyi;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * @author renlu
 * created by renlu at 2021/7/15 10:45 上午
 */
@Component
@Slf4j
public class SpringContextUtils implements ApplicationContextAware {
    ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
    }

    public void setContext(ApplicationContext applicationContext) {
        this.context = applicationContext;
    }

    public ApplicationContext getContext() {
        return context;
    }

    public void autowireBean(Object bean) {
        this.context.getAutowireCapableBeanFactory().autowireBean(bean);
    }

    public <T> T getBean(Class<T> clazz) {
        return context.getBean(clazz);
    }

}
