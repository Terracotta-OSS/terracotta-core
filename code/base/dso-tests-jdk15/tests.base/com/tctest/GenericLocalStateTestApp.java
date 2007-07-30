/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.exception.ImplementMe;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class GenericLocalStateTestApp extends AbstractErrorCatchingTransparentApp {

  public GenericLocalStateTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
  }

  protected void runTest() throws Throwable {
    throw new ImplementMe();
  }

  protected void testMutate(Wrapper m, LOCK_MODE lockMode, Mutator mutator) throws Throwable {
    int currentSize = m.size();
    LOCK_MODE curr_lockMode = m.getHandler().getLockMode();
    boolean gotExpectedException = false;

    if (await() == 0) {
      m.getHandler().setLockMode(lockMode);
      try {
        mutator.doMutate(m.getProxy());
      } catch (Exception e) {
        e.printStackTrace();
        gotExpectedException = true;        
      }
    }

    await();
    m.getHandler().setLockMode(curr_lockMode);

    if (gotExpectedException) {
      int newSize = m.size();
      Assert.assertEquals("Collection type: " + m.getObject().getClass() + ", lock: " + lockMode, currentSize, newSize);
    }
  }

  protected abstract int await();

  static enum LOCK_MODE {
    NONE, READ, WRITE
  }

  static class Handler implements InvocationHandler {
    private final Object o;
    private LOCK_MODE    lockMode = LOCK_MODE.NONE;

    public Handler(Object o) {
      this.o = o;
    }

    public LOCK_MODE getLockMode() {
      return lockMode;
    }

    public void setLockMode(LOCK_MODE mode) {
      synchronized (this) {
        lockMode = mode;
      }
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      switch (lockMode) {
        case NONE:
          return method.invoke(o, args);
        case READ:
          return invokeWithReadLock(method, args);
        case WRITE:
          return invokeWithWriteLock(method, args);
        default:
          throw new RuntimeException("Should not happen");
      }
    }

    private Object invokeWithReadLock(Method method, Object[] args) throws Throwable {
      synchronized (o) {
        return method.invoke(o, args);
      }
    }

    private Object invokeWithWriteLock(Method method, Object[] args) throws Throwable {
      synchronized (o) {
        return method.invoke(o, args);
      }
    }
  }

  static interface Wrapper {
    public Object getObject();

    public Object getProxy();

    public Handler getHandler();

    public int size();
  }

  static interface Mutator {
    public void doMutate(Object o);
  }

  static class CollectionWrapper implements Wrapper {
    private Object  object;
    private Object  proxy;
    private Handler handler;

    public CollectionWrapper(Class objectClass, Class interfaceClass) throws Exception {
      object = objectClass.newInstance();
      handler = new Handler(object);
      proxy = Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] { interfaceClass }, handler);
    }

    public Handler getHandler() {
      return handler;
    }

    public Object getObject() {
      return object;
    }

    public Object getProxy() {
      return proxy;
    }

    public int size() {
      if (object instanceof Map) {
        return ((Map) object).size();
      } else if (object instanceof List) {
        return ((List) object).size();
      } else if (object instanceof Set) { return ((Set) object).size(); }

      throw new RuntimeException("not expected: " + object.getClass().getName());
    }
  }
}
