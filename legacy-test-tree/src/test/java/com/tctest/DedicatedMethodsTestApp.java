/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tctest;

import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractTransparentApp;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class DedicatedMethodsTestApp extends AbstractTransparentApp {

  private static final String METHOD_PREFIX  = "test";
  private static final String METHOD_PATTERN = "^" + METHOD_PREFIX + ".*$";

  public DedicatedMethodsTestApp(final String appId, final ApplicationConfig config,
                                 final ListenerProvider listenerProvider) {
    super(appId, config, listenerProvider);
  }

  protected abstract BuiltinBarrier getBarrierForNodeCoordination();

  public void run() {
    try {
      final List<String> tests = getTestNames();

      for (String test : tests) {
        if (null == getBarrierForNodeCoordination() || 0 == getBarrierForNodeCoordination().await()) {
          System.out.println("Running " + test + " ...");
        }

        runMethod(findMethod(test));
      }
    } catch (Throwable t) {
      notifyError(t);
    }
  }

  private List<String> getTestNames() {
    final List<String> rv = new ArrayList();
    final Class klass = getClass();
    final Method[] methods = klass.getDeclaredMethods();
    for (final Method m : methods) {
      if (m.getName().matches(METHOD_PATTERN)) {
        Class[] args = m.getParameterTypes();

        if (0 == args.length) {
          rv.add(m.getName().replaceFirst(METHOD_PREFIX, ""));
        } else {
          throw new RuntimeException("bad method: " + m);
        }
      }
    }

    if (rv.size() <= 0) { throw new RuntimeException("Didn't find any operations"); }

    // make test order predictable (although this is a bad thing to rely on)
    Collections.sort(rv);

    return rv;
  }

  private void runMethod(final Method m) throws Throwable {
    try {
      m.invoke(this, new Object[0]);
    } catch (InvocationTargetException ite) {
      throw ite.getTargetException();
    }
  }

  private Method findMethod(final String name) throws NoSuchMethodException {
    Method method = getClass().getDeclaredMethod(METHOD_PREFIX + name, new Class[0]);
    method.setAccessible(true);
    return method;
  }
}
