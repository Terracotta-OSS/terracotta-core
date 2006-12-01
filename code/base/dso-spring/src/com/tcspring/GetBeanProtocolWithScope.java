/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcspring;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.Scope;
import org.springframework.beans.factory.support.AbstractBeanFactory;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.tc.aspectwerkz.joinpoint.StaticJoinPoint;

import java.util.LinkedList;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;

public class GetBeanProtocolWithScope extends GetBeanProtocol {
  private static final String TC_SESSION_SCOPE_CONVERSATION_ID = "_TC_SESSION_SCOPE_CONVERSATION_ID";
  private transient final Log logger = LogFactory.getLog(this.getClass());
  
  // SessionScope.get() is synchronized on session mutex, so the flow below 
  // JP at SessionScope.get() is exclusive for each shared session,
  // However, synchroization needs to be considered if operation involves 
  // 1. shared state across sessions
  // 2. shared state also avaialbe for operations outside the cflow

  
  /**
   * Captures the scope id and creates complex bean id 
   */
  public Object scopeIdCflow(StaticJoinPoint jp, Scope scope, String name, AbstractBeanFactory beanFactory) throws Throwable {
    LinkedList stack = (LinkedList)cflowStack.get();
    try {
      stack.addFirst(new Object[]{new ComplexBeanId(scope.getConversationId(), name, true), beanFactory, scope} );
      return jp.proceed();
    } finally {
      stack.removeFirst();
    }
  }
  
  public Object interceptGetSessionId(StaticJoinPoint jp, HttpSession session) throws Throwable {
    Thread.dumpStack();
    
    Object conversationId = session.getAttribute(TC_SESSION_SCOPE_CONVERSATION_ID);
    if (conversationId == null) {
      conversationId = jp.proceed();
      session.setAttribute(TC_SESSION_SCOPE_CONVERSATION_ID, conversationId);
    }
    
    return conversationId;
  }
  
  /**
   * Registers destruction callback to remove the shared bean from the root
   */
  public Object beanNameCflow(StaticJoinPoint jp, String beanName, AutowireCapableBeanFactory beanFactory)
    throws Throwable {
    Object rtv = super.beanNameCflow(jp, beanName, beanFactory);

    // Put tc destruction callback at the end. For now it is OK  
    
    if (beanFactory instanceof DistributableBeanFactory) {
      LinkedList stack = (LinkedList)cflowStack.get();
      if (!stack.isEmpty()) {
        Object param = stack.getFirst();
        if (param instanceof Object[]) {
          Object[] params = (Object[]) param;
          if (params[0] instanceof ComplexBeanId && ((ComplexBeanId)params[0]).getBeanName().equals(beanName)) {
            Scope scope = (Scope) params[2];
            try {
              stack.addFirst(beanName);
              scope.registerDestructionCallback(beanName, 
                new ScopedBeanDestructionCallBack((ComplexBeanId)params[0], (DistributableBeanFactory)params[1]));
            } finally {
              stack.removeFirst();
            }
          }
        }
      }
    }
    
    return rtv;
  }  

  /**
   * Supress getting the bean from the shared HttpSession before virtualization
   * SessionScope.get() is synchronized on session mutex, so the flow below this JP is exclusive for each shared session, 
   * although concurrent calls of DistribuatableBeanFactory.getBeanFromSingletonCache() is still possible, we should be fine without sync. 
   */
  public Object interceptSessionGet(StaticJoinPoint jp, String name) throws Throwable {
    Object[] params = (Object[])((LinkedList)cflowStack.get()).getFirst();
    ComplexBeanId beanId = (ComplexBeanId)params[0];
    AbstractBeanFactory beanFactory = (AbstractBeanFactory)params[1];
    
    if (!(beanFactory instanceof DistributableBeanFactory) 
        || !name.endsWith(beanId.getBeanName())) { // not a shared context or not getting the bean at all; I wish I could be a little be more strict here.
      return jp.proceed();
    } else {
      DistributableBeanFactory distributableFactory = (DistributableBeanFactory)beanFactory;
      if (!distributableFactory.isDistributedBean(beanId.getBeanName())) { // not a shared bean
        return jp.proceed();
      } else {
        beanId.setEqualPeer(null);
        Object distributed = distributableFactory.getBeanFromSingletonCache(beanId);
        if (distributed != null) {                                        // existing shared bean
          if (beanId.getEqualPeer().isVirtualized()) {
            return jp.proceed();
          } else {
            beanId.getEqualPeer().setVirtualized(true);
            return null; // will force to go through creation and virtualization
          }
        } else {  
          return jp.proceed();              
        }
      }
    }
  }
  
  /**
   * Chains up the destruction callback for scoped bean. 
   */
  public Object interceptRegisterCallback(StaticJoinPoint jp, HttpSession session, String name, Object listener) 
    throws Throwable {
    LinkedList stack = (LinkedList)cflowStack.get(); 
    if (!stack.isEmpty() 
        && name.endsWith(stack.getFirst().toString()) && name.startsWith(ServletRequestAttributes.DESTRUCTION_CALLBACK_NAME_PREFIX)) {     
      // sync this block ???  -- it is not atomic, potential racing with other code 
      Object existing = session.getAttribute(name);
      if (existing == null) { // wrap it and set it
        session.setAttribute(name, 
                             new ChainedBindingListener((HttpSessionBindingListener)listener));
      } else if (existing instanceof ChainedBindingListener) {
        ((ChainedBindingListener)existing).addNext(
           new ChainedBindingListener((HttpSessionBindingListener)listener));
      } else {
        // this is not going to work, there is an unknown callback get ahead of us
        logger.warn("Unexpected destruction callback listener found for bean " + stack.getFirst());
      }
      // end of sync
    } else {
      jp.proceed(); // don't think this ever happens, but just in case.
    }
    
    return null;
  }
  
  static class ScopedBeanDestructionCallBack implements Runnable {
    private transient ComplexBeanId beanId = null;
    private transient DistributableBeanFactory beanFactory = null;
    
    public ScopedBeanDestructionCallBack(ComplexBeanId id, DistributableBeanFactory factory) {
      this.beanId = id;
      this.beanFactory = factory;
    }
    
    public void run() {
      beanFactory.removeBeanFromSingletonCache(beanId);
    }
  }
  
  static class ChainedBindingListener implements HttpSessionBindingListener {
    private ChainedBindingListener next = null;
    private transient HttpSessionBindingListener destructionListener; // might be nulled out when sharing happens
    
    public void addNext(ChainedBindingListener newNext) {
      if (next == null) { next = newNext;} 
      else { next.addNext(newNext); }
    }

    public ChainedBindingListener(HttpSessionBindingListener listener) {
      this.destructionListener = listener;
    }

    public void valueBound(HttpSessionBindingEvent event) {
      if (destructionListener != null) { destructionListener.valueBound(event); }
      if (next != null) { next.valueBound(event); }
    }

    public void valueUnbound(HttpSessionBindingEvent event) {
      if (destructionListener != null) { destructionListener.valueUnbound(event); }
      if (next != null) { next.valueUnbound(event); }
    }
    
    // for debug
    public final String toString() {
      return super.toString() + "[destructionListener=" + destructionListener + ";next=" + next + "]";
    }
  }
}
