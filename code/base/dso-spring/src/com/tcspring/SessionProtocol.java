/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcspring;

import com.tc.aspectwerkz.joinpoint.StaticJoinPoint;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;

/**
 * Clustering aspects related to http session.  
 * 
 * @author Eugene Kuleshov
 */
public class SessionProtocol {

  private static final String TC_SESSION_SCOPE_CONVERSATION_ID = "_TC_SESSION_SCOPE_CONVERSATION_ID";

  
  /**
   * Uses clustered http session to store session id, to protect from the case when session id is
   * different between nodes.
   * 
   * cflow(execution(* org.springframework.web.context.request.SessionScope.getConversationId()))
   *   AND withincode(* org.springframework.web.context.request.ServletRequestAttributes.getSessionId())
   *   AND call(* javax.servlet.http.HttpSession+.getId())
   *   AND target(session)
   * 
   * @see org.springframework.web.context.request.SessionScope#getConversationId()
   * @see org.springframework.web.context.request.ServletRequestAttributes#getSessionId()
   */
  public Object clusterSessionId(StaticJoinPoint jp, HttpSession session) throws Throwable {
    Object conversationId = session.getAttribute(TC_SESSION_SCOPE_CONVERSATION_ID);
    if (conversationId == null) {
      conversationId = jp.proceed();
      session.setAttribute(TC_SESSION_SCOPE_CONVERSATION_ID, conversationId);
    }
    
    return conversationId;
  }


  private ThreadLocal cflowCallback = new ThreadLocal();
  
  /**
   * @see org.springframework.web.context.request.ServletRequestAttributes.registerSessionDestructionCallback(String name, Runnable callback)
   */
  public Object captureDestructionCallback(StaticJoinPoint jp, String name, Runnable callback) throws Throwable {
    if(callback instanceof ScopedBeanDestructionCallBack) {
      try {
        cflowCallback.set(callback);
        return jp.proceed();
      } finally {
        cflowCallback.set(null);        
      }      
    }
    return jp.proceed();
  }

  /**
   * @see org.springframework.web.context.request.ServletRequestAttributes.registerSessionDestructionCallback(String name, Runnable callback)
   * @see javax.servlet.http.HttpSession#setAttribute(String name, Object value)
   */
  public Object virtualizeSessionDestructionListener(StaticJoinPoint jp, String name, HttpSession session) throws Throwable {
    Object oldAttribute = session.getAttribute(name);
    ScopedBeanDestructionCallBack callback = (ScopedBeanDestructionCallBack) cflowCallback.get();
    if(callback!=null) {
      if(oldAttribute==null) {
        session.setAttribute(name, new DestructionCallbackBindingListener(callback));
        return null;
        
      } else if(oldAttribute instanceof DestructionCallbackBindingListener) {
        ((DestructionCallbackBindingListener) oldAttribute).setScopedBeanDestructionCallBack(callback);
        return null;
        
      }
    }
    
    return jp.proceed();
  }

  
  /**
   * Adapter that implements the Servlet 2.3 HttpSessionBindingListener
   * interface, wrapping a request destruction callback.
   */
  private static class DestructionCallbackBindingListener implements HttpSessionBindingListener {

    private transient ScopedBeanDestructionCallBack destructionCallback;

    public DestructionCallbackBindingListener(ScopedBeanDestructionCallBack destructionCallback) {
      this.destructionCallback = destructionCallback;
    }

    public void setScopedBeanDestructionCallBack(ScopedBeanDestructionCallBack destructionCallback) {
      this.destructionCallback = destructionCallback;
    }

    public ScopedBeanDestructionCallBack getScopedBeanDestructionCallBack() {
      return destructionCallback;
    }

    public void valueBound(HttpSessionBindingEvent event) {
    }

    public void valueUnbound(HttpSessionBindingEvent event) {
      this.destructionCallback.run();
    }
  }
  
}

