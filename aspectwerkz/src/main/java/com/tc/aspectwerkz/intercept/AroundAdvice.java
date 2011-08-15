/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.intercept;

import com.tc.aspectwerkz.joinpoint.JoinPoint;

/**
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonér </a>
 */
public interface AroundAdvice extends Advice {

  /**
   * @param jp
   * @throws Throwable
   */
  Object invoke(JoinPoint jp) throws Throwable;
}
