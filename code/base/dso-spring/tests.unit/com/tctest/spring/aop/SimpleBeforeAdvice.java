/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.aop;

import java.lang.reflect.Method;

import org.springframework.aop.MethodBeforeAdvice;

public class SimpleBeforeAdvice implements MethodBeforeAdvice {

  public void before(Method method, Object[] args, Object target) throws Throwable {
    Logger.log += "before args(" + makeString(args) + ") this(" + target.getClass().getName() + ") ";
  }
  
  private String makeString(Object[] args) {
    if (null == args) {
      return "*NULL*";
    } else if(args.length == 0) {
      return "*EMPTY*";
    } else {
      return "" + args[0];
    }
  }
}
