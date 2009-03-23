/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.Type;
import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.aspectwerkz.reflect.FieldInfo;
import com.tc.aspectwerkz.reflect.impl.java.JavaClassInfo;
import com.tc.client.AbstractClientFactory;
import com.tc.cluster.DsoCluster;
import com.tc.cluster.DsoClusterImpl;
import com.tc.config.lock.LockContextInfo;
import com.tc.lang.StartupHelper;
import com.tc.lang.TCThreadGroup;
import com.tc.lang.ThrowableHandler;
import com.tc.lang.StartupHelper.StartupAction;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.management.beans.sessions.SessionMonitor;
import com.tc.object.ClientObjectManager;
import com.tc.object.ClientShutdownManager;
import com.tc.object.DistributedObjectClient;
import com.tc.object.LiteralValues;
import com.tc.object.ObjectID;
import com.tc.object.Portability;
import com.tc.object.SerializationUtil;
import com.tc.object.TCObject;
import com.tc.object.bytecode.hook.impl.PreparedComponentsFromL2Connection;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.event.DmiManager;
import com.tc.object.loaders.ClassProvider;
import com.tc.object.loaders.NamedClassLoader;
import com.tc.object.loaders.StandardClassProvider;
import com.tc.object.lockmanager.api.LockLevel;
import com.tc.object.logging.InstrumentationLogger;
import com.tc.object.logging.InstrumentationLoggerImpl;
import com.tc.object.logging.RuntimeLogger;
import com.tc.object.logging.RuntimeLoggerImpl;
import com.tc.object.tx.ClientTransactionManager;
import com.tc.object.tx.TimerSpec;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Assert;
import com.tc.util.Util;
import com.tc.util.concurrent.SetOnceFlag;
import com.tc.util.runtime.Vm;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ManagerImpl implements Manager {
  private static final TCLogger                    logger                       = TCLogging.getLogger(Manager.class);
  private static final LiteralValues               literals                     = new LiteralValues();
  private final SetOnceFlag                        clientStarted                = new SetOnceFlag();
  private final DSOClientConfigHelper              config;
  private final ClassProvider                      classProvider;
  private final boolean                            startClient;
  private final PreparedComponentsFromL2Connection connectionComponents;
  private final Thread                             shutdownAction;
  private final Portability                        portability;
  private final DsoClusterImpl                     dsoCluster;
  private final RuntimeLogger                      runtimeLogger;

  private final InstrumentationLogger              instrumentationLogger;

  private ClientObjectManager                      objectManager;
  private ClientShutdownManager                    shutdownManager;
  private ClientTransactionManager                 txManager;
  private DistributedObjectClient                  dso;
  private DmiManager                               methodCallManager;
  private final SerializationUtil                  serializer                   = new SerializationUtil();
  private final MethodDisplayNames                 methodDisplay                = new MethodDisplayNames(serializer);

  public ManagerImpl(final DSOClientConfigHelper config, final PreparedComponentsFromL2Connection connectionComponents) {
    this(true, null, null, config, connectionComponents, true);
  }

  // For tests
  public ManagerImpl(final boolean startClient, final ClientObjectManager objectManager, final ClientTransactionManager txManager,
                     final DSOClientConfigHelper config, final PreparedComponentsFromL2Connection connectionComponents) {
    this(startClient, objectManager, txManager, config, connectionComponents, true);
  }

  // For tests
  public ManagerImpl(final boolean startClient, final ClientObjectManager objectManager, final ClientTransactionManager txManager,
                     final DSOClientConfigHelper config, final PreparedComponentsFromL2Connection connectionComponents,
                     final boolean shutdownActionRequired) {
    this.objectManager = objectManager;
    this.portability = config.getPortability();
    this.txManager = txManager;
    this.config = config;
    this.instrumentationLogger = new InstrumentationLoggerImpl(config.instrumentationLoggingOptions());
    this.startClient = startClient;
    this.connectionComponents = connectionComponents;
    this.dsoCluster = new DsoClusterImpl();
    if (shutdownActionRequired) {
      shutdownAction = new Thread(new ShutdownAction());
      // Register a shutdown hook for the DSO client
      Runtime.getRuntime().addShutdownHook(shutdownAction);
    } else {
      shutdownAction = null;
    }
    this.runtimeLogger = new RuntimeLoggerImpl(config);
    this.classProvider = new StandardClassProvider(runtimeLogger);
    registerStandardLoaders();
  }

  private void registerStandardLoaders() {
    ClassLoader loader1 = ClassLoader.getSystemClassLoader();
    ClassLoader loader2 = loader1.getParent();
    ClassLoader loader3 = loader2.getParent();

    final ClassLoader sunSystemLoader;
    final ClassLoader extSystemLoader;

    if (loader3 != null) { // user is using alternate system loader
      sunSystemLoader = loader2;
      extSystemLoader = loader3;
    } else {
      sunSystemLoader = loader1;
      extSystemLoader = loader2;
    }

    registerNamedLoader((NamedClassLoader) sunSystemLoader, null);
    registerNamedLoader((NamedClassLoader) extSystemLoader, null);
  }

  public SessionMonitor getHttpSessionMonitor() {
    return dso.getHttpSessionMonitor();
  }

  public void init() {
    init(false);
  }

  public void initForTests() {
    init(true);
  }

  private void init(final boolean forTests) {
    resolveClasses(); // call this before starting any threads (SEDA, DistributedMethod call stuff, etc)

    if (startClient) {
      if (clientStarted.attemptSet()) {
        startClient(forTests);
      }
    }
  }

  public String getClientID() {
    return String.valueOf(this.dso.getChannel().getClientIDProvider().getClientID().toLong());
  }

  private void resolveClasses() {
    // See LKC-2323 -- A number of Manager methods can be entered from the internals of URLClassLoader (specifically
    // sun.misc.URLClassPath.getLoader()) and can cause deadlocks. Making sure these methods are invoked once, thus
    // resolving any class loads, should eliminate the problem.
    //
    // NOTE: it is entirely possible more signatures might need to added here

    Object o = new Manageable() {
      public void __tc_managed(final TCObject t) {
        throw new AssertionError();
      }

      public TCObject __tc_managed() {
        return null;
      }

      public boolean __tc_isManaged() {
        return false;
      }
    };
    lookupExistingOrNull(o);
    monitorEnter(o, LOCK_TYPE_WRITE, LockContextInfo.NULL_LOCK_CONTEXT_INFO);
    monitorExit(o);
    logicalInvoke(new HashMap(), SerializationUtil.CLEAR_SIGNATURE, new Object[] {});
  }

  private void startClient(final boolean forTests) {
    final TCThreadGroup group = new TCThreadGroup(new ThrowableHandler(TCLogging
        .getLogger(DistributedObjectClient.class)));

    StartupAction action = new StartupHelper.StartupAction() {
      public void execute() throws Throwable {
        AbstractClientFactory clientFactory = AbstractClientFactory.getFactory();
        dso = clientFactory.createClient(config, group, classProvider, connectionComponents, ManagerImpl.this,
                                         dsoCluster, runtimeLogger);

        if (forTests) {
          dso.setCreateDedicatedMBeanServer(true);
        }
        dso.start();
        objectManager = dso.getObjectManager();
        txManager = dso.getTransactionManager();
        methodCallManager = dso.getDmiManager();
        dsoCluster.init(dso.getClusterMetaDataManager(), objectManager);

        shutdownManager = new ClientShutdownManager(objectManager, dso.getRemoteTransactionManager(), dso
            .getStageManager(), dso.getCommunicationsManager(), dso.getChannel(), dso.getClientHandshakeManager(), dso
            .getStatisticsAgentSubSystem(), connectionComponents);
      }

    };

    StartupHelper startupHelper = new StartupHelper(group, action);
    startupHelper.startUp();
  }

  public void stop() {
    shutdown(false);
  }

  private void shutdown(final boolean fromShutdownHook) {
    if (shutdownManager != null) {
      try {
        // XXX: This "fromShutdownHook" flag should be removed. It's only here temporarily to make shutdown behave
        // before I started futzing with it
        shutdownManager.execute(fromShutdownHook);
      } finally {
        // If we're not being called as a result of the shutdown hook, de-register the hook
        if (Thread.currentThread() != shutdownAction) {
          try {
            Runtime.getRuntime().removeShutdownHook(shutdownAction);
          } catch (Exception e) {
            // ignore
          }
        }
      }
    }
  }

  public void logicalInvoke(final Object object, final String methodSignature, final Object[] params) {
    Manageable m = (Manageable) object;
    if (m.__tc_managed() != null) {
      TCObject tco = lookupExistingOrNull(object);

      try {
        if (tco != null) {

          if (SerializationUtil.ADD_ALL_SIGNATURE.equals(methodSignature)) {
            logicalAddAllInvoke(serializer.methodToID(methodSignature), methodSignature, (Collection) params[0], tco);
          } else if (SerializationUtil.ADD_ALL_AT_SIGNATURE.equals(methodSignature)) {
            logicalAddAllAtInvoke(serializer.methodToID(methodSignature), methodSignature, ((Integer) params[0])
                .intValue(), (Collection) params[1], tco);
          } else {
            adjustForJava1ParametersIfNecessary(methodSignature, params);
            tco.logicalInvoke(serializer.methodToID(methodSignature), methodDisplay
                .getDisplayForSignature(methodSignature), params);
          }
        }
      } catch (Throwable t) {
        Util.printLogAndRethrowError(t, logger);
      }
    }
  }

  public void logicalInvokeWithTransaction(final Object object, final Object lockObject, final String methodName, final Object[] params) {
    monitorEnter(lockObject, LockLevel.WRITE, LockContextInfo.NULL_LOCK_CONTEXT_INFO);
    try {
      logicalInvoke(object, methodName, params);
    } finally {
      monitorExit(lockObject);
    }
  }

  private void adjustForJava1ParametersIfNecessary(final String methodName, final Object[] params) {
    if ((params.length == 2) && (params[1] != null) && (params[1].getClass().equals(Integer.class))) {
      if (SerializationUtil.SET_ELEMENT_SIGNATURE.equals(methodName)
          || SerializationUtil.INSERT_ELEMENT_AT_SIGNATURE.equals(methodName)) {
        // special case for reversing parameters
        Object tmp = params[0];
        params[0] = params[1];
        params[1] = tmp;
      }
    }
  }

  private void logicalAddAllInvoke(final int method, final String methodSignature, final Collection collection, final TCObject tcobj) {
    for (Iterator i = collection.iterator(); i.hasNext();) {
      tcobj.logicalInvoke(method, methodDisplay.getDisplayForSignature(methodSignature), new Object[] { i.next() });
    }
  }

  private void logicalAddAllAtInvoke(final int method, final String methodSignature, int index, final Collection collection,
                                     final TCObject tcobj) {

    for (Iterator i = collection.iterator(); i.hasNext();) {
      tcobj.logicalInvoke(method, methodDisplay.getDisplayForSignature(methodSignature), new Object[] {
          new Integer(index++), i.next() });
    }
  }

  public Object lookupOrCreateRoot(final String name, final Object object) {
    return lookupOrCreateRoot(name, object, false);
  }

  public Object lookupOrCreateRootNoDepth(final String name, final Object obj) {
    return lookupOrCreateRoot(name, obj, true);
  }

  public Object createOrReplaceRoot(final String name, final Object object) {
    try {
      return this.objectManager.createOrReplaceRoot(name, object);
    } catch (Throwable t) {
      Util.printLogAndRethrowError(t, logger);

      // shouldn't get here
      throw new AssertionError();
    }
  }

  private Object lookupOrCreateRoot(final String rootName, final Object object, final boolean noDepth) {
    try {
      if (noDepth) { return this.objectManager.lookupOrCreateRootNoDepth(rootName, object); }
      return this.objectManager.lookupOrCreateRoot(rootName, object, true);
    } catch (Throwable t) {
      Util.printLogAndRethrowError(t, logger);

      // shouldn't get here
      throw new AssertionError();
    }
  }

  public void beginLockWithoutTxn(final String lockID, final int type) {
    boolean locked = this.txManager.beginLockWithoutTxn(lockID, type, LockContextInfo.NULL_LOCK_OBJECT_TYPE,
                                                        LockContextInfo.NULL_LOCK_CONTEXT_INFO);
    if (locked && runtimeLogger.getLockDebug()) {
      runtimeLogger.lockAcquired(lockID, type, null, null);
    }
  }

  public void beginLock(final String lockID, final int type, final String contextInfo) {
    try {
      begin(lockID, type, null, null, contextInfo);
    } catch (Throwable t) {
      Util.printLogAndRethrowError(t, logger);
    }
  }

  public void beginVolatile(final TCObject tcObject, final String fieldName, final int type) {
    if (tcObject == null) { throw new NullPointerException("beginVolatile called on a null TCObject"); }

    begin(generateVolatileLockName(tcObject, fieldName), type, null, null, LockContextInfo.NULL_LOCK_CONTEXT_INFO);
  }

  private void begin(final String lockID, final int type, final Object instance, final TCObject tcobj, final String contextInfo) {
    String lockObjectClass = instance == null ? LockContextInfo.NULL_LOCK_OBJECT_TYPE : instance.getClass().getName();

    boolean locked = this.txManager.begin(lockID, type, lockObjectClass, contextInfo);
    if (locked && runtimeLogger.getLockDebug()) {
      runtimeLogger.lockAcquired(lockID, type, instance, tcobj);
    }
  }

  private void beginInterruptibly(final String lockID, final int type, final Object instance, final TCObject tcobj, final String contextInfo)
      throws InterruptedException {
    String lockObjectType = instance == null ? LockContextInfo.NULL_LOCK_OBJECT_TYPE : instance.getClass().getName();

    boolean locked = this.txManager.beginInterruptibly(lockID, type, lockObjectType, contextInfo);
    if (locked && runtimeLogger.getLockDebug()) {
      runtimeLogger.lockAcquired(lockID, type, instance, tcobj);
    }
  }

  private boolean tryBegin(final String lockID, final int type, final Object instance, final TimerSpec timeout, final TCObject tcobj) {
    String lockObjectType = instance == null ? LockContextInfo.NULL_LOCK_OBJECT_TYPE : instance.getClass().getName();

    boolean locked = this.txManager.tryBegin(lockID, timeout, type, lockObjectType);
    if (locked && runtimeLogger.getLockDebug()) {
      runtimeLogger.lockAcquired(lockID, type, instance, tcobj);
    }
    return locked;
  }

  private boolean tryBegin(final String lockID, final int type, final Object instance, final TCObject tcobj) {
    return tryBegin(lockID, type, instance, new TimerSpec(0), tcobj);
  }

  public void commitVolatile(final TCObject tcObject, final String fieldName) {
    if (tcObject == null) { throw new NullPointerException("commitVolatile called on a null TCObject"); }

    commitLock(generateVolatileLockName(tcObject, fieldName));
  }

  public void commitLock(final String lockName) {

    try {
      this.txManager.commit(lockName);
    } catch (Throwable t) {
      Util.printLogAndRethrowError(t, logger);
    }
  }

  public void objectNotify(final Object obj) {
    if (obj == null) { throw new NullPointerException("notify() called on a null reference"); }

    TCObject tco = lookupExistingOrNull(obj);

    if (tco != null) {
      managedObjectNotify(obj, tco, false);
    } else {
      obj.notify();
    }
  }

  public void objectNotifyAll(final Object obj) {
    if (obj == null) { throw new NullPointerException("notifyAll() called on a null reference"); }

    TCObject tco = lookupExistingOrNull(obj);

    if (tco != null) {
      managedObjectNotify(obj, tco, true);
    } else {
      obj.notifyAll();
    }
  }

  private void managedObjectNotify(final Object obj, final TCObject tco, final boolean all) {
    try {
      if (runtimeLogger.getWaitNotifyDebug()) {
        runtimeLogger.objectNotify(all, obj, tco);
      }
      this.txManager.notify(generateAutolockName(tco), all, obj);
    } catch (Throwable t) {
      Util.printLogAndRethrowError(t, logger);
    }
  }

  public void objectWait(final Object obj) throws InterruptedException {
    TCObject tco = lookupExistingOrNull(obj);

    if (tco != null) {
      try {
        TimerSpec call = new TimerSpec();
        if (runtimeLogger.getWaitNotifyDebug()) {
          runtimeLogger.objectWait(call, obj, tco);
        }
        this.txManager.wait(generateAutolockName(tco), call, obj);
      } catch (InterruptedException ie) {
        throw ie;
      } catch (Throwable t) {
        Util.printLogAndRethrowError(t, logger);
      }
    } else {
      obj.wait();
    }
  }

  public void objectWait(final Object obj, final long millis) throws InterruptedException {
    TCObject tco = lookupExistingOrNull(obj);
    if (tco != null) {
      try {
        TimerSpec call = new TimerSpec(millis);
        if (runtimeLogger.getWaitNotifyDebug()) {
          runtimeLogger.objectWait(call, obj, tco);
        }
        this.txManager.wait(generateAutolockName(tco), call, obj);
      } catch (InterruptedException ie) {
        throw ie;
      } catch (Throwable t) {
        Util.printLogAndRethrowError(t, logger);
      }
    } else {
      obj.wait(millis);
    }
  }

  public void objectWait(final Object obj, final long millis, final int nanos) throws InterruptedException {
    TCObject tco = lookupExistingOrNull(obj);

    if (tco != null) {
      try {
        TimerSpec call = new TimerSpec(millis, nanos);
        if (runtimeLogger.getWaitNotifyDebug()) {
          runtimeLogger.objectWait(call, obj, tco);
        }
        this.txManager.wait(generateAutolockName(tco), call, obj);
      } catch (InterruptedException ie) {
        throw ie;
      } catch (Throwable t) {
        Util.printLogAndRethrowError(t, logger);
      }
    } else {
      obj.wait(millis, nanos);
    }
  }

  private boolean isLiteralAutolock(final Object o) {
    if (o instanceof Manageable) { return false; }
    return (!(o instanceof Class)) && (!(o instanceof ObjectID)) && literals.isLiteralInstance(o);
  }

  public boolean isDsoMonitorEntered(final Object o) {
    String lockName = getLockName(o);
    if (lockName == null) { return false; }
    boolean dsoMonitorEntered = txManager.isLockOnTopStack(lockName);

    if (!dsoMonitorEntered && isManaged(o)) {
      logger
          .info("Object "
                + o
                + " is a shared object, but a shared lock is not obtained within a locking context. This usually means the object get shared within a synchronized block/method.");
    }

    return dsoMonitorEntered;
  }

  private String getLockName(final Object obj) {
    TCObject tco = lookupExistingOrNull(obj);
    if (tco != null) {
      return generateAutolockName(tco);
    } else if (isLiteralAutolock(obj)) { return generateLiteralLockName(obj); }
    return null;
  }

  public void monitorEnter(final Object obj, final int type, final String contextInfo) {
    if (obj == null) { throw new NullPointerException("monitorEnter called on a null object"); }

    TCObject tco = lookupExistingOrNull(obj);

    try {
      if (tco != null) {
        if (tco.autoLockingDisabled()) { return; }

        begin(generateAutolockName(tco), type, obj, tco, contextInfo);
      } else if (isLiteralAutolock(obj)) {
        begin(generateLiteralLockName(obj), type, obj, null, contextInfo);
      }
    } catch (Throwable t) {
      Util.printLogAndRethrowError(t, logger);
    }
  }

  public void monitorExit(final Object obj) {
    if (obj == null) { throw new NullPointerException("monitorExit called on a null object"); }

    TCObject tco = lookupExistingOrNull(obj);

    try {
      if (tco != null) {
        if (tco.autoLockingDisabled()) { return; }

        // don't call this.commit() here, the error handling would happen twice in that case
        this.txManager.commit(generateAutolockName(tco));
      } else if (isLiteralAutolock(obj)) {
        this.txManager.commit(generateLiteralLockName(obj));
      }
    } catch (Throwable t) {
      Util.printLogAndRethrowError(t, logger);
    }
  }
  
  public boolean isLocked(final Object obj, final int lockLevel) {
    if (obj == null) { throw new NullPointerException("isLocked called on a null object"); }

    TCObject tco = lookupExistingOrNull(obj);

    if (tco != null) {
      return this.txManager.isLocked(generateAutolockName(tco), lockLevel);
    } else {
      return this.txManager.isLocked(generateLiteralLockName(obj), lockLevel);
    }
  }

  public boolean tryMonitorEnter(final Object obj, final int type, final long timeoutInNanos) {
    if (obj == null) { throw new NullPointerException("monitorEnter called on a null object"); }

    TCObject tco = lookupExistingOrNull(obj);

    try {
      TimerSpec timeout = createTimerSpecFromNanos(timeoutInNanos);

      if (tco != null) {
        if (tco.autoLockingDisabled()) { return false; }

        return tryBegin(generateAutolockName(tco), type, obj, timeout, tco);
      } else if (isLiteralAutolock(obj)) { return tryBegin(generateLiteralLockName(obj), type, obj, timeout, null); }
    } catch (Throwable t) {
      Util.printLogAndRethrowError(t, logger);
    }
    return false;
  }

  public void monitorEnterInterruptibly(final Object obj, final int type) throws InterruptedException {
    if (obj == null) { throw new NullPointerException("monitorEnterInterruptibly called on a null object"); }

    TCObject tco = lookupExistingOrNull(obj);

    try {
      if (tco != null) {
        if (tco.autoLockingDisabled()) { return; }

        beginInterruptibly(generateAutolockName(tco), type, obj, tco, LockContextInfo.NULL_LOCK_CONTEXT_INFO);
      } else if (isLiteralAutolock(obj)) {
        beginInterruptibly(generateLiteralLockName(obj), type, obj, null, LockContextInfo.NULL_LOCK_CONTEXT_INFO);
      }
    } catch (InterruptedException e) {
      throw e;
    } catch (Throwable t) {
      Util.printLogAndRethrowError(t, logger);
    }
  }

  private TimerSpec createTimerSpecFromNanos(final long timeoutInNanos) {
    TimerSpec timeout = null;
    if (timeoutInNanos <= 0) {
      timeout = new TimerSpec(0);
    } else {
      long mills = Util.getMillis(timeoutInNanos);
      int nanos = Util.getNanos(timeoutInNanos, mills);
      timeout = new TimerSpec(mills, nanos);
    }
    return timeout;
  }

  public boolean tryBeginLock(final String lockID, final int type) {
    return tryBegin(lockID, type, null, null);
  }

  public boolean tryBeginLock(final String lockID, final int type, final long timeoutInNanos) {
    return tryBegin(lockID, type, null, createTimerSpecFromNanos(timeoutInNanos), null);
  }

  public int localHeldCount(final Object obj, final int lockLevel) {
    if (obj == null) { throw new NullPointerException("isHeldByCurrentThread called on a null object"); }

    TCObject tco = lookupExistingOrNull(obj);

    if (tco != null) {
      return this.txManager.localHeldCount(generateAutolockName(tco), lockLevel);
    } else {
      return this.txManager.localHeldCount(generateLiteralLockName(obj), lockLevel);
    }

  }

  public boolean isHeldByCurrentThread(final Object obj, final int lockLevel) {
    if (obj == null) { throw new NullPointerException("isHeldByCurrentThread called on a null object"); }

    TCObject tco = lookupExistingOrNull(obj);

    if (tco != null) {
      return this.txManager.isHeldByCurrentThread(generateAutolockName(tco), lockLevel);
    } else {
      return this.txManager.isHeldByCurrentThread(generateLiteralLockName(obj), lockLevel);
    }

  }

  public int queueLength(final Object obj) {
    if (obj == null) { throw new NullPointerException("queueLength called on a null object"); }

    TCObject tco = lookupExistingOrNull(obj);

    if (tco != null) {
      return this.txManager.queueLength(generateAutolockName(tco));
    } else {
      return this.txManager.queueLength(generateLiteralLockName(obj));
    }
  }

  public int waitLength(final Object obj) {
    if (obj == null) { throw new NullPointerException("waitLength called on a null object"); }

    TCObject tco = lookupExistingOrNull(obj);

    if (tco != null) {
      return this.txManager.waitLength(generateAutolockName(tco));
    } else {
      return this.txManager.waitLength(generateLiteralLockName(obj));
    }
  }

  public TCObject shareObjectIfNecessary(final Object pojo) {
    TCObject tobj = lookupExistingOrNull(pojo);
    if (tobj != null) { return tobj; }

    try {
      return this.objectManager.lookupOrShare(pojo);
    } catch (Throwable t) {
      Util.printLogAndRethrowError(t, logger);

      // shouldn't get here
      throw new AssertionError();
    }
  }

  public TCObject lookupOrCreate(final Object obj) {
    if (obj instanceof Manageable) { return ((Manageable) obj).__tc_managed(); }
    return this.objectManager.lookupOrCreate(obj);
  }

  public TCObject lookupExistingOrNull(final Object pojo) {
    if (pojo instanceof Manageable) { return ((Manageable) pojo).__tc_managed(); }

    try {
      return this.objectManager.lookupExistingOrNull(pojo);
    } catch (Throwable t) {
      Util.printLogAndRethrowError(t, logger);

      // shouldn't get here
      throw new AssertionError();
    }
  }

  public void pinLock(String lockName) {
    txManager.pinLock(lockName);
  }

  public void unpinLock(String lockName) {
    txManager.unpinLock(lockName);
  }

  public void evictLock(String lockName) {
    txManager.evictLock(lockName);
  }

  public Object lookupObject(final ObjectID id) throws ClassNotFoundException {
    return this.objectManager.lookupObject(id);
  }

  public Object lookupObject(final ObjectID id, final ObjectID parentContext) throws ClassNotFoundException {
    return this.objectManager.lookupObject(id, parentContext);
  }

  public boolean distributedMethodCall(final Object receiver, final String method, final Object[] params, final boolean runOnAllNodes) {
    TCObject tco = lookupExistingOrNull(receiver);

    try {
      if (tco != null) {
        return methodCallManager.distributedInvoke(receiver, method, params, runOnAllNodes);
      } else {
        return false;
      }
    } catch (Throwable t) {
      Util.printLogAndRethrowError(t, logger);
      return false;
    }
  }

  public void distributedMethodCallCommit() {
    methodCallManager.distributedInvokeCommit();
  }

  public void checkWriteAccess(final Object context) {
    // XXX: make sure that "context" is the ALWAYS the right object to check here, and then rename it
    if (isManaged(context)) {
      try {
        txManager.checkWriteAccess(context);
      } catch (Throwable t) {
        Util.printLogAndRethrowError(t, logger);
      }
    }
  }
  
  public int calculateDsoHashCode(final Object obj) {
    if (literals.isLiteralInstance(obj)) {
      // isLiteralInstance() returns false for array types, so we don't need recursion here.
      return literals.calculateDsoHashCode(obj);
    } 
    if (overridesHashCode(obj)) {
      return obj.hashCode();
    }
    // obj does not have a stable hashCode(); share it and use hash code of its ObjectID
    TCObject tcobject = shareObjectIfNecessary(obj);
    if (tcobject != null) {
      return tcobject.getObjectID().hashCode();
    }
    // A not-shareable, not-literal object?  Hmm, seems we shouldn't get here.
    throw Assert.failure("Cannot calculate stable DSO hash code for an object that is not literal and not shareable");
  }
  
  public boolean isLiteralInstance(final Object obj) {
    return literals.isLiteralInstance(obj);
  }
  
  public boolean isManaged(final Object obj) {
    if (obj instanceof Manageable) {
      TCObject tcobj = ((Manageable) obj).__tc_managed();

      return tcobj != null && tcobj.isShared();
    }
    return this.objectManager.isManaged(obj);
  }

  public boolean isDsoMonitored(final Object obj) {
    if (this.objectManager.isCreationInProgress() || this.txManager.isTransactionLoggingDisabled()) { return false; }

    TCObject tcobj = lookupExistingOrNull(obj);
    if (tcobj != null) {
      return tcobj.isShared();
    } else {
      return isLiteralAutolock(obj);
    }
  }

  public Object lookupRoot(final String name) {
    try {
      return this.objectManager.lookupRoot(name);
    } catch (Throwable t) {
      Util.printLogAndRethrowError(t, logger);

      // shouldn't get here
      throw new AssertionError();
    }
  }

  private static String generateVolatileLockName(final TCObject tcobj, final String fieldName) {
    Assert.assertNotNull(tcobj);
    return ByteCodeUtil.generateVolatileLockName(tcobj.getObjectID(), fieldName);
  }

  private static String generateAutolockName(final TCObject tcobj) {
    Assert.assertNotNull(tcobj);
    return ByteCodeUtil.generateAutolockName(tcobj.getObjectID());
  }

  private static String generateLiteralLockName(final Object obj) {
    Assert.assertNotNull(obj);
    return ByteCodeUtil.generateLiteralLockName(literals.valueFor(obj), obj);
  }

  public boolean isLogical(final Object object) {
    return this.config.isLogical(object.getClass().getName());
  }

  public boolean isRoot(final Field field) {
    String fName = field.getName();
    Class c = field.getDeclaringClass();

    if (Vm.isIBM() && c.getName().startsWith("java.lang.reflect.")) {
      // This avoids a StackOverFlow on ibm jdk -- it does mean that roots defined in classes in java.lang.reflect.*
      // won't work right, but there are other chicken/egg reasons why roots there won't work there too
      return false;
    }

    ClassInfo classInfo = JavaClassInfo.getClassInfo(c);

    FieldInfo[] fields = classInfo.getFields();
    for (FieldInfo fieldInfo : fields) {
      if (fieldInfo.getName().equals(fName)) { return this.config.isRoot(fieldInfo); }
    }

    return false;
  }

  public TCProperties getTCProperites() {
    return TCPropertiesImpl.getProperties();
  }

  private class ShutdownAction implements Runnable {
    public void run() {
      // XXX: we should just call stop(), but for the 1.5 (chex) release, I'm reverting the behavior
      // stop();

      shutdown(true);
    }
  }

  public boolean isPhysicallyInstrumented(final Class clazz) {
    return this.portability.isClassPhysicallyInstrumented(clazz);
  }

  public TCLogger getLogger(final String loggerName) {
    return TCLogging.getLogger(loggerName);
  }

  public InstrumentationLogger getInstrumentationLogger() {
    return instrumentationLogger;
  }

  private static class MethodDisplayNames {

    private final Map display = new HashMap();

    public MethodDisplayNames(final SerializationUtil serializer) {
      String[] sigs = serializer.getSignatures();
      for (String sig : sigs) {
        display.put(sig, getDisplayStringFor(sig));
      }
    }

    private String getDisplayStringFor(final String signature) {
      String methodName = signature.substring(0, signature.indexOf('('));
      StringBuffer rv = new StringBuffer(methodName);
      rv.append('(');

      Type[] args = Type.getArgumentTypes(signature.substring(signature.indexOf('(')));
      for (int i = 0; i < args.length; i++) {
        if (i > 0) {
          rv.append(',');
        }
        Type t = args[i];
        int sort = t.getSort();
        switch (sort) {
          case Type.ARRAY:
            Type elemType = t.getElementType();
            if (elemType.getSort() == Type.OBJECT) {
              rv.append(getShortName(elemType));
            } else {
              rv.append(elemType.getClassName());
            }
            for (int d = t.getDimensions(); d > 0; --d) {
              rv.append("[]");
            }
            break;
          case Type.OBJECT:
            rv.append(getShortName(t));
            break;
          case Type.BOOLEAN:
          case Type.BYTE:
          case Type.CHAR:
          case Type.DOUBLE:
          case Type.FLOAT:
          case Type.INT:
          case Type.LONG:
          case Type.SHORT:
            rv.append(t.getClassName());
            break;
          default:
            throw new AssertionError("unknown sort: " + sort);
        }
      }

      rv.append(')');
      return rv.toString();
    }

    private String getShortName(final Type t) {
      String fqName = t.getClassName();
      int lastDot = fqName.lastIndexOf('.');
      if (lastDot > -1) { return fqName.substring(lastDot + 1); }
      return fqName;
    }

    String getDisplayForSignature(final String methodSignature) {
      String rv = (String) display.get(methodSignature);
      if (rv == null) { throw new AssertionError("missing display string for signature: " + methodSignature); }
      return rv;
    }
  }

  public DmiManager getDmiManager() {
    return this.methodCallManager;
  }

  public boolean isFieldPortableByOffset(final Object pojo, final long fieldOffset) {
    TCObject tcObj = lookupExistingOrNull(pojo);
    return tcObj != null && tcObj.isFieldPortableByOffset(fieldOffset);
  }

  public boolean overridesHashCode(final Object obj) {
    return this.portability.overridesHashCode(obj);
  }

  public void registerNamedLoader(final NamedClassLoader loader, final String webAppName) {
    String loaderName = loader.__tc_getClassLoaderName();
    String appGroup = config.getAppGroup(loaderName, webAppName);
    this.classProvider.registerNamedLoader(loader, appGroup);
  }

  public ClassProvider getClassProvider() {
    return this.classProvider;
  }

  public DsoCluster getDsoCluster() {
    return this.dsoCluster;
  }
}
