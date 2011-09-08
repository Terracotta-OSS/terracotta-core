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
import com.tc.client.ClientMode;
import com.tc.cluster.DsoCluster;
import com.tc.cluster.DsoClusterImpl;
import com.tc.exception.ExceptionWrapper;
import com.tc.exception.ExceptionWrapperImpl;
import com.tc.exception.TCNotRunningException;
import com.tc.lang.StartupHelper;
import com.tc.lang.StartupHelper.StartupAction;
import com.tc.lang.TCThreadGroup;
import com.tc.lang.ThrowableHandler;
import com.tc.license.LicenseManager;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.management.TunneledDomainUpdater;
import com.tc.object.ClientObjectManager;
import com.tc.object.ClientShutdownManager;
import com.tc.object.DistributedObjectClient;
import com.tc.object.LiteralValues;
import com.tc.object.ObjectID;
import com.tc.object.Portability;
import com.tc.object.RemoteSearchRequestManager;
import com.tc.object.SerializationUtil;
import com.tc.object.TCObject;
import com.tc.object.bytecode.hook.impl.PreparedComponentsFromL2Connection;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.event.DmiManager;
import com.tc.object.loaders.ClassProvider;
import com.tc.object.loaders.NamedClassLoader;
import com.tc.object.loaders.StandardClassProvider;
import com.tc.object.locks.ClientLockManager;
import com.tc.object.locks.DsoLockID;
import com.tc.object.locks.LockID;
import com.tc.object.locks.LockIdFactory;
import com.tc.object.locks.LockLevel;
import com.tc.object.locks.Notify;
import com.tc.object.locks.NotifyImpl;
import com.tc.object.locks.UnclusteredLockID;
import com.tc.object.logging.InstrumentationLogger;
import com.tc.object.logging.InstrumentationLoggerImpl;
import com.tc.object.logging.RuntimeLogger;
import com.tc.object.logging.RuntimeLoggerImpl;
import com.tc.object.metadata.AbstractNVPair;
import com.tc.object.metadata.MetaDataDescriptor;
import com.tc.object.metadata.MetaDataDescriptorImpl;
import com.tc.object.metadata.NVPair;
import com.tc.object.tx.ClientTransactionManager;
import com.tc.object.tx.UnlockedSharedObjectException;
import com.tc.operatorevent.TerracottaOperatorEvent;
import com.tc.operatorevent.TerracottaOperatorEvent.EventSubsystem;
import com.tc.operatorevent.TerracottaOperatorEvent.EventType;
import com.tc.operatorevent.TerracottaOperatorEventImpl;
import com.tc.operatorevent.TerracottaOperatorEventLogging;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.search.SearchQueryResults;
import com.tc.statistics.StatisticRetrievalAction;
import com.tc.statistics.StatisticsAgentSubSystem;
import com.tc.statistics.StatisticsAgentSubSystemImpl;
import com.tc.text.ConsoleParagraphFormatter;
import com.tc.text.StringFormatter;
import com.tc.util.Assert;
import com.tc.util.FindbugsSuppressWarnings;
import com.tc.util.Util;
import com.tc.util.concurrent.SetOnceFlag;
import com.tc.util.runtime.Vm;
import com.tcclient.cluster.DsoClusterInternal;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import javax.management.MBeanServer;

public class ManagerImpl implements ManagerInternal {
  private static final TCLogger                    logger              = TCLogging.getLogger(Manager.class);
  private final SetOnceFlag                        clientStarted       = new SetOnceFlag();
  private final SetOnceFlag                        clientStopped       = new SetOnceFlag();
  private final DSOClientConfigHelper              config;
  private final ClassProvider                      classProvider;
  private final boolean                            startClient;
  private final PreparedComponentsFromL2Connection connectionComponents;
  private final Thread                             shutdownAction;
  private final Portability                        portability;
  private final StatisticsAgentSubSystem           statisticsAgentSubSystem;
  private final DsoClusterInternal                 dsoCluster;
  private final RuntimeLogger                      runtimeLogger;
  private final LockIdFactory                      lockIdFactory;
  private final ClientMode                         clientMode;

  private final InstrumentationLogger              instrumentationLogger;

  private ClientObjectManager                      objectManager;
  private ClientShutdownManager                    shutdownManager;
  private ClientTransactionManager                 txManager;
  private ClientLockManager                        lockManager;
  private RemoteSearchRequestManager               searchRequestManager;
  private DistributedObjectClient                  dso;
  private DmiManager                               methodCallManager;

