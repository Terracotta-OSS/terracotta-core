/**
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tcspring;

import org.springframework.beans.factory.BeanFactory;

/**
 * Introduction interface to save an accessible copy of BeanFactory 
 * 
 * @author Liyu Yi
 */
public interface BeanFactoryAware {
  public void tc$setBeanFactory(BeanFactory factory);
  public BeanFactory tc$getBeanFactory();
}
