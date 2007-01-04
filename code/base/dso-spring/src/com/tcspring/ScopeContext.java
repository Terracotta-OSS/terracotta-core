/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcspring;

import org.springframework.beans.factory.config.Scope;
import org.springframework.beans.factory.support.AbstractBeanFactory;

/**
 * Scope context holder
 * 
 * @author Eugene Kuleshov 
 */
public interface ScopeContext {

  void setBeanId(Object beanId);
  
  void setScope(Scope scope);

  void setBeanFactory(AbstractBeanFactory beanFactory);

  Object getBeanId();
  
  Scope getScope();
  
  AbstractBeanFactory getBeanFactory();

}

