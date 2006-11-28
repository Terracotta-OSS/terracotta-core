/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tctest.spring.bean;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

public class TestInterceptor implements MethodInterceptor {

  public Object invoke(MethodInvocation mi) throws Throwable {
    return "interceptorInvoked-" +  mi.proceed();
  }

}
