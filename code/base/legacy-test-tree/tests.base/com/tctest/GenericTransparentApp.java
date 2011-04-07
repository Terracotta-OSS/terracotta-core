/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;

public abstract class GenericTransparentApp extends AbstractErrorCatchingTransparentApp {

  private static final String METHOD_PREFIX  = "test";
  private static final String METHOD_PATTERN = "^" + METHOD_PREFIX + ".*$";

  // roots
  private final CyclicBarrier barrier;
  private final CyclicBarrier barrier2;
  private final Exit          exit           = new Exit();
  protected final Map         sharedMap      = new HashMap();

  private final Class         type;
  private final List          tests;
  private final int           variants;

  private transient boolean   mutator;

  public GenericTransparentApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider, Class type) {
    this(appId, cfg, listenerProvider, type, 1);
  }

  public GenericTransparentApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider, Class type,
                               int variants) {
    super(appId, cfg, listenerProvider);

    final int count = getParticipantCount();
    if (count < 2) { throw new RuntimeException("wrong number of nodes: " + count); }

    this.barrier = new CyclicBarrier(getParticipantCount());
    this.barrier2 = new CyclicBarrier(getParticipantCount());

    this.type = type;
    this.variants = variants;
    this.tests = getTestNames();
  }

  protected abstract Object getTestObject(String testName);

  protected abstract void setupTestObject(String testName);

  private void makeTestObject(String test) {
    synchronized (sharedMap) {
      sharedMap.clear(); // don't want any cross talk
      setupTestObject(test);
    }
  }

  @Override
  public void runTest() throws Throwable {
    int num = barrier.await();
    mutator = (num == 0);

    if (mutator) {
      doMutate();
    } else {
      doValidate();
    }
  }

  protected boolean isMutator() {
    return mutator;
  }

  private void doValidate() throws Throwable {
    Thread.currentThread().setName("VALIDATOR " + getApplicationId());
    for (Iterator i = tests.iterator(); i.hasNext();) {
      String name = (String) i.next();

      for (int variant = 1; variant <= variants; variant++) {
        barrier.await();

        if (exit.shouldExit()) { return; }

        try {
          runOp(name, true, variant);
        } catch (Throwable t) {
          t.printStackTrace();

          exit.toggle();
          throw t;
        } finally {
          barrier2.await();
        }

        if (exit.shouldExit()) { return; }
      }
    }
  }

  private void doMutate() throws Throwable {
    Thread.currentThread().setName("MUTATOR " + getApplicationId());
    for (Iterator i = tests.iterator(); i.hasNext();) {
      String name = (String) i.next();

      System.err.print("Running test: " + name + " ... ");
      long start = System.currentTimeMillis();

      for (int variant = 1; variant <= variants; variant++) {
        try {
          runOp(name, false, variant);
          runOp(name, true, variant);
        } catch (Throwable t) {
          t.printStackTrace();

          exit.toggle();
          throw t;
        } finally {
          barrier.await();

          if (!exit.shouldExit()) {
            barrier2.await();
          }
        }

        if (exit.shouldExit()) { return; }
      }

      System.err.println(" took " + (System.currentTimeMillis() - start) + " millis");
    }
  }

  private void runOp(String op, boolean validate, int variant) throws Throwable {
    Method m = findMethod(op);

    if (!validate) {
      makeTestObject(op);
    }

    Object object = getTestObject(op);

    if (object instanceof Iterator) {
      // do some automagic for Iterators
      for (Iterator i = (Iterator) object; i.hasNext();) {
        runMethod(m, i.next(), validate, variant);
      }
    } else {
      runMethod(m, object, validate, variant);
    }
  }

  private void runMethod(Method m, Object object, boolean validate, int variant) throws Throwable {
    final Object[] args;
    if (variants > 1) {
      args = new Object[] { object, Boolean.valueOf(validate), new Integer(variant) };
    } else {
      args = new Object[] { object, Boolean.valueOf(validate) };
    }

    try {
      m.invoke(this, args);
    } catch (InvocationTargetException ite) {
      throw ite.getTargetException();
    }
  }

  private Method findMethod(String name) throws NoSuchMethodException {
    final Class[] sig;
    if (variants > 1) {
      sig = new Class[] { type, Boolean.TYPE, Integer.TYPE };
    } else {
      sig = new Class[] { type, Boolean.TYPE };
    }

    Method method = getClass().getDeclaredMethod(METHOD_PREFIX + name, sig);
    method.setAccessible(true);
    return method;
  }

  private List getTestNames() {
    List rv = new ArrayList();
    Class klass = getClass();
    Method[] methods = klass.getDeclaredMethods();
    for (Method m : methods) {
      if (m.getName().matches(METHOD_PATTERN)) {
        Class[] args = m.getParameterTypes();

        final boolean ok;
        if (variants > 1) {
          ok = (args.length == 3) && args[0].equals(type) && args[1].equals(Boolean.TYPE)
               && args[2].equals(Integer.TYPE);
        } else {
          ok = (args.length == 2) && args[0].equals(type) && args[1].equals(Boolean.TYPE);
        }

        if (ok) {
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

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = GenericTransparentApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
    config.getOrCreateSpec(Exit.class.getName());

    spec.addRoot("sharedMap", "sharedMap");
    spec.addRoot("barrier", "barrier");
    spec.addRoot("barrier2", "barrier2");
    spec.addRoot("exit", "exit");

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);
  }

  private static class Exit {
    private boolean exit = false;

    synchronized boolean shouldExit() {
      return exit;
    }

    synchronized void toggle() {
      exit = true;
    }
  }

}
