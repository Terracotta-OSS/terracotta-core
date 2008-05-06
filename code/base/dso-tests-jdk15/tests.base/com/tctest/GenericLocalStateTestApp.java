/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.exception.ImplementMe;
import com.tc.exception.TCNonPortableObjectError;
import com.tc.object.tx.UnlockedSharedObjectException;
import com.tc.object.util.ReadOnlyException;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public abstract class GenericLocalStateTestApp extends AbstractErrorCatchingTransparentApp {

  public GenericLocalStateTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
  }

  protected void runTest() throws Throwable {
    throw new ImplementMe();
  }

  protected void testMutate(Object testTarget, LockMode lockMode, Class mutatorClass) throws Exception {
    System.out.println("-- Applying " + mutatorClass.getSimpleName() + " on " + testTarget.getClass().getSimpleName()
                       + " with lock " + lockMode);
    int before = testTarget.hashCode();
    System.out.println("   Before mutate: " + testTarget);
    Wrapper wrapper = new MyWrapper(mutatorClass, Mutator.class);
    wrapper.getHandler().setLockMode(lockMode);
    try {
      ((Mutator) wrapper.getProxy()).doMutate(testTarget);
    } catch (UnlockedSharedObjectException usoe) {
      if (lockMode != LockMode.NONE) throw usoe;
    } catch (ReadOnlyException roe) {
      if (lockMode != LockMode.READ) throw roe;
    } catch (TCNonPortableObjectError ne) {
      if (lockMode != LockMode.WRITE) throw ne;
    }
    int after = testTarget.hashCode();
    System.out.println("   After mutate: " + testTarget);
    validate(before, after, testTarget, lockMode, mutatorClass);
  }

  protected abstract void validate(int before, int after, Object testTarget, LockMode lockMode, Class mutatorClass)
      throws Exception;

  static enum LockMode {
    NONE, READ, WRITE
  }

  static class Handler implements InvocationHandler {
    private final Object o;
    private LockMode     lockMode = LockMode.NONE;

    public Handler(Object o) {
      this.o = o;
    }

    public LockMode getLockMode() {
      return lockMode;
    }

    public void setLockMode(LockMode mode) {
      synchronized (this) {
        lockMode = mode;
      }
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      try {
        switch (lockMode) {
          case NONE:
            return method.invoke(o, args);
          case READ:
            return invokeWithReadLock(method, args);
          case WRITE:
            return invokeWithWriteLock(method, args);
          default:
            throw new RuntimeException("Shouldn't happen");
        }
      } catch (InvocationTargetException e) {
        throw e.getTargetException();
      }
    }

    private Object invokeWithReadLock(Method method, Object[] args) throws Throwable {
      // System.out.println("invokeWithReadLock: " + method);
      synchronized (args[0]) {
        return method.invoke(o, args);
      }
    }

    private Object invokeWithWriteLock(Method method, Object[] args) throws Throwable {
      // System.out.println("invokeWithWriteLock: " + method);
      synchronized (args[0]) {
        return method.invoke(o, args);
      }
    }
  }

  static class NonPortable implements Comparable {
    public int compareTo(Object o) {
      return 1;
    }

    public String toString() {
      return "NonPortable object";
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

  static class MyWrapper implements Wrapper {
    private Object  object;
    private Object  proxy;
    private Handler handler;

    public MyWrapper(Class objectClass, Class interfaceClass) throws Exception {
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
      try {
        Method method = object.getClass().getMethod("size");
        return (Integer) method.invoke(object);
      } catch (Exception e) {
        e.printStackTrace();
        throw new RuntimeException(e);
      }
    }
  }
}
