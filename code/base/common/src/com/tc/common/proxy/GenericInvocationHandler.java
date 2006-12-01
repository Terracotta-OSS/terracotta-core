/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.common.proxy;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.util.Assert;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * This class provides a generic {@link InvocationHandler}, used with {@link java.lang.reflect.Proxy}, that allows
 * some sanity in generating proxies. Basically, you create a {@link GenericInvocationHandler}as the target of a
 * {@link Proxy}. When called, the invocation handler will look in an object that you give it at construction time for
 * a method of the correct signature, and invoke that.
 * </p>
 * <p>
 * If no such method exists, the method {@link #handlerMethodNotFound}will be called instead. By default, this simply
 * throws a {@link RuntimeException}, but can be overridden in subclasses to do anything you want.
 */
class GenericInvocationHandler implements InvocationHandler {
  private static final TCLogger logger = TCLogging.getLogger(GenericInvocationHandler.class);

  private Object                handler;

  /**
   * Only should be called from subclasses that know what they're doing.
   */
  protected GenericInvocationHandler() {
    this.handler = null;
  }

  public GenericInvocationHandler(Object handler) {
    Assert.eval(handler != null);
    this.handler = handler;
  }

  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    try {
      return invokeInternal(proxy, method, args);
    } catch (Throwable t) {
      // try to log, but make sure the orignal exception is always re-thrown, even if
      // logging somehow manages to throw an exception
      try {
        if (logger.isDebugEnabled()) {
          logger.debug("EXCEPTION thrown trying to call " + method, t);
        }
      } catch (Throwable loggingError) {
        // too bad, logging blows
      }

      throw t;
    }
  }

  protected Object getHandler() {
    return this.handler;
  }

  protected void setHandler(Object o) {
    this.handler = o;
  }

  private Object invokeInternal(Object proxy, Method method, Object[] args) throws Throwable {
    try {
      final Method theMethod = handler.getClass().getMethod(method.getName(), method.getParameterTypes());
      try {
        theMethod.setAccessible(true);
      } catch (SecurityException se) {
        logger.warn("Cannot setAccessible(true) for method [" + theMethod + "], " + se.getMessage());
      }
      return theMethod.invoke(handler, args);
    } catch (InvocationTargetException ite) {
      // We need to unwrap this; we want to throw what the method actually
      // threw, not an InvocationTargetException (which causes all sorts of
      // madness, since it's unlikely the interface method throws that
      // exception, and you'll end up with an UndeclaredThrowableException.
      // Blech.)
      throw ite.getCause();
    } catch (NoSuchMethodException nsme) {
      return handlerMethodNotFound(proxy, method, args);
    }
  }

  private String describeClasses(Class[] classes) {
    StringBuffer out = new StringBuffer();
    for (int i = 0; i < classes.length; ++i) {
      if (i > 0) out.append(", ");
      out.append(classes[i].getName());
    }
    return out.toString();
  }

  protected Object handlerMethodNotFound(Object proxy, Method method, Object[] args) throws Throwable {
    throw new NoSuchMethodError("Handler " + handler + " does not have a method named " + method.getName()
                                + " that is " + "has argument types " + describeClasses(method.getParameterTypes()));
  }

}