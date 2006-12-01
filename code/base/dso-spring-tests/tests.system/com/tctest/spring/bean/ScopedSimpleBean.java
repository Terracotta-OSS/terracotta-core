/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.bean;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.config.Scope;

import com.tcspring.ComplexBeanId;
import com.tcspring.DistributableBeanFactory;
import com.tctest.spring.integrationtests.tests.CustomScopedBeanTest;
import com.tctest.spring.integrationtests.tests.CustomScopedBeanTest.ConversationScope;

import javax.servlet.http.HttpSessionBindingListener;

public class ScopedSimpleBean extends SimpleBean 
  implements CustomScopedBeanTest.ScopeAware, BeanNameAware, IScopedSimpleBean, BeanFactoryAware {
  private transient Scope scope = null;
  private transient String beanName = null;
  private transient BeanFactory factory = null;
  
  public void setScope(Scope scope) {
    this.scope = scope;
  }

  public String invokeDestructionCallback() {
    String rtv = "";
    if (scope instanceof ConversationScope) {
      ConversationScope convScope = (ConversationScope)scope;
      HttpSessionBindingListener listener = (HttpSessionBindingListener)convScope.getDestructionCallback(beanName);
      listener.valueUnbound(null);  // cause unbound
    }
    return rtv;
  }
  
  public void setBeanName(String name) {
    this.beanName = name;
  }
  
  public void setBeanFactory(BeanFactory factory) {
    this.factory = factory;
  }
  
  public String getScopeId() {
    return scope.getConversationId();
  }
  
  public boolean isInClusteredSingletonCache() {
    ComplexBeanId beanId = new ComplexBeanId(scope.getConversationId(), beanName, true);
    return ((DistributableBeanFactory)factory).getBeanFromSingletonCache(beanId) != null;
  }
}
