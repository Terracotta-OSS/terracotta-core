/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcspring;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.tc.aspectwerkz.joinpoint.StaticJoinPoint;

/**
 * Used to replace org.springframework.webflow.conversation.impl.UtilConcurrentConversationLock
 * 
 * @author Eugene Kuleshov
 */
public class ConversationLockProtocol {
  private final transient Log logger = LogFactory.getLog(getClass());

  // a hack to work around class cast exception for call advice
  public Object replaceUtilConversationLock(StaticJoinPoint jp) throws Throwable {
    Object o = jp.proceed();
    if(o.getClass().getName().equals("org.springframework.webflow.conversation.impl.UtilConcurrentConversationLock")) {
      logger.info("Creating distributed ConversationLock");
      return new DSOConversationLock();
    }
    return o;

  }

}
