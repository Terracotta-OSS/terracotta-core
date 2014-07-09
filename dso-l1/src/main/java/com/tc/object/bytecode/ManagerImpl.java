/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.abortable.AbortableOperationManager;
import com.tc.abortable.AbortableOperationManagerImpl;
import com.tc.abortable.AbortedOperationException;
import com.tc.client.AbstractClientFactory;
import com.tc.cluster.DsoCluster;
import com.tc.cluster.DsoClusterImpl;
import com.tc.lang.L1ThrowableHandler;
import com.tc.lang.StartupHelper;
import com.tc.lang.StartupHelper.StartupAction;
import com.tc.lang.TCThreadGroup;
import com.tc.license.LicenseManager;
import com.tc.license.ProductID;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.management.TCManagementEvent;
import com.tc.management.TunneledDomainUpdater;
import com.tc.net.ClientID;
import com.tc.net.GroupID;
import com.tc.net.core.security.TCSecurityManager;
import com.tc.object.ClientObjectManager;
import com.tc.object.ClientShutdownManager;
import com.tc.object.DistributedObjectClient;
import com.tc.object.LiteralValues;
import com.tc.object.LogicalOperation;
import com.tc.object.ObjectID;
import com.tc.object.RemoteSearchRequestManager;
import com.tc.object.ServerEventDestination;
import com.tc.object.ServerEventListenerManager;
import com.tc.object.TCObject;
import com.tc.object.bytecode.hook.impl.PreparedComponentsFromL2Connection;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.loaders.ClassProvider;
import com.tc.object.loaders.SingleLoaderClassProvider;
import com.tc.object.locks.ClientLockManager;
import com.tc.object.locks.DsoLockID;
import com.tc.object.locks.LockID;
import com.tc.object.locks.LockIdFactory;
import com.tc.object.locks.LockLevel;
import com.tc.object.locks.Notify;
import com.tc.object.locks.NotifyImpl;
import com.tc.object.locks.UnclusteredLockID;
import com.tc.object.management.ServiceID;
import com.tc.object.metadata.MetaDataDescriptor;
import com.tc.object.metadata.MetaDataDescriptorImpl;
import com.tc.object.tx.ClientTransaction;
import com.tc.object.tx.ClientTransactionManager;
import com.tc.object.tx.ClusterEventListener;
import com.tc.object.tx.OnCommitCallable;
import com.tc.object.tx.TransactionCompleteListener;
import com.tc.object.tx.UnlockedSharedObjectException;
import com.tc.operatorevent.TerracottaOperatorEvent;
import com.tc.operatorevent.TerracottaOperatorEvent.EventLevel;
import com.tc.operatorevent.TerracottaOperatorEvent.EventSubsystem;
import com.tc.operatorevent.TerracottaOperatorEvent.EventType;
import com.tc.operatorevent.TerracottaOperatorEventImpl;
import com.tc.operatorevent.TerracottaOperatorEventLogging;
import com.tc.platform.PlatformService;
import com.tc.platform.PlatformServiceImpl;
import com.tc.platform.rejoin.RejoinManagerImpl;
import com.tc.platform.rejoin.RejoinManagerInternal;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.search.SearchQueryResults;
import com.tc.search.SearchRequestID;
import com.tc.server.ServerEventType;
import com.tc.util.Assert;
import com.tc.util.UUID;
import com.tc.util.Util;
import com.tc.util.concurrent.Runners;
import com.tc.util.concurrent.SetOnceFlag;
import com.tc.util.concurrent.TaskRunner;
import com.tcclient.cluster.DsoClusterInternal;
import com.terracottatech.search.AbstractNVPair;
import com.terracottatech.search.NVPair;
import com.terracottatech.search.SearchBuilder.Search;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;

import javax.management.MBeanServer;

public class ManagerImpl implements Manager {
  private static final TCLogger                       logger                    = TCLogging.getLogger(Manager.class);
  private final SetOnceFlag                           clientStarted             = new SetOnceFlag();
  private final SetOnceFlag                           clientStopped             = new SetOnceFlag();
  private final ClassProvider                         classProvider;
  private final boolean                               startClient;
  private final Thread                                shutdownAction;
  private final DsoClusterInternal                    dsoCluster;
  private final LockIdFactory                         lockIdFactory;
  private final TCSecurityManager                     securityManager;
  private ClientObjectManager                         objectManager;
  private ClientShutdownManager                       shutdownManager;
  private ClientTransactionManager                    txManager;
  private ClientLockManager                           lockManager;
  private RemoteSearchRequestManager                  searchRequestManager;
  private DistributedObjectClient                     dso;