  private final SerializationUtil                  serializer          = new SerializationUtil();
  private final MethodDisplayNames                 methodDisplay       = new MethodDisplayNames(this.serializer);

  private static final boolean                     QUERY_WAIT_FOR_TXNS = TCPropertiesImpl
                                                                           .getProperties()
                                                                           .getBoolean(TCPropertiesConsts.SEARCH_QUERY_WAIT_FOR_TXNS);

  public ManagerImpl(final DSOClientConfigHelper config, final PreparedComponentsFromL2Connection connectionComponents) {
    this(true, null, null, null, null, config, connectionComponents, true, null, null, false);
  }

  public ManagerImpl(final boolean startClient, final ClientObjectManager objectManager,
                     final ClientTransactionManager txManager, final ClientLockManager lockManager,
                     final RemoteSearchRequestManager searchRequestManager, final DSOClientConfigHelper config,
                     final PreparedComponentsFromL2Connection connectionComponents) {
    this(startClient, objectManager, txManager, lockManager, searchRequestManager, config, connectionComponents, true,
         null, null, false);
  }

  public ManagerImpl(final boolean startClient, final ClientObjectManager objectManager,
                     final ClientTransactionManager txManager, final ClientLockManager lockManager,
                     final RemoteSearchRequestManager searchRequestManager, final DSOClientConfigHelper config,
                     final PreparedComponentsFromL2Connection connectionComponents,
                     final boolean shutdownActionRequired, final RuntimeLogger runtimeLogger,
                     final ClassProvider classProvider, final boolean isExpressRejoinMode) {
    this.objectManager = objectManager;
    this.portability = config.getPortability();
    this.txManager = txManager;
    this.lockManager = lockManager;
    this.searchRequestManager = searchRequestManager;
    this.config = config;
    this.instrumentationLogger = new InstrumentationLoggerImpl(config.instrumentationLoggingOptions());
    this.startClient = startClient;
    this.connectionComponents = connectionComponents;
    this.dsoCluster = new DsoClusterImpl();
    this.statisticsAgentSubSystem = new StatisticsAgentSubSystemImpl();
    if (shutdownActionRequired) {
      this.shutdownAction = new Thread(new ShutdownAction());
      // Register a shutdown hook for the DSO client
      Runtime.getRuntime().addShutdownHook(this.shutdownAction);
    } else {
      this.shutdownAction = null;
    }
    this.runtimeLogger = runtimeLogger == null ? new RuntimeLoggerImpl(config) : runtimeLogger;
    this.classProvider = classProvider == null ? new StandardClassProvider(this.runtimeLogger) : classProvider;
    if (config.hasBootJar()) {
      registerStandardLoaders();
    }
    this.lockIdFactory = new LockIdFactory(this);
    this.clientMode = isExpressRejoinMode ? ClientMode.EXPRESS_REJOIN_MODE : ClientMode.DSO_MODE;
  }

