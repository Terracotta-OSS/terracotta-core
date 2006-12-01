/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.aop;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

public class SimpleMethodInterceptor implements MethodInterceptor {

  public Object invoke(MethodInvocation methodInvocation) throws Throwable {
    Logger.log += "before-around args(" + methodInvocation.getArguments()[0] + ") this(" + methodInvocation.getThis().getClass().getName() + ") ";
    Object result = methodInvocation.proceed();
    Logger.log += "after-around ";
    return result;
  }

}
