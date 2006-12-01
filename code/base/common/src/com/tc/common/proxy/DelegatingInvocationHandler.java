/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.common.proxy;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.util.Assert;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author andrew A {@link GenericInvocationHandler}that delegates all unknown methods to a special 'delegate' object.
 *         This is very useful for creating delegates that override only certain methods.
 */
class DelegatingInvocationHandler extends GenericInvocationHandler {
  private static final TCLogger logger = TCLogging.getLogger(GenericInvocationHandler.class);
  private final Object          delegate;

  public DelegatingInvocationHandler(Object delegate, Object handler) {
    super(handler);
    Assert.eval(delegate != null);
    this.delegate = delegate;
  }

  protected Object handlerMethodNotFound(Object proxy, Method method, Object[] args) throws Throwable {
    try {
      Method theMethod = delegate.getClass().getMethod(method.getName(), method.getParameterTypes());

      try {
        theMethod.setAccessible(true);
      } catch (SecurityException se) {
        logger.warn("Cannot setAccessible(true) for method [" + theMethod + "], " + se.getMessage());
      }

      return theMethod.invoke(delegate, args);
    } catch (InvocationTargetException ite) {
      // We need to unwrap this; we want to throw what the method actually
      // threw, not an InvocationTargetException (which causes all sorts of
      // madness, since it's unlikely the interface method throws that
      // exception, and you'll end up with an UndeclaredThrowableException.
      // Blech.)
      throw ite.getCause();
    } catch (NoSuchMethodException nsme) {
      return super.handlerMethodNotFound(proxy, method, args);
    }
  }
}