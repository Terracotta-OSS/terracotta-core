/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcspring;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.framework.AdvisedSupport;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.beans.factory.BeanFactory;

import com.tc.aspectwerkz.joinpoint.StaticJoinPoint;

/**
 * Intercepts the Spring AOP proxy creation and returns a FastProxy instead - falls back to regular creation upon failure
 * or if some requirements are not fulfilled.
 * 
 * @author Jonas Bon&#233;r
 */
public class AopProxyFactoryProtocol {
  private final transient Log logger = LogFactory.getLog(getClass());
  /**
   * Around advice that is intercepting the pointcut: 
   * <tt>
   * execution(* org.springframework.aop.framework.AopProxyFactory+.createAopProxy(..)) && args(proxyFactory)
   * </tt>
   */
  public Object createAopProxy(StaticJoinPoint jp, AdvisedSupport proxyFactory) throws Throwable {
    ApplicationHelper applicationHelper = new ApplicationHelper(proxyFactory.getClass());
    if (!applicationHelper.isDSOApplication() || !applicationHelper.isFastProxyEnabled()) { return jp.proceed(); }

    try {
      if (proxyFactory instanceof ProxyFactoryBean) {
        return new FastAopProxy((ProxyFactoryBean) proxyFactory);
      } else if (proxyFactory instanceof ProxyFactory && proxyFactory.isFrozen()) {
        return jp.proceed(); // TODO implement support for ProxyFactory later if needed
      } else {
        return jp.proceed();
      }
    } catch (Throwable e) {
      logger.warn("Falling back to using regular Spring AOP proxy creation, due to: " + e);
      // if something goes wrong fall back on using cglib or dynamic proxy
      // f.e. mixins are not supported so -> exception -> fallback to cglib
      return jp.proceed();
    }
  }
  
  /**
   * Advises PCD: before(execution(void saveBeanFactory(..)) && args(beanFactory) && this(bean))
   */
  public void saveBeanFactory(BeanFactoryAware bean, BeanFactory beanFactory) throws Throwable {
      bean.tc$setBeanFactory(beanFactory);
  }

  /**
   * Mixin to hold a publicly accessible reference to BeanFactory that created the bean
   */
  public static class BeanFactoryAwareMixin implements BeanFactoryAware {
    private transient BeanFactory beanFactory;
    
    public void tc$setBeanFactory(BeanFactory beanFactory) {
      this.beanFactory = beanFactory;
    }

    public BeanFactory tc$getBeanFactory() {
      return beanFactory;
    }
  }
  
}

