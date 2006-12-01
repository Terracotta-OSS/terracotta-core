/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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