  private volatile DSOClientConfigHelper              config;
  private volatile PreparedComponentsFromL2Connection connectionComponents;
  private final ProductID productId;

  private final ConcurrentHashMap<String, Object>     registeredObjects         = new ConcurrentHashMap<String, Object>();
  private final AbortableOperationManager             abortableOperationManager = new AbortableOperationManagerImpl();
  private final PlatformServiceImpl                   platformService;
  private final RejoinManagerInternal                 rejoinManager;
  private final UUID                                  uuid;
  private ServerEventListenerManager                  serverEventListenerManager;
  private final String                                L1VMShutdownHookName      = "L1 VM Shutdown Hook";
  private volatile TaskRunner                         taskRunner;
  private final Queue<TCManagementEvent>              unfiredTcManagementEvents = new ConcurrentLinkedQueue<TCManagementEvent>();

  public ManagerImpl(final DSOClientConfigHelper config, final PreparedComponentsFromL2Connection connectionComponents,
                     final TCSecurityManager securityManager) {
    this(true, null, null, null, null, config, connectionComponents, true, null, false, securityManager, null);
  }

  public ManagerImpl(final boolean startClient, final ClientObjectManager objectManager,
                     final ClientTransactionManager txManager, final ClientLockManager lockManager,
                     final RemoteSearchRequestManager searchRequestManager, final DSOClientConfigHelper config,
                     final PreparedComponentsFromL2Connection connectionComponents,
                     final TCSecurityManager securityManager) {
    this(startClient, objectManager, txManager, lockManager, searchRequestManager, config, connectionComponents, true,
         null, false, securityManager, null);
  }

  public ManagerImpl(final boolean startClient, final ClientObjectManager objectManager,
                     final ClientTransactionManager txManager, final ClientLockManager lockManager,
                     final RemoteSearchRequestManager searchRequestManager, final DSOClientConfigHelper config,
                     final PreparedComponentsFromL2Connection connectionComponents,
                     final boolean shutdownActionRequired, final ClassLoader loader, final boolean isExpressRejoinMode,
                     final TCSecurityManager securityManager, final ProductID productId) {
    this.objectManager = objectManager;
    this.securityManager = securityManager;
    this.txManager = txManager;
    this.lockManager = lockManager;
    this.searchRequestManager = searchRequestManager;
    this.config = config;
    this.startClient = startClient;
    this.connectionComponents = connectionComponents;
    this.productId = productId;
    this.rejoinManager = new RejoinManagerImpl(isExpressRejoinMode);
    this.dsoCluster = new DsoClusterImpl(rejoinManager);
    this.uuid = UUID.getUUID();
    if (shutdownActionRequired) {
      this.shutdownAction = new Thread(new ShutdownAction(), L1VMShutdownHookName);
      // Register a shutdown hook for the terracotta client
      Runtime.getRuntime().addShutdownHook(this.shutdownAction);
    } else {
      this.shutdownAction = null;
    }
    this.classProvider = new SingleLoaderClassProvider(loader == null ? getClass().getClassLoader() : loader);

    this.lockIdFactory = new LockIdFactory(this);
    this.platformService = new PlatformServiceImpl(this, isExpressRejoinMode);

    logger.info("manager created with rejoinEnabled=" + isExpressRejoinMode);
  }

  public void set(final DSOClientConfigHelper config, final PreparedComponentsFromL2Connection connectionComponents) {
    this.config = config;
    this.connectionComponents = connectionComponents;
  }

  @Override
  public void init() {
    init(false);
  }

  @Override
  public void initForTests() {
    init(true);
  }

  private void init(final boolean forTests) {
    resolveClasses(); // call this before starting any threads (SEDA, DistributedMethod call stuff, etc)

    if (this.startClient) {
      if (this.clientStarted.attemptSet()) {
        startClient(forTests);
        this.platformService.init(rejoinManager, this.dso.getClientHandshakeManager());
      }
    }
  }

