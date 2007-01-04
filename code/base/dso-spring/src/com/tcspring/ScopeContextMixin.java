/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcspring;

import org.springframework.beans.factory.config.Scope;
import org.springframework.beans.factory.support.AbstractBeanFactory;


/**
 * Holds current Scope context
 * 
 * @author Eugene Kuleshov
 */
public class ScopeContextMixin implements ScopeContext {
  private Object beanId;
  private Scope scope;
  private AbstractBeanFactory beanFactory;

  public Object getBeanId() {
    return beanId;
  }

  public void setBeanId(Object beanId) {
    this.beanId = beanId;
  }

  public void setScope(Scope scope) {
    this.scope = scope;
  }
  
  public Scope getScope() {
    return scope;
  }
  
  public AbstractBeanFactory getBeanFactory() {
    return beanFactory;
  }
  
  public void setBeanFactory(AbstractBeanFactory beanFactory) {
    this.beanFactory = beanFactory;
  }
  
}

