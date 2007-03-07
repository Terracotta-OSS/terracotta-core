/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.Type;
import com.tc.cluster.Cluster;
import com.tc.cluster.ClusterEventListener;
import com.tc.lang.StartupHelper;
import com.tc.lang.TCThreadGroup;
import com.tc.lang.ThrowableHandler;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.management.beans.sessions.SessionMonitorMBean;
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
import com.tc.object.lockmanager.api.LockLevel;
import com.tc.object.logging.NullRuntimeLogger;
import com.tc.object.logging.RuntimeLogger;
import com.tc.object.tx.ClientTransactionManager;
import com.tc.object.tx.WaitInvocation;
import com.tc.object.tx.optimistic.OptimisticTransactionManager;
import com.tc.object.tx.optimistic.OptimisticTransactionManagerImpl;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Assert;
import com.tc.util.Util;
import com.tc.util.concurrent.SetOnceFlag;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author teck
 */
public class ManagerImpl implements Manager {
  private static final TCLogger                    logger        = TCLogging.getLogger(Manager.class);

  private final LiteralValues                      literals      = new LiteralValues();

  private final SetOnceFlag                        clientStarted = new SetOnceFlag();
  private final DSOClientConfigHelper              config;
  private final ClassProvider                      classProvider;
  private final boolean                            startClient;
  private final PreparedComponentsFromL2Connection connectionComponents;
  private final Thread                             shutdownAction;
  private final Portability                        portability;
  private final Cluster                            cluster;

  private RuntimeLogger                            runtimeLogger = new NullRuntimeLogger();
  private ClientObjectManager                      objectManager;
  private ClientShutdownManager                    shutdownManager;
  private ClientTransactionManager                 txManager;
  private DistributedObjectClient                  dso;
  private DmiManager                               methodCallManager;
  private OptimisticTransactionManager             optimisticTransactionManager;
  private SerializationUtil                        serializer    = new SerializationUtil();
  private MethodDisplayNames                       methodDisplay = new MethodDisplayNames(serializer);

  public ManagerImpl(DSOClientConfigHelper config, ClassProvider classProvider,
                     PreparedComponentsFromL2Connection connectionComponents) {
    this(true, null, null, config, classProvider, connectionComponents, true);
  }

  // For tests
  public ManagerImpl(boolean startClient, ClientObjectManager objectManager, ClientTransactionManager txManager,
                     DSOClientConfigHelper config, ClassProvider classProvider,
                     PreparedComponentsFromL2Connection connectionComponents) {
    this(startClient, objectManager, txManager, config, classProvider, connectionComponents, true);
  }

  // For tests
  public ManagerImpl(boolean startClient, ClientObjectManager objectManager, ClientTransactionManager txManager,
                     DSOClientConfigHelper config, ClassProvider classProvider,
                     PreparedComponentsFromL2Connection connectionComponents, boolean shutdownActionRequired) {
    this.objectManager = objectManager;
    this.portability = config.getPortability();
    this.txManager = txManager;
    this.config = config;
    this.startClient = startClient;
    this.classProvider = classProvider;
    this.connectionComponents = connectionComponents;
    this.cluster = new Cluster();
    if (shutdownActionRequired) {
      shutdownAction = new Thread(new ShutdownAction());
      // Register a shutdown hook for the DSO client
      Runtime.getRuntime().addShutdownHook(shutdownAction);
    } else {
      shutdownAction = null;
    }

  }

  public SessionMonitorMBean getSessionMonitorMBean() {
    return dso.getSessionMonitorMBean();
  }

  public void init() {
    resolveClasses(); // call this before starting any threads (SEDA, DistributedMethod call stuff, etc)

    if (startClient) {
      if (clientStarted.attemptSet()) {
        startClient();
      }
    }
  }

  public String getClientID() {
    return String.valueOf(this.dso.getChannel().getChannelIDProvider().getChannelID().toLong());
  }