  @Override
  public ClientID getClientID() {
    return this.dso.getChannel().getClientIDProvider().getClientID();
  }

  @Override
  public String getUUID() {
    return this.uuid.toString();
  }

  @Override
  public TunneledDomainUpdater getTunneledDomainUpdater() {
    if (null == dso) { return null; }
    return dso.getTunneledDomainManager();
  }

  private void resolveClasses() {
    // fix for deadlock on JDK 1.5 (MNK-2890, MNK-2914)
    new Date().toString();

    // See LKC-2323 -- A number of Manager methods can be entered from the internals of URLClassLoader (specifically
    // sun.misc.URLClassPath.getLoader()) and can cause deadlocks. Making sure these methods are invoked once, thus
    // resolving any class loads, should eliminate the problem.
    //
    // NOTE: it is entirely possible more signatures might need to added here

    final Object o = new Manageable() {
      @Override
      public void __tc_managed(final TCObject t) {
        throw new AssertionError();
      }

      @Override
      public TCObject __tc_managed() {
        return null;
      }

      @Override
      public boolean __tc_isManaged() {
        return false;
      }
    };
    lookupExistingOrNull(o);
    // lock(new StringLockID("test"), LockLevel.WRITE);
    // unlock(new StringLockID("test"), LockLevel.WRITE);
    logicalInvoke(new FakeManageableObject(), LogicalOperation.CLEAR, new Object[] {});
  }

  private void startClient(final boolean forTests) {
    L1ThrowableHandler throwableHandler = new L1ThrowableHandler(TCLogging.getLogger(DistributedObjectClient.class),
                                                                 new Callable<Void>() {
                                                                   @Override
                                                                   public Void call() throws Exception {
                                                                     stop();
                                                                     return null;
                                                                   }
                                                                 });
    final TCThreadGroup group = new TCThreadGroup(throwableHandler);
    this.taskRunner = Runners.newDefaultCachedScheduledTaskRunner(group);
    final StartupAction action = new StartupHelper.StartupAction() {
      @Override
      public void execute() throws Throwable {
        final AbstractClientFactory clientFactory = AbstractClientFactory.getFactory();
        ManagerImpl.this.dso = clientFactory.createClient(ManagerImpl.this.config, group,
                                                          ManagerImpl.this.classProvider,
                                                          ManagerImpl.this.connectionComponents, ManagerImpl.this,
                                                          ManagerImpl.this.dsoCluster,
                                                          ManagerImpl.this.securityManager,
                                                          ManagerImpl.this.abortableOperationManager,
                                                          ManagerImpl.this.rejoinManager, uuid, productId);

        if (forTests) {
          ManagerImpl.this.dso.setCreateDedicatedMBeanServer(true);
        }
        ManagerImpl.this.dso.start();
        ManagerImpl.this.objectManager = ManagerImpl.this.dso.getObjectManager();
        ManagerImpl.this.txManager = ManagerImpl.this.dso.getTransactionManager();
        ManagerImpl.this.lockManager = ManagerImpl.this.dso.getLockManager();
        ManagerImpl.this.searchRequestManager = ManagerImpl.this.dso.getSearchRequestManager();
        ManagerImpl.this.serverEventListenerManager = ManagerImpl.this.dso.getServerEventListenerManager();

        ManagerImpl.this.shutdownManager = new ClientShutdownManager(ManagerImpl.this.objectManager,
                                                                     ManagerImpl.this.dso,
                                                                     ManagerImpl.this.connectionComponents,
                                                                     rejoinManager);

        ManagerImpl.this.dsoCluster.init(ManagerImpl.this.dso.getClusterMetaDataManager(),
                                         ManagerImpl.this.objectManager, ManagerImpl.this.dso.getClusterEventsStage());
        ManagerImpl.this.dsoCluster.addClusterListener(new ClusterEventListener(ManagerImpl.this.shutdownManager
            .getRemoteTransactionManager()));

        // fire queued events now that the DSO client is created
        while (true) {
          TCManagementEvent event = unfiredTcManagementEvents.poll();
          if (event == null) {
            break;
          }
          dso.getManagementServicesManager().sendEvent(event);
        }
        unfiredTcManagementEvents.clear();
      }

    };

    final StartupHelper startupHelper = new StartupHelper(group, action);
    startupHelper.startUp();
  }

