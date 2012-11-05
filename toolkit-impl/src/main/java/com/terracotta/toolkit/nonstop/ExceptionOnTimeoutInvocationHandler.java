/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.nonstop;

import net.sf.ehcache.constructs.nonstop.NonStopCacheException;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

class ExceptionOnTimeoutInvocationHandler implements InvocationHandler {

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    throw new NonStopCacheException(method.getName() + " timed out");
  }

}