  private void registerStandardLoaders() {
    final ClassLoader loader1 = ClassLoader.getSystemClassLoader();
    final ClassLoader loader2 = loader1.getParent();
    final ClassLoader loader3 = loader2.getParent();

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

  public void init() {
    init(false, null);
  }

  public void initForTests() {
    // The method that takes a latch is what we should use in tests now to avoid DMI issues
    throw new UnsupportedOperationException();
  }

  public void initForTests(CountDownLatch latch) {
    init(true, latch);
  }

  private void init(final boolean forTests, final CountDownLatch testStartLatch) {
    resolveClasses(); // call this before starting any threads (SEDA, DistributedMethod call stuff, etc)

    if (this.startClient) {
      if (this.clientStarted.attemptSet()) {
        startClient(forTests, testStartLatch);
      }
    }
  }

  public String getClientID() {
    return Long.toString(this.dso.getChannel().getClientIDProvider().getClientID().toLong());
  }

  public String getUUID() {
    return this.config.getUUID().toString();
  }

  public TunneledDomainUpdater getTunneledDomainUpdater() {
    if (null == dso) { return null; }
    return dso.getTunneledDomainManager();
  }

  private void resolveClasses() {
    // See LKC-2323 -- A number of Manager methods can be entered from the internals of URLClassLoader (specifically
    // sun.misc.URLClassPath.getLoader()) and can cause deadlocks. Making sure these methods are invoked once, thus
    // resolving any class loads, should eliminate the problem.
    //
    // NOTE: it is entirely possible more signatures might need to added here

    final Object o = new Manageable() {
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
    // lock(new StringLockID("test"), LockLevel.WRITE);
    // unlock(new StringLockID("test"), LockLevel.WRITE);
    logicalInvoke(new FakeManageableObject(), SerializationUtil.CLEAR_SIGNATURE, new Object[] {});
  }

  private void startClient(final boolean forTests, final CountDownLatch testStartLatch) {
    final TCThreadGroup group = new TCThreadGroup(new ThrowableHandler(
                                                                       TCLogging
                                                                           .getLogger(DistributedObjectClient.class)));

    final StartupAction action = new StartupHelper.StartupAction() {
      public void execute() throws Throwable {
        final AbstractClientFactory clientFactory = AbstractClientFactory.getFactory();
        ManagerImpl.this.dso = clientFactory.createClient(ManagerImpl.this.config, group,
                                                          ManagerImpl.this.classProvider,
                                                          ManagerImpl.this.connectionComponents, ManagerImpl.this,
                                                          ManagerImpl.this.statisticsAgentSubSystem,
                                                          ManagerImpl.this.dsoCluster, ManagerImpl.this.runtimeLogger,
                                                          ManagerImpl.this.clientMode);

        if (forTests) {
          ManagerImpl.this.dso.setCreateDedicatedMBeanServer(true);
        }
        ManagerImpl.this.dso.start(testStartLatch);
        ManagerImpl.this.objectManager = ManagerImpl.this.dso.getObjectManager();
        ManagerImpl.this.txManager = ManagerImpl.this.dso.getTransactionManager();
        ManagerImpl.this.lockManager = ManagerImpl.this.dso.getLockManager();
        ManagerImpl.this.searchRequestManager = ManagerImpl.this.dso.getSearchRequestManager();
        ManagerImpl.this.methodCallManager = ManagerImpl.this.dso.getDmiManager();

        ManagerImpl.this.shutdownManager = new ClientShutdownManager(ManagerImpl.this.objectManager,
                                                                     ManagerImpl.this.dso,
                                                                     ManagerImpl.this.connectionComponents);

        ManagerImpl.this.dsoCluster.init(ManagerImpl.this.dso.getClusterMetaDataManager(),
                                         ManagerImpl.this.objectManager, ManagerImpl.this.dso.getClusterEventsStage());
      }

    };

    final StartupHelper startupHelper = new StartupHelper(group, action);
    startupHelper.startUp();
  }

  public void registerBeforeShutdownHook(final Runnable beforeShutdownHook) {
    if (this.shutdownManager != null) {
      this.shutdownManager.registerBeforeShutdownHook(beforeShutdownHook);
    }
  }

  public void stop() {
    shutdown(false, false);
  }

  public void stopImmediate() {
    shutdown(false, true);
  }

  private void shutdown(boolean fromShutdownHook, boolean forceImmediate) {
    if (clientStopped.attemptSet()) {
      shutdownClient(fromShutdownHook, forceImmediate);
    } else {
      logger.info("Client already shutdown.");
    }
  }

  private void shutdownClient(boolean fromShutdownHook, boolean forceImmediate) {
    Assert.eval(clientStopped.isSet());
    this.runtimeLogger.shutdown();
    if (this.shutdownManager != null) {
      try {
        // XXX: This "fromShutdownHook" flag should be removed. It's only here temporarily to make shutdown behave
        // before I started futzing with it
        this.shutdownManager.execute(fromShutdownHook, forceImmediate);
      } finally {
        // If we're not being called as a result of the shutdown hook, de-register the hook
        if (Thread.currentThread() != this.shutdownAction) {
          try {
            Runtime.getRuntime().removeShutdownHook(this.shutdownAction);
          } catch (final Exception e) {
            // ignore
          }
        }
      }
    }
  }

  public void logicalInvoke(final Object object, final String methodSignature, final Object[] params) {
    final Manageable m = (Manageable) object;
    if (m.__tc_managed() != null) {
      final TCObject tco = lookupExistingOrNull(object);

      try {
        if (tco != null) {

          if (SerializationUtil.ADD_ALL_SIGNATURE.equals(methodSignature)) {
            logicalAddAllInvoke(this.serializer.methodToID(methodSignature), methodSignature, (Collection) params[0],
                                tco);
          } else if (SerializationUtil.ADD_ALL_AT_SIGNATURE.equals(methodSignature)) {
            logicalAddAllAtInvoke(this.serializer.methodToID(methodSignature), methodSignature,
                                  ((Integer) params[0]).intValue(), (Collection) params[1], tco);
          } else {
            adjustForJava1ParametersIfNecessary(methodSignature, params);
            tco.logicalInvoke(this.serializer.methodToID(methodSignature),
                              this.methodDisplay.getDisplayForSignature(methodSignature), params);
          }
        }
      } catch (final Throwable t) {
        Util.printLogAndRethrowError(t, logger);
      }
    }
  }

  public void logicalInvokeWithTransaction(final Object object, final Object lockObject, final String methodName,
                                           final Object[] params) {
    final LockID lock = generateLockIdentifier(lockObject);
    lock(lock, LockLevel.WRITE);
    try {
      logicalInvoke(object, methodName, params);
    } finally {
      unlock(lock, LockLevel.WRITE);
    }
  }

  private void adjustForJava1ParametersIfNecessary(final String methodName, final Object[] params) {
    if ((params.length == 2) && (params[1] != null) && (params[1].getClass().equals(Integer.class))) {
      if (SerializationUtil.SET_ELEMENT_SIGNATURE.equals(methodName)
          || SerializationUtil.INSERT_ELEMENT_AT_SIGNATURE.equals(methodName)) {
        // special case for reversing parameters
        final Object tmp = params[0];
        params[0] = params[1];
        params[1] = tmp;
      }
    }
  }

  private void logicalAddAllInvoke(final int method, final String methodSignature, final Collection collection,
                                   final TCObject tcobj) {
    for (final Iterator i = collection.iterator(); i.hasNext();) {
      tcobj
          .logicalInvoke(method, this.methodDisplay.getDisplayForSignature(methodSignature), new Object[] { i.next() });
    }
  }

  private void logicalAddAllAtInvoke(final int method, final String methodSignature, int index,
                                     final Collection collection, final TCObject tcobj) {

    for (final Iterator i = collection.iterator(); i.hasNext();) {
      tcobj.logicalInvoke(method, this.methodDisplay.getDisplayForSignature(methodSignature),
                          new Object[] { Integer.valueOf(index++), i.next() });
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
    } catch (final Throwable t) {
      Util.printLogAndRethrowError(t, logger);

      // shouldn't get here
      throw new AssertionError();
    }
  }

  private Object lookupOrCreateRoot(final String rootName, final Object object, final boolean noDepth) {
    try {
      if (noDepth) { return this.objectManager.lookupOrCreateRootNoDepth(rootName, object); }
      return this.objectManager.lookupOrCreateRoot(rootName, object, true);
    } catch (final Throwable t) {
      Util.printLogAndRethrowError(t, logger);

      // shouldn't get here
      throw new AssertionError();
    }
  }

  public boolean isLiteralAutolock(final Object o) {
    if (o instanceof Manageable) { return false; }
    return (!(o instanceof Class)) && (!(o instanceof ObjectID)) && LiteralValues.isLiteralInstance(o);
  }

  public boolean isDsoMonitorEntered(final Object o) {
    if (this.objectManager.isCreationInProgress()) { return false; }

    final LockID lock = generateLockIdentifier(o);
    final boolean dsoMonitorEntered = this.lockManager.isLockedByCurrentThread(lock, null)
                                      || this.txManager.isLockOnTopStack(lock);

    if (isManaged(o) && !dsoMonitorEntered) {
      logger
          .info("An unlock is being attempted on an Object of class "
                + o.getClass().getName()
                + " [Identity Hashcode : 0x"
                + Integer.toHexString(System.identityHashCode(o))
                + "] "
                + " which is a shared object, however there is no associated clustered lock held by the current thread. This usually means that the object became shared within a synchronized block/method.");
    }

    return dsoMonitorEntered;
  }

  public TCObject lookupOrCreate(final Object obj) {
    if (obj instanceof Manageable) {
      TCObject tco = ((Manageable) obj).__tc_managed();
      if (tco != null) { return tco; }
    }

    return this.objectManager.lookupOrCreate(obj);
  }

  public TCObject lookupExistingOrNull(final Object pojo) {
    if (pojo == null) { return null; }

    if (pojo instanceof Manageable) { return ((Manageable) pojo).__tc_managed(); }

    try {
      return this.objectManager.lookupExistingOrNull(pojo);
    } catch (final Throwable t) {
      Util.printLogAndRethrowError(t, logger);

      // shouldn't get here
      throw new AssertionError();
    }
  }

  public Object lookupObject(final ObjectID id) throws ClassNotFoundException {
    return this.objectManager.lookupObject(id);
  }

  public void preFetchObject(final ObjectID id) {
    this.objectManager.preFetchObject(id);
  }

  public Object lookupObject(final ObjectID id, final ObjectID parentContext) throws ClassNotFoundException {
    return this.objectManager.lookupObject(id, parentContext);
  }

  public boolean distributedMethodCall(final Object receiver, final String method, final Object[] params,
                                       final boolean runOnAllNodes) {
    final TCObject tco = lookupExistingOrNull(receiver);

    try {
      if (tco != null) {
        return this.methodCallManager.distributedInvoke(receiver, method, params, runOnAllNodes);
      } else {
        return false;
      }
    } catch (final Throwable t) {
      Util.printLogAndRethrowError(t, logger);
      return false;
    }
  }

  public void distributedMethodCallCommit() {
    this.methodCallManager.distributedInvokeCommit();
  }

  public void checkWriteAccess(final Object context) {
    // XXX: make sure that "context" is the ALWAYS the right object to check here, and then rename it
    if (isManaged(context)) {
      try {
        this.txManager.checkWriteAccess(context);
      } catch (final Throwable t) {
        Util.printLogAndRethrowError(t, logger);
      }
    }
  }

  public int calculateDsoHashCode(final Object obj) {
    if (obj == null) { throw new NullPointerException(); }

    if (LiteralValues.isLiteralInstance(obj)) {
      // isLiteralInstance() returns false for array types, so we don't need recursion here.
      return LiteralValues.calculateDsoHashCode(obj);
    }
    if (overridesHashCode(obj)) { return obj.hashCode(); }

    // obj does not have a stable hashCode(); only if it is already a shared object will we use it's OID as the hashCode
    final TCObject tcobject = lookupExistingOrNull(obj);
    if (tcobject != null) { return tcobject.getObjectID().hashCode(); }

    throw new IllegalArgumentException(
                                       "A cluster-wide stable hash code could not be calculated for the supplied object of type ["
                                           + obj.getClass()
                                           + "]. A cluster-wide stable hash code can only be calculated for instances that are already clustered, or for instances of types that override hashCode() with an implementation based purely on non cluster-transient state.");
  }

  public boolean isLiteralInstance(final Object obj) {
    return LiteralValues.isLiteralInstance(obj);
  }

  public boolean isManaged(final Object obj) {
    if (obj instanceof Manageable) {
      final TCObject tcobj = ((Manageable) obj).__tc_managed();

      return tcobj != null && tcobj.isShared();
    }
    return this.objectManager.isManaged(obj);
  }

  public boolean isDsoMonitored(final Object obj) {
    if (this.objectManager.isCreationInProgress() || this.txManager.isTransactionLoggingDisabled()) { return false; }

    final TCObject tcobj = lookupExistingOrNull(obj);
    if (tcobj != null) {
      return tcobj.isShared();
    } else {
      return isLiteralAutolock(obj);
    }
  }

  public Object lookupRoot(final String name) {
    try {
      return this.objectManager.lookupRoot(name);
    } catch (final Throwable t) {
      Util.printLogAndRethrowError(t, logger);

      // shouldn't get here
      throw new AssertionError();
    }
  }

  public Object getChangeApplicator(final Class clazz) {
    return this.config.getChangeApplicator(clazz);
  }

  public boolean isLogical(final Object object) {
    return this.config.isLogical(object.getClass().getName());
  }

  public boolean isRoot(final Field field) {
    final String fName = field.getName();
    final Class c = field.getDeclaringClass();

    if (Vm.isIBM() && c.getName().startsWith("java.lang.reflect.")) {
      // This avoids a StackOverFlow on ibm jdk -- it does mean that roots defined in classes in java.lang.reflect.*
      // won't work right, but there are other chicken/egg reasons why roots there won't work there too
      return false;
    }

    final ClassInfo classInfo = JavaClassInfo.getClassInfo(c);

    final FieldInfo[] fields = classInfo.getFields();
    for (final FieldInfo fieldInfo : fields) {
      if (fieldInfo.getName().equals(fName)) { return this.config.isRoot(fieldInfo); }
    }

    return false;
  }

  public TCProperties getTCProperties() {
    return TCPropertiesImpl.getProperties();
  }

  private class ShutdownAction implements Runnable {
    public void run() {
      // XXX: we should just call stop(), but for the 1.5 (chex) release, I'm reverting the behavior
      // stop();

      shutdown(true, false);
    }
  }

  public boolean isPhysicallyInstrumented(final Class clazz) {
    return this.portability.isClassPhysicallyInstrumented(clazz);
  }

  public TCLogger getLogger(final String loggerName) {
    return TCLogging.getLogger(loggerName);
  }

  public InstrumentationLogger getInstrumentationLogger() {
    return this.instrumentationLogger;
  }

  private static class MethodDisplayNames {

    private final Map display = new HashMap();

    public MethodDisplayNames(final SerializationUtil serializer) {
      final String[] sigs = serializer.getSignatures();
      for (final String sig : sigs) {
        this.display.put(sig, getDisplayStringFor(sig));
      }
    }

    private String getDisplayStringFor(final String signature) {
      final String methodName = signature.substring(0, signature.indexOf('('));
      final StringBuffer rv = new StringBuffer(methodName);
      rv.append('(');

      final Type[] args = Type.getArgumentTypes(signature.substring(signature.indexOf('(')));
      for (int i = 0; i < args.length; i++) {
        if (i > 0) {
          rv.append(',');
        }
        final Type t = args[i];
        final int sort = t.getSort();
        switch (sort) {
          case Type.ARRAY:
            final Type elemType = t.getElementType();
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
      final String fqName = t.getClassName();
      final int lastDot = fqName.lastIndexOf('.');
      if (lastDot > -1) { return fqName.substring(lastDot + 1); }
      return fqName;
    }

    String getDisplayForSignature(final String methodSignature) {
      final String rv = (String) this.display.get(methodSignature);
      if (rv == null) { throw new AssertionError("missing display string for signature: " + methodSignature); }
      return rv;
    }
  }

  public DmiManager getDmiManager() {
    return this.methodCallManager;
  }

  public boolean isFieldPortableByOffset(final Object pojo, final long fieldOffset) {
    final TCObject tcObj = lookupExistingOrNull(pojo);
    return tcObj != null && tcObj.isFieldPortableByOffset(fieldOffset);
  }

  public boolean overridesHashCode(final Object obj) {
    return this.portability.overridesHashCode(obj);
  }

  public void registerNamedLoader(final NamedClassLoader loader, final String webAppName) {
    final String loaderName = loader.__tc_getClassLoaderName();
    final String appGroup = this.config.getAppGroup(loaderName, webAppName);
    this.classProvider.registerNamedLoader(loader, appGroup);
  }

  public ClassProvider getClassProvider() {
    return this.classProvider;
  }

  public DsoCluster getDsoCluster() {
    return this.dsoCluster;
  }

  public MBeanServer getMBeanServer() {
    return this.dso.getL1Management().getMBeanServer();
  }

  public StatisticRetrievalAction getStatisticRetrievalActionInstance(final String name) {
    if (this.statisticsAgentSubSystem.waitUntilSetupComplete()) {
      return this.statisticsAgentSubSystem.getStatisticsRetrievalRegistry().getActionInstance(name);
    } else {
      return null;
    }
  }

  public void registerStatisticRetrievalAction(StatisticRetrievalAction sra) {
    this.statisticsAgentSubSystem.getStatisticsRetrievalRegistry().registerActionInstance(sra);
  }

  public SessionConfiguration getSessionConfiguration(String appName) {
    return config.getSessionConfiguration(appName);
  }

  private static class FakeManageableObject implements Manageable {

    public boolean __tc_isManaged() {
      return false;
    }

    public void __tc_managed(final TCObject t) {
      //
    }

    public TCObject __tc_managed() {
      return null;
    }
  }

  public LockID generateLockIdentifier(final long l) {
    return this.lockIdFactory.generateLockIdentifier(l);
  }

  public LockID generateLockIdentifier(final String str) {
    return this.lockIdFactory.generateLockIdentifier(str);
  }

  public LockID generateLockIdentifier(final Object obj) {
    return this.lockIdFactory.generateLockIdentifier(obj);
  }

  public LockID generateLockIdentifier(final Object obj, final String fieldName) {
    return this.lockIdFactory.generateLockIdentifier(obj, fieldName);
  }

  public int globalHoldCount(final LockID lock, final LockLevel level) {
    return this.lockManager.globalHoldCount(lock, level);
  }

  public int globalPendingCount(final LockID lock) {
    return this.lockManager.globalPendingCount(lock);
  }

  public int globalWaitingCount(final LockID lock) {
    return this.lockManager.globalWaitingCount(lock);
  }

  public boolean isLocked(final LockID lock, final LockLevel level) {
    return this.lockManager.isLocked(lock, level);
  }

  public boolean isLockedByCurrentThread(final LockID lock, final LockLevel level) {
    return this.lockManager.isLockedByCurrentThread(lock, level);
  }

  public int localHoldCount(final LockID lock, final LockLevel level) {
    return this.lockManager.localHoldCount(lock, level);
  }

  public void lock(final LockID lock, final LockLevel level) {
    if (clusteredLockingEnabled(lock)) {
      this.lockManager.lock(lock, level);
      this.txManager.begin(lock, level);

      if (this.runtimeLogger.getLockDebug()) {
        this.runtimeLogger.lockAcquired(lock, level);
      }
    }
  }

  public void lockInterruptibly(final LockID lock, final LockLevel level) throws InterruptedException {
    if (clusteredLockingEnabled(lock)) {
      this.lockManager.lockInterruptibly(lock, level);
      this.txManager.begin(lock, level);

      if (this.runtimeLogger.getLockDebug()) {
        this.runtimeLogger.lockAcquired(lock, level);
      }
    }
  }

  public Notify notify(final LockID lock, final Object waitObject) {
    if (clusteredLockingEnabled(lock) && (lock instanceof DsoLockID)) {
      this.txManager.notify(this.lockManager.notify(lock, waitObject));
    } else {
      waitObject.notify();
    }
    return NotifyImpl.NULL;
  }

  public Notify notifyAll(final LockID lock, final Object waitObject) {
    if (clusteredLockingEnabled(lock) && (lock instanceof DsoLockID)) {
      this.txManager.notify(this.lockManager.notifyAll(lock, waitObject));
    } else {
      waitObject.notifyAll();
    }
    return NotifyImpl.NULL;
  }

  public boolean tryLock(final LockID lock, final LockLevel level) {
    if (clusteredLockingEnabled(lock)) {
      if (this.lockManager.tryLock(lock, level)) {
        this.txManager.begin(lock, level);
        if (this.runtimeLogger.getLockDebug()) {
          this.runtimeLogger.lockAcquired(lock, level);
        }
        return true;
      } else {
        return false;
      }
    } else {
      return true;
    }
  }

  public boolean tryLock(final LockID lock, final LockLevel level, final long timeout) throws InterruptedException {
    if (clusteredLockingEnabled(lock)) {
      if (this.lockManager.tryLock(lock, level, timeout)) {
        this.txManager.begin(lock, level);
        if (this.runtimeLogger.getLockDebug()) {
          this.runtimeLogger.lockAcquired(lock, level);
        }
        return true;
      } else {
        return false;
      }
    } else {
      return true;
    }
  }

  public void unlock(final LockID lock, final LockLevel level) {
    if (clusteredLockingEnabled(lock)) {
      try {
        this.txManager.commit(lock, level);
      } finally {
        this.lockManager.unlock(lock, level);
      }
    }
  }

  public void wait(final LockID lock, final Object waitObject) throws InterruptedException {
    if (clusteredLockingEnabled(lock) && (lock instanceof DsoLockID)) {
      try {
        this.txManager.commit(lock, LockLevel.WRITE);
      } catch (final UnlockedSharedObjectException e) {
        throw new IllegalMonitorStateException();
      }
      try {
        this.lockManager.wait(lock, waitObject);
      } finally {
        // XXX this is questionable
        this.txManager.begin(lock, LockLevel.WRITE);
      }
    } else {
      waitObject.wait();
    }
  }

  public void wait(final LockID lock, final Object waitObject, final long timeout) throws InterruptedException {
    if (clusteredLockingEnabled(lock) && (lock instanceof DsoLockID)) {
      try {
        this.txManager.commit(lock, LockLevel.WRITE);
      } catch (final UnlockedSharedObjectException e) {
        throw new IllegalMonitorStateException();
      }
      try {
        this.lockManager.wait(lock, waitObject, timeout);
      } finally {
        // XXX this is questionable
        this.txManager.begin(lock, LockLevel.WRITE);
      }
    } else {
      waitObject.wait(timeout);
    }
  }

  public void pinLock(final LockID lock) {
    this.lockManager.pinLock(lock);
  }

  public void unpinLock(final LockID lock) {
    this.lockManager.unpinLock(lock);
  }

  private boolean clusteredLockingEnabled(final LockID lock) {
    return !((lock instanceof UnclusteredLockID) || this.txManager.isTransactionLoggingDisabled() || this.txManager
        .isObjectCreationInProgress());
  }

  public boolean isLockedByCurrentThread(final LockLevel level) {
    return this.lockManager.isLockedByCurrentThread(level);
  }

  public void monitorEnter(final LockID lock, final LockLevel level) {
    lock(lock, level);
  }

  /*
   * We catch IMSE exception here as it can be thrown when an unlock is clustered but the acquiring lock wasn't. This
   * can happen when a user follows the unsupported lock-share-unlock pattern.
   */
  @FindbugsSuppressWarnings("IMSE_DONT_CATCH_IMSE")
  public void monitorExit(final LockID lock, final LockLevel level) {
    try {
      unlock(lock, level);
    } catch (final TCNotRunningException e) {
      logger.info("Ignoring " + e.getClass().getName() + " in unlock(lockID=" + lock + ", level=" + level + ")");
    } catch (final IllegalMonitorStateException e) {
      final ConsoleParagraphFormatter formatter = new ConsoleParagraphFormatter(60, new StringFormatter());
      final ExceptionWrapper wrapper = new ExceptionWrapperImpl();
      logger.fatal(wrapper.wrap(formatter.format(UNLOCK_SHARE_LOCK_ERROR)), e);
      System.exit(-1);
    } catch (final Throwable t) {
      if (clientMode.isExpressRejoinClient()) {
        logger
            .info("Ignoring " + t.getClass().getName() + " in unlock(lockID=" + lock + ", level=" + level + "). " + t);
        logger.info("Shutting down this Express Client");
        try {
          stop();
        } catch (final Throwable e) {
          logger.info("Ignoring " + e);
        }
      } else {
        final ConsoleParagraphFormatter formatter = new ConsoleParagraphFormatter(60, new StringFormatter());
        final ExceptionWrapper wrapper = new ExceptionWrapperImpl();
        logger.fatal(wrapper.wrap(formatter.format(IMMINENT_INFINITE_LOOP_ERROR)), t);
        System.exit(-1);
      }
    }
  }

  private static final String UNLOCK_SHARE_LOCK_ERROR      = "An attempt was just made to unlock a clustered lock that was not locked.  "
                                                             + "This was attempted on exit from a Java synchronized block.  This is highly likely to be due to the calling code locking on an "
                                                             + "object, adding it to the clustered heap, and then attempting to unlock it.  The client JVM will now be terminated to prevent "
                                                             + "the calling thread from entering an infinite loop.";

  private static final String IMMINENT_INFINITE_LOOP_ERROR = "An exception/error was just thrown from an application thread while attempting "
                                                             + "to commit a transaction and unlock the associated lock.  The unlock was called on exiting a Java synchronized block.  In order "
                                                             + "to prevent the calling thread from entering an infinite loop the client JVM will now be terminated.";

  public void waitForAllCurrentTransactionsToComplete() {
    this.txManager.waitForAllCurrentTransactionsToComplete();
  }

  public MetaDataDescriptor createMetaDataDescriptor(String category) {
    return new MetaDataDescriptorImpl(category);
  }

  public SearchQueryResults executeQuery(String cachename, List queryStack, boolean includeKeys, boolean includeValues,
                                         Set<String> attributeSet, List<NVPair> sortAttributes,
                                         List<NVPair> aggregators, int maxResults, int batchSize) {
    if (QUERY_WAIT_FOR_TXNS) {
      waitForAllCurrentTransactionsToComplete();
    }
    return searchRequestManager.query(cachename, queryStack, includeKeys, includeValues, attributeSet, sortAttributes,
                                      aggregators, maxResults, batchSize);
  }

  public NVPair createNVPair(String name, Object value) {
    return AbstractNVPair.createNVPair(name, value);
  }

  // for testing purpose
  public DistributedObjectClient getDso() {
    return this.dso;
  }

  public void verifyCapability(String capability) {
    LicenseManager.verifyCapability(capability);
  }

  public void fireOperatorEvent(EventType eventLevel, EventSubsystem eventSubsystem, String eventMessage) {
    TerracottaOperatorEvent opEvent = new TerracottaOperatorEventImpl(eventLevel, eventSubsystem, eventMessage, "");
    TerracottaOperatorEventLogging.getEventLogger().fireOperatorEvent(opEvent);
  }
}
