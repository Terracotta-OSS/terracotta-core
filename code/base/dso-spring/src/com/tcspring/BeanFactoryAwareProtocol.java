/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcspring;

import org.springframework.beans.factory.BeanFactory;

/**
 * Mixin that can be applied to all BeanFactoryAware beans, holds a publicly accessible reference to the bean factory that created bean.
 * 
 * @author Liyu Yi
 */
public class BeanFactoryAwareProtocol {

  /**
   * Advises PCD: before(execution(void saveBeanFactory(..)) && args(beanFactory) && this(bean))
   */
  public void saveBeanFactory(BeanFactoryAware bean, BeanFactory beanFactory) throws Throwable {
      bean.tc$setBeanFactory(beanFactory);
  }

  /**
   * Mixin to hold a publicly accessible reference to BeanFactory that created the bean
   * 
   * @author Liyu
   */
  public static class BeanFactoryAwareMixin implements BeanFactoryAware {
    private transient BeanFactory m_beanFactory;
    public void tc$setBeanFactory(BeanFactory factory) {
      m_beanFactory = factory;
    }

    public BeanFactory tc$getBeanFactory() {
      return m_beanFactory;
    }
  }
}