  private void resolveClasses() {
    // See LKC-2323 -- A number of Manager methods can be entered from the internals of URLClassLoader (specifically
    // sun.misc.URLClassPath.getLoader()) and can cause deadlocks. Making sure these methods are invoked once, thus
    // resolving any class loads, should eliminate the problem.
    //
    // NOTE: it is entirely possible more signatures might need to added here

    Object o = new Manageable() {
      public void __tc_managed(TCObject t) {
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
    monitorEnter(o, LOCK_TYPE_WRITE);
    monitorExit(o);
    logicalInvoke(new HashMap(), SerializationUtil.CLEAR_SIGNATURE, new Object[] {});
  }

  private void startClient() {
    final TCThreadGroup group = new TCThreadGroup(new ThrowableHandler(TCLogging
        .getLogger(DistributedObjectClient.class)));

    // Do the client startup in a dedicated thread to ensure all threads we create inherit the proper thread group
    StartupHelper.StartupAction start = new StartupHelper.StartupAction() {
      public void execute() throws Throwable {
        dso = new DistributedObjectClient(config, group, classProvider, connectionComponents, ManagerImpl.this, cluster);
        dso.start();
        objectManager = dso.getObjectManager();
        txManager = dso.getTransactionManager();
        runtimeLogger = dso.getRuntimeLogger();

        optimisticTransactionManager = new OptimisticTransactionManagerImpl(objectManager, txManager);

        methodCallManager = dso.getDmiManager();

        shutdownManager = new ClientShutdownManager(objectManager, dso.getRemoteTransactionManager(), dso
            .getStageManager(), dso.getCommunicationsManager(), dso.getChannel(), dso.getClientHandshakeManager(),
                                                    connectionComponents);
      }
    };

    try {
      new StartupHelper(group, start, "DSO Client Startup").startUp();
    } catch (Throwable t) {
      if (t instanceof Error) { throw (Error) t; }
      if (t instanceof RuntimeException) { throw (RuntimeException) t; }
      throw new RuntimeException(t);
    }
  }

  public void stop() {
    shutdown(false);
  }

  private void shutdown(boolean fromShutdownHook) {
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

  public void logicalInvoke(Object object, String methodSignature, Object[] params) {
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

  public void logicalInvokeWithTransaction(Object object, Object lockObject, String methodName, Object[] params) {
    monitorEnter(lockObject, LockLevel.WRITE);
    try {
      logicalInvoke(object, methodName, params);
    } finally {
      monitorExit(lockObject);
    }
  }

  private void adjustForJava1ParametersIfNecessary(String methodName, Object[] params) {
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

  private void logicalAddAllInvoke(int method, String methodSignature, Collection collection, TCObject tcobj) {
    for (Iterator i = collection.iterator(); i.hasNext();) {
      tcobj.logicalInvoke(method, methodDisplay.getDisplayForSignature(methodSignature), new Object[] { i.next() });
    }
  }

  private void logicalAddAllAtInvoke(int method, String methodSignature, int index, Collection collection,
                                     TCObject tcobj) {

    for (Iterator i = collection.iterator(); i.hasNext();) {
      tcobj.logicalInvoke(method, methodDisplay.getDisplayForSignature(methodSignature), new Object[] {
          new Integer(index++), i.next() });
    }
  }

  public Object lookupOrCreateRoot(String name, Object object) {
    return lookupOrCreateRoot(name, object, false);
  }

  public Object lookupOrCreateRootNoDepth(String name, Object obj) {
    return lookupOrCreateRoot(name, obj, true);
  }

  public Object createOrReplaceRoot(String name, Object object) {
    try {
      return this.objectManager.createOrReplaceRoot(name, object);
    } catch (Throwable t) {
      Util.printLogAndRethrowError(t, logger);

      // shouldn't get here
      throw new AssertionError();
    }
  }

  private Object lookupOrCreateRoot(String rootName, Object object, boolean noDepth) {
    try {
      if (noDepth) { return this.objectManager.lookupOrCreateRootNoDepth(rootName, object); }
      return this.objectManager.lookupOrCreateRoot(rootName, object, true);
    } catch (Throwable t) {
      Util.printLogAndRethrowError(t, logger);

      // shouldn't get here
      throw new AssertionError();
    }
  }

  public void beginLock(String lockID, int type) {
    try {
      begin(lockID, type, null, null);
    } catch (Throwable t) {
      Util.printLogAndRethrowError(t, logger);
    }
  }

  public void beginVolatile(TCObject tcObject, String fieldName, int type) {
    if (tcObject == null) { throw new NullPointerException("beginVolatile called on a null TCObject"); }

    begin(generateVolatileLockName(tcObject, fieldName), type, null, null);
  }

  private void begin(String lockID, int type, Object instance, TCObject tcobj) {
    this.txManager.begin(lockID, type);
    if (runtimeLogger.lockDebug()) {
      runtimeLogger.lockAcquired(lockID, type, instance, tcobj);
    }
  }

  private boolean tryBegin(String lockID, int type, Object instance, TCObject tcobj) {
    boolean locked = this.txManager.tryBegin(lockID, type);
    if (locked && runtimeLogger.lockDebug()) {
      runtimeLogger.lockAcquired(lockID, type, instance, tcobj);
    }
    return locked;
  }

  public void commitVolatile(TCObject tcObject, String fieldName) {
    if (tcObject == null) { throw new NullPointerException("commitVolatile called on a null TCObject"); }

    commitLock(generateVolatileLockName(tcObject, fieldName));
  }

  public void commitLock(String lockName) {

    try {
      this.txManager.commit(lockName);
    } catch (Throwable t) {
      Util.printLogAndRethrowError(t, logger);
    }
  }

  public void objectNotify(Object obj) {
    if (obj == null) { throw new NullPointerException("notify() called on a null reference"); }

    TCObject tco = lookupExistingOrNull(obj);

    if (tco != null) {
      managedObjectNotify(obj, tco, false);
    } else {
      obj.notify();
    }
  }

  public void objectNotifyAll(Object obj) {
    if (obj == null) { throw new NullPointerException("notifyAll() called on a null reference"); }

    TCObject tco = lookupExistingOrNull(obj);

    if (tco != null) {
      managedObjectNotify(obj, tco, true);
    } else {
      obj.notifyAll();
    }
  }

  private void managedObjectNotify(Object obj, TCObject tco, boolean all) {
    try {
      if (runtimeLogger.waitNotifyDebug()) {
        runtimeLogger.objectNotify(all, obj, tco);
      }
      this.txManager.notify(generateAutolockName(tco), all, obj);
    } catch (Throwable t) {
      Util.printLogAndRethrowError(t, logger);
    }
  }

  public void objectWait0(Object obj) throws InterruptedException {
    TCObject tco = lookupExistingOrNull(obj);

    if (tco != null) {
      try {
        WaitInvocation call = new WaitInvocation();
        if (runtimeLogger.waitNotifyDebug()) {
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

  public void objectWait1(Object obj, long millis) throws InterruptedException {
    TCObject tco = lookupExistingOrNull(obj);
    if (tco != null) {
      try {
        WaitInvocation call = new WaitInvocation(millis);
        if (runtimeLogger.waitNotifyDebug()) {
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

  public void objectWait2(Object obj, long millis, int nanos) throws InterruptedException {
    TCObject tco = lookupExistingOrNull(obj);

    if (tco != null) {
      try {
        WaitInvocation call = new WaitInvocation(millis, nanos);
        if (runtimeLogger.waitNotifyDebug()) {
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

  private boolean isLiteralAutolock(Object o) {
    if (o instanceof Manageable) { return false; }
    return (!(o instanceof Class)) && literals.isLiteralInstance(o);
  }

  public void monitorEnter(Object obj, int type) {
    if (obj == null) { throw new NullPointerException("monitorEnter called on a null object"); }

    TCObject tco = lookupExistingOrNull(obj);

    try {
      if (tco != null) {
        if (tco.autoLockingDisabled()) { return; }

        begin(generateAutolockName(tco), type, obj, tco);
      } else if (isLiteralAutolock(obj)) {
        begin(generateLiteralLockName(obj), type, obj, null);
      }
    } catch (Throwable t) {
      Util.printLogAndRethrowError(t, logger);
    }
  }

  public void monitorExit(Object obj) {
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

  public boolean isLocked(Object obj) {
    if (obj == null) { throw new NullPointerException("isLocked called on a null object"); }

    TCObject tco = lookupExistingOrNull(obj);

    if (tco != null) {
      return this.txManager.isLocked(generateAutolockName(tco));
    } else {
      return this.txManager.isLocked(generateLiteralLockName(obj));
    }
  }

  public boolean tryMonitorEnter(Object obj, int type) {
    if (obj == null) { throw new NullPointerException("monitorEnter called on a null object"); }

    TCObject tco = lookupExistingOrNull(obj);

    try {
      if (tco != null) {
        if (tco.autoLockingDisabled()) { return false; }

        return tryBegin(generateAutolockName(tco), type, obj, tco);
      } else if (isLiteralAutolock(obj)) { return tryBegin(generateLiteralLockName(obj), type, obj, null); }
    } catch (Throwable t) {
      Util.printLogAndRethrowError(t, logger);
    }
    return false;
  }

  public boolean tryBeginLock(String lockID, int type) {
    return tryBegin(lockID, type, null, null);
  }

  public boolean isHeldByCurrentThread(Object obj, int lockLevel) {
    if (obj == null) { throw new NullPointerException("isHeldByCurrentThread called on a null object"); }

    TCObject tco = lookupExistingOrNull(obj);

    if (tco != null) {
      return this.txManager.isHeldByCurrentThread(generateAutolockName(tco), lockLevel);
    } else {
      return this.txManager.isHeldByCurrentThread(generateLiteralLockName(obj), lockLevel);
    }

  }

  public int queueLength(Object obj) {
    if (obj == null) { throw new NullPointerException("queueLength called on a null object"); }

    TCObject tco = lookupExistingOrNull(obj);

    if (tco != null) {
      return this.txManager.queueLength(generateAutolockName(tco));
    } else {
      return this.txManager.queueLength(generateLiteralLockName(obj));
    }
  }

  public int waitLength(Object obj) {
    if (obj == null) { throw new NullPointerException("waitLength called on a null object"); }

    TCObject tco = lookupExistingOrNull(obj);

    if (tco != null) {
      return this.txManager.waitLength(generateAutolockName(tco));
    } else {
      return this.txManager.waitLength(generateLiteralLockName(obj));
    }
  }

  public boolean isCreationInProgress() {
    return this.objectManager.isCreationInProgress() || this.txManager.isTransactionLoggingDisabled();
  }

  public TCObject shareObjectIfNecessary(Object pojo) {
    TCObject tobj = ((Manageable) pojo).__tc_managed();
    if (tobj != null) { return tobj; }

    try {
      return this.objectManager.lookupOrShare(pojo);
    } catch (Throwable t) {
      Util.printLogAndRethrowError(t, logger);

      // shouldn't get here
      throw new AssertionError();
    }
  }

  public TCObject lookupOrCreate(Object obj) {
    if (obj instanceof Manageable) { return ((Manageable) obj).__tc_managed(); }
    return this.objectManager.lookupOrCreate(obj);
  }

  public TCObject lookupExistingOrNull(Object pojo) {
    if (pojo instanceof Manageable) { return ((Manageable) pojo).__tc_managed(); }

    try {
      return this.objectManager.lookupExistingOrNull(pojo);
    } catch (Throwable t) {
      Util.printLogAndRethrowError(t, logger);

      // shouldn't get here
      throw new AssertionError();
    }
  }

  public Object lookupObject(ObjectID id) throws ClassNotFoundException {
    return this.objectManager.lookupObject(id);
  }

  public boolean distributedMethodCall(Object receiver, String method, Object[] params, boolean runOnAllNodes) {
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

  public void checkWriteAccess(Object context) {
    // XXX: make sure that "context" is the ALWAYS the right object to check here, and then rename it
    if (isManaged(context)) {
      try {
        txManager.checkWriteAccess(context);
      } catch (Throwable t) {
        Util.printLogAndRethrowError(t, logger);
      }
    }
  }

  public boolean isManaged(Object obj) {
    if (obj instanceof Manageable) {
      TCObject tcobj = ((Manageable) obj).__tc_managed();

      return tcobj != null && tcobj.isShared();
    }
    return this.objectManager.isManaged(obj);
  }

  public Object lookupRoot(String name) {
    try {
      return this.objectManager.lookupRoot(name);
    } catch (Throwable t) {
      Util.printLogAndRethrowError(t, logger);

      // shouldn't get here
      throw new AssertionError();
    }
  }

  private static String generateVolatileLockName(TCObject tcobj, String fieldName) {
    Assert.assertNotNull(tcobj);
    return ByteCodeUtil.generateVolatileLockName(tcobj.getObjectID(), fieldName);
  }

  private static String generateAutolockName(TCObject tcobj) {
    Assert.assertNotNull(tcobj);
    return ByteCodeUtil.generateAutolockName(tcobj.getObjectID());
  }

  private static String generateLiteralLockName(Object obj) {
    Assert.assertNotNull(obj);
    return ByteCodeUtil.generateLiteralLockName(obj);
  }

  public boolean isLogical(Object object) {
    return this.config.isLogical(object.getClass().getName());
  }

  public boolean isRoot(String className, String fieldName) {
    return this.config.isRoot(className, fieldName);
  }

  public Object deepCopy(Object source) {
    Object ret = null;
    try {
      ret = this.objectManager.deepCopy(source, optimisticTransactionManager);
    } catch (Throwable t) {
      Util.printLogAndRethrowError(t, logger);
    }
    return ret;
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

  public void optimisticBegin() {
    this.optimisticTransactionManager.begin();
  }

  public void optimisticCommit() throws ClassNotFoundException {
    this.optimisticTransactionManager.commit();
  }

  public void optimisticRollback() {
    this.optimisticTransactionManager.rollback();
  }

  public boolean isPhysicallyInstrumented(Class clazz) {
    return this.portability.isClassPhysicallyInstrumented(clazz);
  }

  public TCLogger getLogger(String loggerName) {
    return TCLogging.getLogger(loggerName);
  }

  private static class MethodDisplayNames {

    private final Map display = new HashMap();

    public MethodDisplayNames(SerializationUtil serializer) {
      String[] sigs = serializer.getSignatures();
      for (int i = 0; i < sigs.length; i++) {
        display.put(sigs[i], getDisplayStringFor(sigs[i]));
      }
    }

    private String getDisplayStringFor(String signature) {
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

    private String getShortName(Type t) {
      String fqName = t.getClassName();
      int lastDot = fqName.lastIndexOf('.');
      if (lastDot > -1) { return fqName.substring(lastDot + 1); }
      return fqName;
    }

    String getDisplayForSignature(String methodSignature) {
      String rv = (String) display.get(methodSignature);
      if (rv == null) { throw new AssertionError("missing display string for signature: " + methodSignature); }
      return rv;
    }
  }

  public void addClusterEventListener(ClusterEventListener cel) {
    cluster.addClusterEventListener(cel);
  }

  public DmiManager getDmiManager() {
    return this.methodCallManager;
  }

}