  @Override
  public void registerBeforeShutdownHook(final Runnable beforeShutdownHook) {
    if (this.shutdownManager != null) {
      this.shutdownManager.registerBeforeShutdownHook(beforeShutdownHook);
    }
  }
  @Override
  public void unregisterBeforeShutdownHook(final Runnable beforeShutdownHook) {
    if (this.shutdownManager != null) {
      this.shutdownManager.unregisterBeforeShutdownHook(beforeShutdownHook);
    }
  }

  @Override
  public void stop() {
    shutdown(false, false);
  }

  @Override
  public void stopImmediate() {
    shutdown(false, true);
  }

  private void shutdown(boolean fromShutdownHook, boolean forceImmediate) {
    if (clientStopped.attemptSet()) {
      logger.info("shuting down Terracotta Client hook=" + fromShutdownHook + " force=" + forceImmediate);
      shutdownClient(fromShutdownHook, forceImmediate);
    } else {
      logger.info("Client already shutdown.");
    }
  }

  private void shutdownClient(boolean fromShutdownHook, boolean forceImmediate) {
    Assert.eval(clientStopped.isSet());
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

  @Override
  public void logicalInvoke(final Object object, final LogicalOperation method, final Object[] params) {
    final Manageable m = (Manageable) object;
    if (m.__tc_managed() != null) {
      final TCObject tco = lookupExistingOrNull(object);

      try {
        if (tco != null) {

          if (LogicalOperation.ADD_ALL.equals(method)) {
            logicalAddAllInvoke((Collection) params[0], tco);
          } else if (LogicalOperation.ADD_ALL_AT.equals(method)) {
            logicalAddAllAtInvoke(((Integer) params[0]), (Collection) params[1], tco);
          } else {
            tco.logicalInvoke(method, params);
          }
        }
      } catch (final Throwable t) {
        Util.printLogAndRethrowError(t, logger);
      }
    }
  }

  @Override
  public void logicalInvokeWithTransaction(final Object object, final Object lockObject, final LogicalOperation method,
                                           final Object[] params) throws AbortedOperationException {
    final LockID lock = generateLockIdentifier(lockObject);
    lock(lock, LockLevel.WRITE);
    try {
      logicalInvoke(object, method, params);
    } finally {
      unlock(lock, LockLevel.WRITE);
    }
  }

  private void logicalAddAllInvoke(final Collection<?> collection, final TCObject tcobj) {
    for (Object obj : collection) {
      tcobj.logicalInvoke(LogicalOperation.ADD, new Object[] { obj });
    }
  }

  private void logicalAddAllAtInvoke(int index, final Collection<?> collection, final TCObject tcobj) {
    for (Object obj : collection) {
      tcobj.logicalInvoke(LogicalOperation.ADD_AT, new Object[] { index++, obj });
    }
  }

  @Override
  public Object lookupOrCreateRoot(final String name, final Object object) {
    return lookupOrCreateRoot(name, object, false);
  }

  @Override
  public Object lookupOrCreateRoot(final String name, final Object object, GroupID gid) {
    try {
      return this.objectManager.lookupOrCreateRoot(name, object, gid);
    } catch (final Throwable t) {
      Util.printLogAndRethrowError(t, logger);
      throw new AssertionError();
    }
  }

  @Override
  public Object lookupRoot(final String name, GroupID gid) {
    try {
      return this.objectManager.lookupRoot(name, gid);
    } catch (final Throwable t) {
      Util.printLogAndRethrowError(t, logger);
      throw new AssertionError();
    }
  }

  @Override
  public Object lookupOrCreateRootNoDepth(final String name, final Object obj) {
    return lookupOrCreateRoot(name, obj, true);
  }

  @Override
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

  @Override
  public boolean isLiteralAutolock(final Object o) {
    if (o instanceof Manageable) { return false; }
    return (!(o instanceof Class)) && (!(o instanceof ObjectID)) && LiteralValues.isLiteralInstance(o);
  }

  @Override
  public TCObject lookupOrCreate(final Object obj) {
    if (obj instanceof Manageable) {
      TCObject tco = ((Manageable) obj).__tc_managed();
      if (tco != null) { return tco; }
    }

    return this.objectManager.lookupOrCreate(obj);
  }

  @Override
  public TCObject lookupOrCreate(final Object obj, GroupID gid) {
    if (obj instanceof Manageable) {
      TCObject tco = ((Manageable) obj).__tc_managed();
      if (tco != null) { return tco; }
    }

    return this.objectManager.lookupOrCreate(obj, gid);
  }

  @Override
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

  @Override
  public Object lookupObject(final ObjectID id) throws ClassNotFoundException, AbortedOperationException {
    return this.objectManager.lookupObject(id);
  }

  @Override
  public void preFetchObject(final ObjectID id) throws AbortedOperationException {
    this.objectManager.preFetchObject(id);
  }

  @Override
  public Object lookupObject(final ObjectID id, final ObjectID parentContext) throws ClassNotFoundException,
      AbortedOperationException {
    return this.objectManager.lookupObject(id, parentContext);
  }

  @Override
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

  @Override
  public boolean isLiteralInstance(final Object obj) {
    return LiteralValues.isLiteralInstance(obj);
  }

  @Override
  public boolean isManaged(final Object obj) {
    if (obj instanceof Manageable) {
      final TCObject tcobj = ((Manageable) obj).__tc_managed();

      return tcobj != null && tcobj.isShared();
    }
    return this.objectManager.isManaged(obj);
  }

  @Override
  public Object lookupRoot(final String name) {
    try {
      return this.objectManager.lookupRoot(name);
    } catch (final Throwable t) {
      Util.printLogAndRethrowError(t, logger);

      // shouldn't get here
      throw new AssertionError();
    }
  }

  @Override
  public Object getChangeApplicator(final Class clazz) {
    return this.config.getChangeApplicator(clazz);
  }

  @Override
  public boolean isLogical(final Object object) {
    return this.config.isLogical(object.getClass().getName());
  }

  @Override
  public boolean isRoot(final Field field) {
    return false;
  }

  @Override
  public TCProperties getTCProperties() {
    return TCPropertiesImpl.getProperties();
  }

  private class ShutdownAction implements Runnable {
    @Override
    public void run() {
      logger.info("Running L1 VM shutdown hook");
      shutdown(true, false);
    }
  }

  @Override
  public TCLogger getLogger(final String loggerName) {
    return TCLogging.getLogger(loggerName);
  }

  @Override
  public boolean isFieldPortableByOffset(final Object pojo, final long fieldOffset) {
    throw new AssertionError();
  }

  @Override
  public ClassProvider getClassProvider() {
    return this.classProvider;
  }

  @Override
  public DsoCluster getDsoCluster() {
    return this.dsoCluster;
  }

  @Override
  public MBeanServer getMBeanServer() {
    return this.dso.getL1Management().getMBeanServer();
  }

  private static class FakeManageableObject implements Manageable {

    @Override
    public boolean __tc_isManaged() {
      return false;
    }

    @Override
    public void __tc_managed(final TCObject t) {
      //
    }

    @Override
    public TCObject __tc_managed() {
      return null;
    }
  }

  @Override
  public LockID generateLockIdentifier(final long l) {
    return this.lockIdFactory.generateLockIdentifier(l);
  }

  @Override
  public LockID generateLockIdentifier(final String str) {
    return this.lockIdFactory.generateLockIdentifier(str);
  }

  @Override
  public LockID generateLockIdentifier(final Object obj) {
    return this.lockIdFactory.generateLockIdentifier(obj);
  }

  @Override
  public LockID generateLockIdentifier(final Object obj, final String fieldName) {
    return this.lockIdFactory.generateLockIdentifier(obj, fieldName);
  }

  @Override
  public int globalHoldCount(final LockID lock, final LockLevel level) throws AbortedOperationException {
    return this.lockManager.globalHoldCount(lock, level);
  }

  @Override
  public int globalPendingCount(final LockID lock) throws AbortedOperationException {
    return this.lockManager.globalPendingCount(lock);
  }

  @Override
  public int globalWaitingCount(final LockID lock) throws AbortedOperationException {
    return this.lockManager.globalWaitingCount(lock);
  }

  @Override
  public boolean isLocked(final LockID lock, final LockLevel level) throws AbortedOperationException {
    return this.lockManager.isLocked(lock, level);
  }

  @Override
  public boolean isLockedByCurrentThread(final LockID lock, final LockLevel level) throws AbortedOperationException {
    return this.lockManager.isLockedByCurrentThread(lock, level);
  }

  @Override
  public int localHoldCount(final LockID lock, final LockLevel level) throws AbortedOperationException {
    return this.lockManager.localHoldCount(lock, level);
  }

  @Override
  public void lock(final LockID lock, final LockLevel level) throws AbortedOperationException {
    if (clusteredLockingEnabled(lock)) {
      this.lockManager.lock(lock, level);
      this.txManager.begin(lock, level, false);
    }
  }

  @Override
  public void lockInterruptibly(final LockID lock, final LockLevel level) throws InterruptedException,
      AbortedOperationException {
    if (clusteredLockingEnabled(lock)) {
      this.lockManager.lockInterruptibly(lock, level);
      this.txManager.begin(lock, level, false);
    }
  }

  @Override
  public Notify notify(final LockID lock, final Object waitObject) throws AbortedOperationException {
    if (clusteredLockingEnabled(lock) && (lock instanceof DsoLockID)) {
      this.txManager.notify(this.lockManager.notify(lock, waitObject));
    } else {
      waitObject.notify();
    }
    return NotifyImpl.NULL;
  }

  @Override
  public Notify notifyAll(final LockID lock, final Object waitObject) throws AbortedOperationException {
    if (clusteredLockingEnabled(lock) && (lock instanceof DsoLockID)) {
      this.txManager.notify(this.lockManager.notifyAll(lock, waitObject));
    } else {
      waitObject.notifyAll();
    }
    return NotifyImpl.NULL;
  }

  @Override
  public boolean tryLock(final LockID lock, final LockLevel level) throws AbortedOperationException {
    if (clusteredLockingEnabled(lock)) {
      if (this.lockManager.tryLock(lock, level)) {
        this.txManager.begin(lock, level, false);
        return true;
      } else {
        return false;
      }
    } else {
      return true;
    }
  }

  @Override
  public boolean tryLock(final LockID lock, final LockLevel level, final long timeout) throws InterruptedException,
      AbortedOperationException {
    if (clusteredLockingEnabled(lock)) {
      if (this.lockManager.tryLock(lock, level, timeout)) {
        this.txManager.begin(lock, level, false);
        return true;
      } else {
        return false;
      }
    } else {
      return true;
    }
  }

  @Override
  public void beginAtomicTransaction(LockID lock, LockLevel level) throws AbortedOperationException {
    this.lockManager.lock(lock, level);
    this.txManager.begin(lock, level, true);
  }

  @Override
  public void commitAtomicTransaction(LockID lock, LockLevel level) throws AbortedOperationException {
    try {
      this.txManager.commit(lock, level, true, null);
    } finally {
      lockManager.unlock(lock, level);
    }
  }

  private boolean isCurrentTransactionAtomic() {
    ClientTransaction transaction = txManager.getCurrentTransaction();
    return transaction != null && txManager.getCurrentTransaction().isAtomic();
  }

  private OnCommitCallable getUnlockCallback(final LockID lock, final LockLevel level) {
    return new OnCommitCallable() {
      @Override
      public void call() throws AbortedOperationException {
        lockManager.unlock(lock, level);
      }
    };
  }

  @Override
  public void unlock(final LockID lock, final LockLevel level) throws AbortedOperationException {
    if (clusteredLockingEnabled(lock)) {
      // LockManager Unlock callback will be called on commit of current transaction by txnManager.
      this.txManager.commit(lock, level, false, getUnlockCallback(lock, level));
    }
  }

  @Override
  public void wait(final LockID lock, final Object waitObject) throws InterruptedException, AbortedOperationException {
    if (clusteredLockingEnabled(lock) && (lock instanceof DsoLockID)) {
      if (isCurrentTransactionAtomic()) { throw new UnsupportedOperationException(
                                                                                  "Wait is not supported under an atomic transaction"); }
      try {
        this.txManager.commit(lock, LockLevel.WRITE, false, null);
      } catch (final UnlockedSharedObjectException e) {
        throw new IllegalMonitorStateException();
      }
      try {
        this.lockManager.wait(lock, waitObject);
      } finally {
        // XXX this is questionable
        this.txManager.begin(lock, LockLevel.WRITE, false);
      }
    } else {
      waitObject.wait();
    }
  }

  @Override
  public void wait(final LockID lock, final Object waitObject, final long timeout) throws InterruptedException,
      AbortedOperationException {
    if (clusteredLockingEnabled(lock) && (lock instanceof DsoLockID)) {
      if (isCurrentTransactionAtomic()) { throw new UnsupportedOperationException(
                                                                                  "Wait is not supported under an atomic transaction"); }
      try {
        this.txManager.commit(lock, LockLevel.WRITE, false, null);
      } catch (final UnlockedSharedObjectException e) {
        throw new IllegalMonitorStateException();
      }
      try {
        this.lockManager.wait(lock, waitObject, timeout);
      } finally {
        // XXX this is questionable
        this.txManager.begin(lock, LockLevel.WRITE, false);
      }
    } else {
      waitObject.wait(timeout);
    }
  }

  @Override
  public void pinLock(final LockID lock, long awardID) {
    this.lockManager.pinLock(lock, awardID);
  }

  @Override
  public void unpinLock(final LockID lock, long awardID) {
    this.lockManager.unpinLock(lock, awardID);
  }

  private boolean clusteredLockingEnabled(final LockID lock) {
    return !((lock instanceof UnclusteredLockID) || this.txManager.isTransactionLoggingDisabled() || this.txManager
        .isObjectCreationInProgress());
  }

  @Override
  public boolean isLockedByCurrentThread(final LockLevel level) {
    return this.lockManager.isLockedByCurrentThread(level);
  }

  @Override
  public void waitForAllCurrentTransactionsToComplete() throws AbortedOperationException {
    this.txManager.waitForAllCurrentTransactionsToComplete();
  }

  @Override
  public MetaDataDescriptor createMetaDataDescriptor(String category) {
    return new MetaDataDescriptorImpl(category);
  }

  @Override
  public SearchQueryResults executeQuery(String cachename, List queryStack, boolean includeKeys, boolean includeValues,
                                         Set<String> attributeSet, List<NVPair> sortAttributes,
                                         List<NVPair> aggregators, int maxResults, int batchSize, int resultPageSize,
                                         boolean waitForTxn, SearchRequestID reqId)
      throws AbortedOperationException {
    // Paginated queries are already transactional wrt local changes
    if (resultPageSize == Search.BATCH_SIZE_UNLIMITED && shouldWaitForTxn(waitForTxn)) {
      waitForAllCurrentTransactionsToComplete();
    }
    return searchRequestManager.query(cachename, queryStack, includeKeys, includeValues, attributeSet, sortAttributes,
                                      aggregators,
                                      maxResults, batchSize, reqId,
                                      resultPageSize);
  }

  @Override
  public SearchQueryResults executeQuery(String cachename, List queryStack, Set<String> attributeSet,
                                         Set<String> groupByAttribues, List<NVPair> sortAttributes,
                                         List<NVPair> aggregators, int maxResults, int batchSize, boolean waitForTxn,
                                         SearchRequestID reqId)
      throws AbortedOperationException {
    if (shouldWaitForTxn(waitForTxn)) {
      waitForAllCurrentTransactionsToComplete();
    }
    return searchRequestManager.query(cachename, queryStack, attributeSet, groupByAttribues, sortAttributes,
                                      aggregators, maxResults, batchSize, reqId);
  }

  private boolean shouldWaitForTxn(boolean userChoice) {
    return TCPropertiesImpl.getProperties().getBoolean(TCPropertiesConsts.SEARCH_QUERY_WAIT_FOR_TXNS, userChoice);
  }

  @Override
  public NVPair createNVPair(String name, Object value) {
    return AbstractNVPair.createNVPair(name, value);
  }

  // for testing purpose
  public DistributedObjectClient getDso() {
    return this.dso;
  }

  @Override
  public void verifyCapability(String capability) {
    LicenseManager.verifyCapability(capability);
  }

  @Override
  public void fireOperatorEvent(EventLevel eventLevel, EventSubsystem eventSubsystem, EventType eventType,
                                String eventMessage) {
    TerracottaOperatorEvent opEvent = new TerracottaOperatorEventImpl(eventLevel, eventSubsystem, eventType,
                                                                      eventMessage, "");
    TerracottaOperatorEventLogging.getEventLogger().fireOperatorEvent(opEvent);
  }

  @Override
  public GroupID[] getGroupIDs() {
    return this.dso.getGroupIDs();
  }

  @Override
  public void lockIDWait(final LockID lock, final long timeout) throws InterruptedException, AbortedOperationException {
    if (isCurrentTransactionAtomic()) { throw new UnsupportedOperationException(
                                                                                "Wait is not supported under an atomic transaction"); }
    try {
      this.txManager.commit(lock, LockLevel.WRITE, false, null);
    } catch (final UnlockedSharedObjectException e) {
      throw new IllegalMonitorStateException();
    }
    try {
      this.lockManager.wait(lock, null, timeout);
    } finally {
      // XXX this is questionable
      this.txManager.begin(lock, LockLevel.WRITE, false);
    }
  }

  @Override
  public void lockIDNotifyAll(final LockID lock) throws AbortedOperationException {
    this.txManager.notify(this.lockManager.notifyAll(lock, null));
  }

  @Override
  public void lockIDNotify(final LockID lock) throws AbortedOperationException {
    this.txManager.notify(this.lockManager.notify(lock, null));
  }

  @Override
  public Object registerObjectByNameIfAbsent(String name, Object object) {
    Object old = registeredObjects.putIfAbsent(name, object);
    if (old != null) {
      return old;
    } else {
      return object;
    }
  }

  @Override
  public <T> T lookupRegisteredObjectByName(String name, Class<T> expectedType) {
    return expectedType.cast(registeredObjects.get(name));
  }

  @Override
  public void addTransactionCompleteListener(TransactionCompleteListener listener) {
    txManager.getCurrentTransaction().addTransactionCompleteListener(listener);
  }

  @Override
  public AbortableOperationManager getAbortableOperationManager() {
    return abortableOperationManager;
  }

  @Override
  public PlatformService getPlatformService() {
    return platformService;
  }

  @Override
  public void throttlePutIfNecessary(final ObjectID object) throws AbortedOperationException {
    dso.getRemoteResourceManager().throttleIfMutationIfNecessary(object);
  }

  @Override
  public void registerServerEventListener(final ServerEventDestination destination, final Set<ServerEventType> listenTo) {
    serverEventListenerManager.registerListener(destination, listenTo);
  }

  @Override
  public void unregisterServerEventListener(final ServerEventDestination destination, final Set<ServerEventType> listenTo) {
    serverEventListenerManager.unregisterListener(destination, listenTo);
  }

  @Override
  public int getRejoinCount() {
    return rejoinManager.getRejoinCount();
  }

  @Override
  public boolean isRejoinInProgress() {
    return rejoinManager.isRejoinInProgress();
  }

  @Override
  public TaskRunner getTastRunner() {
    return taskRunner;
  }

  @Override
  public long getLockAwardIDFor(LockID lock) {
    return lockManager.getAwardIDFor(lock);
  }

  @Override
  public boolean isLockAwardValid(LockID lock, long awardID) {
    return lockManager.isLockAwardValid(lock, awardID);
  }

  @Override
  public Object registerManagementService(Object service, ExecutorService executorService) {
    ServiceID serviceID = ServiceID.newServiceID(service);
    dso.getManagementServicesManager().registerService(serviceID, service, executorService);
    return serviceID;
  }

  @Override
  public void unregisterManagementService(Object serviceID) {
    if (!(serviceID instanceof ServiceID)) {
      throw new IllegalArgumentException("serviceID object must be of class " + ServiceID.class.getName());
    }
    dso.getManagementServicesManager().unregisterService((ServiceID)serviceID);
  }

  @Override
  public void sendEvent(TCManagementEvent event) {
    // fix for NonStopCacheStartupTest: queue events if the DSO client hasn't been created yet
    if (dso == null) {
      unfiredTcManagementEvents.offer(event);
    } else {
      dso.getManagementServicesManager().sendEvent(event);
    }
  }
}
