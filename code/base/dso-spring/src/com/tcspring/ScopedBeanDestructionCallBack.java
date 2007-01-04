/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcspring;

import org.springframework.beans.factory.config.Scope;

/**
 * Destruction callback for distributed scoped beans 
 * 
 * @see Scope#registerDestructionCallback(String, Runnable)
 * 
 * @author Eugene Kuleshov
 */
public class ScopedBeanDestructionCallBack implements Runnable {
  private ComplexBeanId beanId;
  private transient DistributableBeanFactory beanFactory;
  private transient Runnable callback;
  
  public ScopedBeanDestructionCallBack(ComplexBeanId beanId, DistributableBeanFactory factory, Runnable callback) {
    this.beanId = beanId;
    this.beanFactory = factory;
    this.callback = callback;
  }

  public ComplexBeanId getBeanId() {
    return beanId;
  }
  
  public void setCallback(Runnable callback) {
    this.callback = callback;
  }

  public Runnable getCallback() {
    return callback;
  }
  
  public void setBeanFactory(DistributableBeanFactory beanFactory) {
    this.beanFactory = beanFactory;
  }
  
  public DistributableBeanFactory getBeanFactory() {
    return beanFactory;
  }
  
  public void run() {
    try {
      if(callback!=null) {
        callback.run();
      }
    } finally {
      if(beanFactory!=null) {
        beanFactory.removeBeanContainer(beanId);
      }
    }
  }
}