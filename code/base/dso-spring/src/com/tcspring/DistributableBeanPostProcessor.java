/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tcspring;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;

import java.beans.PropertyDescriptor;

/**
 * Post process local beans for distributing.
 * 
 * @author Eugene Kuleshov
 */
public class DistributableBeanPostProcessor implements InstantiationAwareBeanPostProcessor {

  private final transient Log            logger = LogFactory.getLog(getClass());

  private final DistributableBeanFactory factory;

  public DistributableBeanPostProcessor(DistributableBeanFactory factory) {
    this.factory = factory;
  }

  public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
    logger.info(factory.getId() + " post processing before initialization " + isDistributed(beanName));
    return bean;
  }

  public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
    logger.info(factory.getId() + " post processing after initialization " + isDistributed(beanName));
    if (factory.isDistributedSingleton(beanName)) {
      ComplexBeanId beanId = new ComplexBeanId(beanName);
      BeanContainer container = factory.getBeanContainer(beanId);
      if (container == null) {
        logger.info(factory.getId() + " distributing new bean " + beanName);
        factory.putBeanContainer(beanId, new BeanContainer(bean, true));
      } else {
        logger.info(factory.getId() + " initializing existing bean " + beanName);
        factory.initializeBean(beanId, bean, container);
        container.setInitialized(true);
        return container.getBean();
      }
    }
    return bean;
  }

  public Object postProcessBeforeInstantiation(Class beanClass, String beanName) throws BeansException {
    logger.info(factory.getId() + " post processing before instantiation " + isDistributed(beanName));
    return null;
  }

  public boolean postProcessAfterInstantiation(Object bean, String beanName) throws BeansException {
    logger.info(factory.getId() + " post processing after instantiation " + isDistributed(beanName));
    return true;
  }

  public PropertyValues postProcessPropertyValues(PropertyValues pvs, PropertyDescriptor[] pds, Object bean,
                                                  String beanName) throws BeansException {
    logger.info(factory.getId() + " post processing property values " + isDistributed(beanName));
    return pvs;
  }

  private String isDistributed(String beanName) {
    return (factory.isDistributedSingleton(beanName) ? "distributed" : "local") + " bean " + beanName;
  }

}
