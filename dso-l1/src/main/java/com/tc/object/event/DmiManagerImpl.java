/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.event;

import com.tc.asm.Type;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ClientObjectManager;
import com.tc.object.ObjectID;
import com.tc.object.TCObject;
import com.tc.object.dmi.DmiClassSpec;
import com.tc.object.dmi.DmiDescriptor;
import com.tc.object.loaders.ClassProvider;
import com.tc.object.locks.LockLevel;
import com.tc.object.locks.StringLockID;
import com.tc.object.logging.RuntimeLogger;
import com.tc.util.Assert;
import com.tc.util.VicariousThreadLocal;
import com.tcclient.object.DistributedMethodCall;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

public class DmiManagerImpl implements DmiManager {
  private static final TCLogger     logger             = TCLogging.getLogger(DmiManager.class);
  private static final StringLockID lock               = new StringLockID("@DistributedMethodCall");
  private static final Object       TRUE               = new Object();
  private static final Object[]     EMPTY_OBJECT_ARRAY = new Object[] {};

  private final ClassProvider       classProvider;
  private final ClientObjectManager objMgr;
  private final RuntimeLogger       runtimeLogger;
  private final ThreadLocal         feedBack;
  private final ThreadLocal         nesting;

  public DmiManagerImpl(ClassProvider cp, ClientObjectManager om, RuntimeLogger rl) {
    Assert.pre(cp != null);
    Assert.pre(om != null);
    Assert.pre(rl != null);
    this.classProvider = cp;
    this.objMgr = om;
    this.runtimeLogger = rl;
    this.feedBack = new ThreadLocal();
    this.nesting = new VicariousThreadLocal();
  }

  public boolean distributedInvoke(Object receiver, String method, Object[] params, boolean runOnAllNodes) {
    if (feedBack.get() != null) { return false; }
    if (nesting.get() != null) { return false; }
    nesting.set(TRUE);

    Assert.pre(receiver != null);
    Assert.pre(method != null);
    Assert.pre(params != null);

    final String methodName = method.substring(0, method.indexOf('('));
    final String paramDesc = method.substring(method.indexOf('('));
    final DistributedMethodCall dmc = new DistributedMethodCall(receiver, params, methodName, paramDesc);
    if (runtimeLogger.getDistributedMethodDebug()) runtimeLogger.distributedMethodCall(receiver.getClass().getName(),
                                                                                       dmc.getMethodName(),
                                                                                       dmc.getParameterDesc());
    objMgr.getTransactionManager().begin(lock, LockLevel.CONCURRENT);
    try {
      final ObjectID receiverId = objMgr.lookupOrCreate(receiver).getObjectID();
      final ObjectID dmiCallId = objMgr.lookupOrCreate(dmc).getObjectID();
      final DmiClassSpec[] classSpecs = getClassSpecs(classProvider, receiver, params);
      final DmiDescriptor dd = new DmiDescriptor(receiverId, dmiCallId, classSpecs, runOnAllNodes);
      objMgr.getTransactionManager().addDmiDescriptor(dd);
      return true;
    } finally {
      objMgr.getTransactionManager().commit(lock, LockLevel.CONCURRENT);
    }
  }

  public void distributedInvokeCommit() {
    if (feedBack.get() != null) { return; }
    Assert.pre(nesting.get() != null);
    nesting.remove();
  }

  public void invoke(DistributedMethodCall dmc) {
    try {
      if (runtimeLogger.getDistributedMethodDebug()) runtimeLogger.distributedMethodCall(dmc.getReceiver().getClass()
          .getName(), dmc.getMethodName(), dmc.getParameterDesc());
      feedBack.set(TRUE);
      invoke0(dmc);
    } catch (Throwable e) {
      // FIXME: debug code
      e.printStackTrace();
      // FIXME: end debug code
      runtimeLogger.distributedMethodCallError(dmc.getReceiver().getClass().getName(), dmc.getMethodName(),
                                               dmc.getParameterDesc(), e);
      if (logger.isDebugEnabled()) logger.debug("Ignoring distributed method call", e);
    } finally {
      feedBack.remove();
    }
  }

