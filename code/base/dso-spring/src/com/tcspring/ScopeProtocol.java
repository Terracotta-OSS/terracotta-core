/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcspring;

import org.springframework.beans.factory.config.Scope;
import org.springframework.beans.factory.support.AbstractBeanFactory;

import com.tc.aspectwerkz.joinpoint.StaticJoinPoint;

/**
 * @author Eugene Kuleshov
 */
public class ScopeProtocol {
  
  private final ThreadLocal scopedBeanId = new ThreadLocal();
  
  /**
   * @see org.springframework.beans.factory.support.AbstractBeanFactory#registerScope(String, Scope)
   */
  public void setDistributableBeanFactory(String scopeName, Scope scope, AbstractBeanFactory beanFactory) {
    if(beanFactory instanceof DistributableBeanFactory) {
      if(scope instanceof DistributableBeanFactoryAware) {
        ((DistributableBeanFactoryAware) scope).setBeanFactory((DistributableBeanFactory) beanFactory);
      }
    }
  }
  
  /**
   * @see org.springframework.beans.factory.config.Scope#get(String, org.springframework.beans.factory.ObjectFactory)
   */
  public Object virtualizeScopedBean(StaticJoinPoint jp, Scope s, String beanName) throws Throwable {
    if(s instanceof DistributableBeanFactoryAware) {
      DistributableBeanFactoryAware scope = (DistributableBeanFactoryAware) s;

      DistributableBeanFactory factory = scope.getBeanFactory();
      if(factory!=null && factory.isDistributedScoped(beanName)) {
        ComplexBeanId beanId = new ComplexBeanId(s.getConversationId(), beanName);
        
        BeanContainer container = factory.getBeanContainer(beanId);
        
        if(container!=null && container.isInitialized()) {
            return container.getBean();
        }
        
        if(container==null) {
          container = new BeanContainer(null, false);
          factory.putBeanContainer(beanId, container);
        }
        
        Object bean = null;
        scopedBeanId.set(beanId);
        try {
          bean = jp.proceed();
        } finally {
          scopedBeanId.set(null);
        }

        if(container.getBean()==null) {
          container.setBean(bean);
        } else {
          factory.initializeBean(beanId, bean, container);
        }

        if(container.getDestructionCallBack()==null) {
          ScopedBeanDestructionCallBack destructionCallBack = new ScopedBeanDestructionCallBack(beanId, factory, null);
          s.registerDestructionCallback(beanName, destructionCallBack);
        }

        container.setInitialized(true);
        
        return container.getBean();
      }
    }
    
    return jp.proceed();
  }
  
  /**
   * @see org.springframework.beans.factory.config.Scope#registerDestructionCallback(String, Runnable)
   */
  public Object wrapDestructionCallback(StaticJoinPoint jp, String beanName, Runnable callback, Scope scope) throws Throwable {
    if (!(callback instanceof ScopedBeanDestructionCallBack) &&
        scope instanceof DistributableBeanFactoryAware) {
      ComplexBeanId beanId = new ComplexBeanId(scope.getConversationId(), beanName);

      DistributableBeanFactoryAware distributableScope = (DistributableBeanFactoryAware) scope;
      
      DistributableBeanFactory factory = distributableScope.getBeanFactory();
      BeanContainer container = factory.getBeanContainer(beanId);
      
      ScopedBeanDestructionCallBack destructionCallBack = container.getDestructionCallBack();
      if(destructionCallBack==null) {
        destructionCallBack = new ScopedBeanDestructionCallBack(beanId, factory, callback);
        container.setDestructionCallBack(destructionCallBack);
      } else {
        if(destructionCallBack.getBeanFactory()==null) {
          destructionCallBack.setBeanFactory(factory);
        }
        if(destructionCallBack.getCallback()==null) {
          destructionCallBack.setCallback(callback);
        }
      }

      scope.registerDestructionCallback(beanName, destructionCallBack);
      return null;
    }
    return jp.proceed();
  }
  
  
  public Object suspendRequestAttributeGet(StaticJoinPoint jp, Scope s, String beanName) throws Throwable {
    return isInRehydration(s, beanName) ? null : jp.proceed();
  }

  public Object suspendRequestAttributeSet(StaticJoinPoint jp, Scope s, String beanName) throws Throwable {
    return isInRehydration(s, beanName) ? null : jp.proceed();
  }

  private boolean isInRehydration(Scope s, String beanName) {
    if (s instanceof DistributableBeanFactoryAware) {
      DistributableBeanFactory factory = ((DistributableBeanFactoryAware) s).getBeanFactory();
      ComplexBeanId beanId = (ComplexBeanId) scopedBeanId.get();
      BeanContainer container = factory.getBeanContainer(beanId);
      if(container!=null && !container.isInitialized()) {
        return true;
      }
    }
    return false;
  }

  
  public interface DistributableBeanFactoryAware {
    void setBeanFactory(DistributableBeanFactory beanFactory);
    DistributableBeanFactory getBeanFactory();
  }
  
  
  public static class DistributableBeanFactoryAwareMixin implements DistributableBeanFactoryAware {
    private DistributableBeanFactory beanFactory;
    
    public DistributableBeanFactory getBeanFactory() {
      return beanFactory;
    }
    
    public void setBeanFactory(DistributableBeanFactory beanFactory) {
      this.beanFactory = beanFactory;
    }
    
  }
  
}

