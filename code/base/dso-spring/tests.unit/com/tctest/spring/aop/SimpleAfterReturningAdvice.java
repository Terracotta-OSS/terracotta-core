/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.aop;

import org.springframework.aop.AfterReturningAdvice;
import java.lang.reflect.Method;

public class SimpleAfterReturningAdvice implements AfterReturningAdvice {

  public void afterReturning(Object returnValue, Method method, Object[] args, Object target) throws Throwable {
    Logger.log += "after-returning(" + returnValue + ") args(" + args[0] + ") this(" + target.getClass().getName() + ") ";
  }

}
