/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.exception.ImplementMe;
import com.tc.exception.TCNonPortableObjectError;
import com.tc.object.tx.ReadOnlyException;
import com.tc.object.tx.UnlockedSharedObjectException;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

public abstract class GenericLocalStateTestApp extends AbstractErrorCatchingTransparentApp {

  public GenericLocalStateTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
  }

  protected void runTest() throws Throwable {
    throw new ImplementMe();
  }

  protected void testMutate(Wrapper wrapper, LockMode lockMode, Mutator mutator) throws Throwable {
    int currentSize = wrapper.size();
    LockMode curr_lockMode = wrapper.getHandler().getLockMode();
    boolean gotExpectedException = false;
    Throwable throwable = null;

    if (await() == 0) {
      wrapper.getHandler().setLockMode(lockMode);
      try {
        mutator.doMutate(wrapper.getProxy());
      } catch (UnlockedSharedObjectException usoe) {
        gotExpectedException = lockMode == LockMode.NONE;
      } catch (ReadOnlyException roe) {
        gotExpectedException = lockMode == LockMode.READ;
      } catch (TCNonPortableObjectError ne) {
        gotExpectedException = lockMode == LockMode.WRITE;
      } catch (Throwable t) {
        throwable = t;
      }
    }

    await();
    wrapper.getHandler().setLockMode(curr_lockMode);

    if (gotExpectedException) {
      validate(wrapper, lockMode, mutator);
      
      int newSize = wrapper.size();
      switch (lockMode) {
        case NONE:
        case READ:
          Assert.assertEquals("Type: " + wrapper.getObject().getClass() + ", lock: " + lockMode,
                              currentSize, newSize);
          break;
        case WRITE:
          System.out.println("Map type: " + wrapper.getObject().getClass().getName());
          System.out.println("Current size: " + currentSize);
          System.out.println("New size: " + newSize);
          Assert.assertFalse("Type: " + wrapper.getObject().getClass() + ", socket shouldn't be added", ((Map) wrapper
              .getObject()).containsKey("socket"));
          break;
        default:
          throw new RuntimeException("Shouldn't happen");
      }
    }

    if (throwable != null) throw throwable;
  }

  protected abstract int await();
  protected void validate(Wrapper wrapper, LockMode lockMode, Mutator mutator) throws Throwable {
    return;
  }

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
            throw new RuntimeException("Should'n happen");
        }
      } catch (InvocationTargetException e) {
        throw e.getTargetException();
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
