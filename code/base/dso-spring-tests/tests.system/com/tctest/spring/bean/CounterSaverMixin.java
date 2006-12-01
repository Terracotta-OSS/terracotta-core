/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.bean;

import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.support.DelegatingIntroductionInterceptor;


public class CounterSaverMixin extends DelegatingIntroductionInterceptor implements CounterSaver {

  private int savedCounter;

  synchronized public void saveCounter() {
    // counter is being saved by invoke() method 
  }
  
  public int getSavedCounter() {
    return savedCounter;
  }
  
  public Object invoke(MethodInvocation invocation) throws Throwable {
      String name = invocation.getMethod().getName();
      if ("saveCounter".equals(name)) {
        synchronized(this) {
          savedCounter = ((ISingleton) invocation.getThis()).getCounter();
        }
        return null;
      } else {
        return super.invoke(invocation);
      }
  }

}

