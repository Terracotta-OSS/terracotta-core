/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcspring;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;

import com.tc.aspectwerkz.joinpoint.StaticJoinPoint;
import com.tc.object.bytecode.Manager;
import com.tc.object.bytecode.ManagerUtil;


/**
 * Virtualize <code>AbstractBeanFactory.getBean()</code>.
 * 
 * @author Eugene Kuleshov
 */
public class GetBeanProtocol {
  
  private final transient Log logger = LogFactory.getLog(getClass());

  private final transient ThreadLocal beanNameCflow = new ThreadLocal() {
      protected Object initialValue() {
        return new String[1];
      }
    };
  
  
  /**
   * Invoked after constructor of the <code>AbstractBeanFactory</code>
   * 
   * @see org.springframework.beans.factory.support.AbstractBeanFactory#AbstractBeanFactory()
   * 
   * @deprecated This approach does not work because of Spring's handling of the circular dependencies 
   */
  public void registerBeanPostProcessor(StaticJoinPoint jp, AbstractBeanFactory factory) {
    if(factory instanceof DistributableBeanFactory) {
      factory.addBeanPostProcessor(new DistributableBeanPostProcessor((DistributableBeanFactory) factory));
    }
  }
  

  /**
   * Captures the name of the bean being created and makes it accessible to virtualizeSingletonBean()
   * It also maintain the locking for distributed bean initialization.
   * 
   * Invoked around <code>AbstractAutowireCapableBeanFactory.createBean(String, ..)</code> method.
   * 
   * @see org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#createBean(String, ...)
   */
  public Object beanNameCflow(StaticJoinPoint jp, String beanName, AutowireCapableBeanFactory factory) throws Throwable {
    String[] beanNameHolder = (String[]) beanNameCflow.get();
    String previousBeanName = beanNameHolder[0];
    beanNameHolder[0] = beanName;
    try {
      if (factory instanceof DistributableBeanFactory) {
        DistributableBeanFactory distributableBeanFactory = (DistributableBeanFactory) factory;
        if (distributableBeanFactory.isDistributedSingleton(beanName)) {
          logger.info(distributableBeanFactory.getId()+" distributed lock for bean " + beanName);
          String lockId = "@spring_context_" + ((DistributableBeanFactory) factory).getId() + "_" + beanName;
          ManagerUtil.beginLock(lockId, Manager.LOCK_TYPE_WRITE);
          try {
            return jp.proceed();
          } finally {
            ManagerUtil.commitLock(lockId);
          }
        }
      }
      return jp.proceed();
      
    } finally {
      beanNameHolder[0] = previousBeanName;
    }
  }

  /**
   * Virtualize singleton bean.
   * 
   * Invoked around call to <code>BeanWrapper.getWrappedInstance()</code> method within 
   * <code>AbstractAutowireCapableBeanFactory.createBean(String, ..)</code> method execution.
   * 
   * @see GetBeanProtocol#beanNameCflow(StaticJoinPoint, String, AutowireCapableBeanFactory)
   * @see org.springframework.beans.BeanWrapper#getWrappedInstance()
   * @see org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#createBean(String, ..)
   */
  public Object virtualizeSingletonBean(StaticJoinPoint jp, AutowireCapableBeanFactory beanFactory) throws Throwable {
    Object localBean = jp.proceed();

    if (beanFactory instanceof DistributableBeanFactory) {
      DistributableBeanFactory distributableBeanFactory = (DistributableBeanFactory) beanFactory;
      String beanName = ((String[]) beanNameCflow.get())[0];
      if (distributableBeanFactory.isDistributedSingleton(beanName)) {
        ComplexBeanId beanId = new ComplexBeanId(beanName);
        BeanContainer container = distributableBeanFactory.getBeanContainer(beanId);
        if (container != null) {
          logger.info(distributableBeanFactory.getId() + " virtualizing existing bean " + beanName);
          return container.getBean();
        }
        logger.info(distributableBeanFactory.getId() + " virtualizing new bean " + beanName);
        distributableBeanFactory.putBeanContainer(beanId, new BeanContainer(localBean, true));
      }
    }
    
    return localBean;
  }
  
  /**
   * Initialize singleton bean.
   * 
   * Invoked after call to <code>AbstractAutowireCapableBeanFactory.populateBean(..)</code> method
   * within execution of <code>AbstractAutowireCapableBeanFactory.createBean(String, ..))</code>
   *  
   * @see GetBeanProtocol#beanNameCflow(StaticJoinPoint, String, AutowireCapableBeanFactory)
   * @see GetBeanProtocol#virtualizeSingletonBean(StaticJoinPoint, AutowireCapableBeanFactory)
   * @see org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#createBean(String, ..)
   * @see org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#populateBean(..)
   */
  public void initializeSingletonBean(String beanName, RootBeanDefinition mergedBeanDefinition,
      BeanWrapper instanceWrapper, AutowireCapableBeanFactory beanFactory) {
    if (beanFactory instanceof DistributableBeanFactory) {
      DistributableBeanFactory distributableBeanFactory = (DistributableBeanFactory) beanFactory;
      if (distributableBeanFactory.isDistributedSingleton(beanName)) {
        ComplexBeanId beanId = new ComplexBeanId(beanName);
        BeanContainer container = distributableBeanFactory.getBeanContainer(beanId);
        if (container != null && !container.isInitialized()) {
          Object localInstance = instanceWrapper.getWrappedInstance();
          distributableBeanFactory.initializeBean(beanId, localInstance, container);
        }
      }
    }
  }
  
}

