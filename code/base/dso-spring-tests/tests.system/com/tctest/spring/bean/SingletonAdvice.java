/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.bean;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

public class SingletonAdvice implements MethodInterceptor, ISingletonAdvice {
  private int counter = 0;
  
  public Object invoke(MethodInvocation invocation) throws Throwable {
    synchronized(this) {
      this.counter++;      
    }
    return invocation.proceed();
  }
  
  synchronized public int getCounter() {
    return counter;
  }

}
