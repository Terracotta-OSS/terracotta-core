/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.config.spec.CyclicBarrierSpec;
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

public abstract class GenericTestApp extends AbstractErrorCatchingTransparentApp {

  private static final String METHOD_PREFIX  = "test";
  private static final String METHOD_PATTERN = "^" + METHOD_PREFIX + ".*$";

  // roots
  private final CyclicBarrier barrier;
  private final CyclicBarrier barrier2;
  private final Exit          exit           = new Exit();
  protected final Map         sharedMap      = new HashMap();

  private final Class         type;
  private final List          tests;

  public GenericTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider, Class type) {
    super(appId, cfg, listenerProvider);

    final int count = getParticipantCount();
    if (count < 2) { throw new RuntimeException("wrong number of nodes: " + count); }

    this.barrier = new CyclicBarrier(getParticipantCount());
    this.barrier2 = new CyclicBarrier(getParticipantCount());

    this.type = type;
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

  public void runTest() throws Throwable {
    int num = barrier.barrier();
    boolean mutator = (num == 0);

    if (mutator) {
      doMutate();
    } else {
      doValidate();
    }
  }

  private void doValidate() throws Throwable {
    Thread.currentThread().setName("VALIDATOR " + getApplicationId());
    for (Iterator i = tests.iterator(); i.hasNext();) {
      String name = (String) i.next();
      barrier.barrier();

      if (exit.shouldExit()) { return; }

      try {
        runOp(name, true);
      } catch (Throwable t) {
        exit.toggle();
        throw t;
      } finally {
        barrier2.barrier();
      }

      if (exit.shouldExit()) { return; }
    }
  }

  private void doMutate() throws Throwable {
    Thread.currentThread().setName("MUTATOR " + getApplicationId());
    for (Iterator i = tests.iterator(); i.hasNext();) {
      String name = (String) i.next();
      System.err.print("Running test: " + name + " ... ");
      long start = System.currentTimeMillis();

      try {
        runOp(name, false);
        runOp(name, true);
      } catch (Throwable t) {
        exit.toggle();
        throw t;
      } finally {
        barrier.barrier();

        if (!exit.shouldExit()) {
          barrier2.barrier();
        }
      }

      System.err.println(" took " + (System.currentTimeMillis() - start) + " millis");

      if (exit.shouldExit()) { return; }
    }
  }

  private void runOp(String op, boolean validate) throws Throwable {
    Method m = findMethod(op);

    if (!validate) {
      makeTestObject(op);
    }

    Object object = getTestObject(op);

    if (object instanceof Iterator) {
      // do some automagic for Iterators
      for (Iterator i = (Iterator) object; i.hasNext();) {
        runMethod(m, i.next(), validate);
      }
    } else {
      runMethod(m, object, validate);
    }
  }

  private void runMethod(Method m, Object object, boolean validate) throws Throwable {
    try {
      m.invoke(this, new Object[] { object, Boolean.valueOf(validate) });
    } catch (InvocationTargetException ite) {
      throw ite.getTargetException();
    }
  }

  private Method findMethod(String name) throws NoSuchMethodException {
    Method method = getClass().getDeclaredMethod(METHOD_PREFIX + name, new Class[] { type, Boolean.TYPE });
    method.setAccessible(true);
    return method;
  }

  private List getTestNames() {
    List rv = new ArrayList();
    Class klass = getClass();
    Method[] methods = klass.getDeclaredMethods();
    for (int i = 0; i < methods.length; i++) {
      Method m = methods[i];
      if (m.getName().matches(METHOD_PATTERN)) {
        Class[] args = m.getParameterTypes();
        if ((args.length == 2) && args[0].equals(type) && args[1].equals(Boolean.TYPE)) {
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
    String testClass = GenericTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
    config.getOrCreateSpec(Exit.class.getName());

    spec.addRoot("sharedMap", "sharedMap");
    spec.addRoot("barrier", "barrier");
    spec.addRoot("barrier2", "barrier2");
    spec.addRoot("exit", "exit");

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);

    new CyclicBarrierSpec().visit(visitor, config);
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
