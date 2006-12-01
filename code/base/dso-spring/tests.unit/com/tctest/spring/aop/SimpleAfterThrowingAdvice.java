/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.aop;

import org.springframework.aop.ThrowsAdvice;

import java.lang.reflect.Method;

public class SimpleAfterThrowingAdvice implements ThrowsAdvice {

  public void afterThrowing(Method method, Object[] args, Object target, Throwable cause) throws Throwable {
    Logger.log += "after-throwing(" + cause.toString() + ") args(" + args[0] + ") this(" + target.getClass().getName() + ") ";
  }

}