  private void invoke0(DistributedMethodCall dmc) throws IllegalArgumentException, IllegalAccessException,
      InvocationTargetException {
    final ClassLoader origContextLoader = Thread.currentThread().getContextClassLoader();
    Method m = getMethod(dmc);
    m.setAccessible(true);

    ClassLoader tcl = dmc.getReceiver().getClass().getClassLoader();
    if (tcl == null) tcl = ClassLoader.getSystemClassLoader();
    Thread.currentThread().setContextClassLoader(tcl);

    try {
      m.invoke(dmc.getReceiver(), getParamaters(dmc));
    } finally {
      Thread.currentThread().setContextClassLoader(origContextLoader);
    }
  }

  private Object[] getParamaters(DistributedMethodCall dmc) {
    Object[] unresolvedParams = dmc.getParametersUnresolved();
    if (unresolvedParams.length == 0) { return EMPTY_OBJECT_ARRAY; }

    TCObject tco = objMgr.lookupExistingOrNull(unresolvedParams);
    if (tco == null) { throw new AssertionError(); }

    synchronized (tco.getResolveLock()) {
      tco.resolveAllReferences();
      return unresolvedParams.clone();
    }
  }

  private static Method getMethod(DistributedMethodCall dmc) {
    String methodName = dmc.getMethodName();
    String paramDesc = dmc.getParameterDesc();

    Class c = dmc.getReceiver().getClass();

    while (c != null) {
      Method[] methods = c.getDeclaredMethods();
      for (Method method : methods) {
        Method m = method;
        if (!m.getName().equals(methodName)) {
          continue;
        }
        Class[] argTypes = m.getParameterTypes();
        StringBuffer signature = new StringBuffer("(");
        for (Class argType : argTypes) {
          signature.append(Type.getDescriptor(argType));
        }
        signature.append(")");
        signature.append(Type.getDescriptor(m.getReturnType()));
        if (signature.toString().equals(paramDesc)) { return m; }
      }

      c = c.getSuperclass();
    }
    throw new RuntimeException("Method " + methodName + paramDesc + " does not exist on this object: "
                               + dmc.getReceiver());
  }

  private static void checkClassAvailability(ClassProvider classProvider, DmiClassSpec[] classSpecs)
      throws ClassNotFoundException {
    Assert.pre(classSpecs != null);
    for (DmiClassSpec s : classSpecs) {
      classProvider.getClassFor(s.getClassName());
    }
  }

  private static DmiClassSpec[] getClassSpecs(ClassProvider classProvider, Object receiver, Object[] params) {
    Assert.pre(classProvider != null);
    Assert.pre(receiver != null);
    Assert.pre(params != null);

    Set set = new HashSet();
    set.add(getClassSpec(classProvider, receiver));
    for (final Object p : params) {
      if (p != null) set.add(getClassSpec(classProvider, p));
    }
    DmiClassSpec[] rv = new DmiClassSpec[set.size()];
    set.toArray(rv);
    return rv;
  }

  private static Object getClassSpec(ClassProvider classProvider, Object obj) {
    Assert.pre(classProvider != null);
    Assert.pre(obj != null);
    final String className = obj.getClass().getName();
    return new DmiClassSpec(className);
  }

  public DistributedMethodCall extract(DmiDescriptor dd) {
    Assert.pre(dd != null);

    try {
      checkClassAvailability(classProvider, dd.getClassSpecs());
    } catch (ClassNotFoundException e) {
      if (logger.isDebugEnabled()) logger.debug("Ignoring distributed method call", e);
      return null;
    }

    try {
      DistributedMethodCall dmc = (DistributedMethodCall) objMgr.lookupObject(dd.getDmiCallId());
      // FIXME: debug code below ----------
      dmc.getClass();
      dmc.getReceiver().getClass().getName();
      dmc.getMethodName();
      dmc.getParameterDesc();
      // FIXME: debug code above ----------

      return dmc;
    } catch (Throwable e) {
      // FIXME: debug code
      e.printStackTrace();
      // FIXME: end debug code

      if (logger.isDebugEnabled()) logger.debug("Ignoring distributed method call", e);
      return null;
    }

    // unreachable
  }

}
