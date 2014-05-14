/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import com.google.common.collect.MapMaker;
import com.tc.abortable.AbortableOperationManager;
import com.tc.abortable.AbortedOperationException;
import com.tc.exception.PlatformRejoinException;
import com.tc.exception.TCClassNotFoundException;
import com.tc.exception.TCNonPortableObjectError;
import com.tc.exception.TCNotRunningException;
import com.tc.exception.TCRuntimeException;
import com.tc.logging.ClientIDLogger;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.GroupID;
import com.tc.net.NodeID;
import com.tc.object.bytecode.Manageable;
import com.tc.object.dna.api.DNA;
import com.tc.object.handshakemanager.ClientHandshakeCallback;
import com.tc.object.idprovider.api.ObjectIDProvider;
import com.tc.object.loaders.ClassProvider;
import com.tc.object.loaders.Namespace;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.object.tx.ClientTransaction;
import com.tc.object.tx.ClientTransactionManager;
import com.tc.text.DumpLoggerWriter;
import com.tc.text.PrettyPrintable;
import com.tc.text.PrettyPrinter;
import com.tc.text.PrettyPrinterImpl;
import com.tc.util.AbortedOperationUtil;
import com.tc.util.Assert;
import com.tc.util.BitSetObjectIDSet;
import com.tc.util.Counter;
import com.tc.util.ObjectIDSet;
import com.tc.util.State;
import com.tc.util.Util;
import com.tc.util.VicariousThreadLocal;
import com.tc.util.concurrent.StoppableThread;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

public class ClientObjectManagerImpl implements ClientObjectManager, ClientHandshakeCallback, PortableObjectProvider,
    PrettyPrintable {
  private static final long                      CONCURRENT_LOOKUP_TIMED_WAIT = TimeUnit.SECONDS.toMillis(1L);
  // REFERENCE_MAP_SEG must be power of 2
  private static final int                       REFERENCE_MAP_SEGS           = 32;
  private static final State                     PAUSED                       = new State("PAUSED");
  private static final State                     RUNNING                      = new State("RUNNING");
  private static final State                     STARTING                     = new State("STARTING");
  private static final State                     SHUTDOWN                     = new State("SHUTDOWN");
  private static final State                     REJOIN_IN_PROGRESS           = new State("REJOIN_IN_PROGRESS");

  private static final TCLogger                  staticLogger                 = TCLogging
                                                                                  .getLogger(ClientObjectManager.class);

  private static final long                      POLL_TIME                    = 1000;
  private static final long                      STOP_WAIT                    = POLL_TIME * 3;

  private static final int                       NO_DEPTH                     = 0;

  private volatile State                         state                        = RUNNING;
  private final ConcurrentMap<Object, TCObject>  pojoToManaged                = new MapMaker()
                                                                                  .concurrencyLevel(REFERENCE_MAP_SEGS)
                                                                                  .weakKeys().makeMap();

  private final ClassProvider                    classProvider;
  private final RemoteObjectManager              remoteObjectManager;
  private final Traverser                        traverser;
  private final TraverseTest                     traverseTest;
  private final TCClassFactory                   clazzFactory;
  private final ObjectIDProvider                 idProvider;
  private final TCObjectFactory                  factory;
  private final ObjectStore                      objectStore;

  private ClientTransactionManager               clientTxManager;

  private StoppableThread                        reaper                       = null;
  private final TCLogger                         logger;

  private final Portability                      portability;
  private final ReferenceQueue                   referenceQueue               = new ReferenceQueue();

  private final Map<ObjectID, ObjectLookupState> objectLatchStateMap          = new HashMap<ObjectID, ObjectLookupState>();
  private final ThreadLocal<LocalLookupContext>  localLookupContext           = new VicariousThreadLocal() {

                                                                                @Override
                                                                                protected synchronized LocalLookupContext initialValue() {
                                                                                  return new LocalLookupContext();
                                                                                }

                                                                              };
  private final RootsHolder                      rootsHolder;
  private final AbortableOperationManager        abortableOperationManager;
  private int                                    currentSession               = 0;

  public ClientObjectManagerImpl(final RemoteObjectManager remoteObjectManager, final ObjectIDProvider idProvider,
                                 final ClientIDProvider provider, final ClassProvider classProvider,
                                 final TCClassFactory classFactory, final TCObjectFactory objectFactory,
                                 final Portability portability, TCObjectSelfStore tcObjectSelfStore,
                                 AbortableOperationManager abortableOperationManager) {
    this(remoteObjectManager, idProvider, provider, classProvider, classFactory, objectFactory, portability,
         tcObjectSelfStore, new RootsHolder(new GroupID[] { new GroupID(0) }),
         abortableOperationManager);
  }

  public ClientObjectManagerImpl(final RemoteObjectManager remoteObjectManager, final ObjectIDProvider idProvider,
                                 final ClientIDProvider provider, final ClassProvider classProvider,
                                 final TCClassFactory classFactory, final TCObjectFactory objectFactory,
                                 final Portability portability, TCObjectSelfStore tcObjectSelfStore,
                                 RootsHolder holder,
                                 AbortableOperationManager abortableOperationManager) {
    this.objectStore = new ObjectStore(tcObjectSelfStore);
    this.remoteObjectManager = remoteObjectManager;
    this.idProvider = idProvider;
    this.portability = portability;
    this.logger = new ClientIDLogger(provider, TCLogging.getLogger(ClientObjectManager.class));
    this.classProvider = classProvider;
    this.traverseTest = new NewObjectTraverseTest();
    this.traverser = new Traverser(this);
    this.clazzFactory = classFactory;
    this.factory = objectFactory;
    this.factory.setObjectManager(this);
    this.rootsHolder = holder;
    this.abortableOperationManager = abortableOperationManager;
    startReaper();
    ensureKeyClassesLoaded();
  }

  @Override
  public synchronized void cleanup() {
    checkAndSetstate();
    // tcObjectSelfStore (or L1ServerMapLocalCacheManager) will be cleanup from RemoteServerMapManagerImpl
    // remoteObjectManager will be cleanup from clientHandshakeCallbacks
    currentSession++;
    pojoToManaged.clear();
    objectStore.cleanup();
    clientTxManager.cleanup();
    while (referenceQueue.poll() != null) {
      // cleanup the referenceQueue
    }
    for (ObjectLookupState latchState : objectLatchStateMap.values()) {
      latchState.setObject(null);
    }
    objectLatchStateMap.clear();
    rootsHolder.cleanup();
  }

  private void checkAndSetstate() {
    throwExceptionIfNecessary(true);
    state = REJOIN_IN_PROGRESS;
    notifyAll();
  }

  private void throwExceptionIfNecessary(boolean throwExp) {
    if (state != PAUSED) {
      String message = "cleanup unexpected state: expected " + PAUSED + " but found " + state;
      if (throwExp) {
        throw new IllegalStateException(message);
      } else {
        logger.warn(message);
      }
    }
  }

  private void ensureKeyClassesLoaded() {
    // load LocalLookupContext early to avoid ClassCircularityError: DEV-1386
    new LocalLookupContext();

    /*
     * Exercise isManaged path early to preload classes and avoid ClassCircularityError during any subsequent calls
     */
    isManaged(new Object());
  }

  @Override
  public Class getClassFor(final String className) throws ClassNotFoundException {
    return this.classProvider.getClassFor(className);
  }

  @Override
  public synchronized boolean isLocal(final ObjectID objectID) {
    if (null == objectID) { return false; }

    if (this.objectStore.contains(objectID)) { return true; }

    return this.remoteObjectManager.isInDNACache(objectID);
  }

  @Override
  public synchronized void pause(final NodeID remote, final int disconnected) {
    assertNotPaused("Attempt to pause while PAUSED");
    this.state = PAUSED;
    notifyAll();
  }

  @Override
  public synchronized void unpause(final NodeID remote, final int disconnected) {
    assertNotRunning("Attempt to unpause while RUNNING");
    this.state = RUNNING;
    notifyAll();
  }

  @Override
  public synchronized void initializeHandshake(final NodeID thisNode, final NodeID remoteNode,
                                               final ClientHandshakeMessage handshakeMessage) {
    if (isShutdown()) return;
    assertPausedOrRejoinInProgress("Attempt to initiateHandshake " + thisNode + " <--> " + remoteNode);
    changeStateToStarting();
    ObjectIDSet oids = new BitSetObjectIDSet();
    addAllObjectIDs(oids, remoteNode);
    handshakeMessage.setObjectIDs(oids);

    // Ignore objects reaped before handshaking otherwise those won't be in the list sent to L2 at handshaking.
    // Leave an inconsistent state between L1 and L2. Reaped object is in L1 removeObjects but L2 doesn't aware
    // and send objects over. This can happen when L2 restarted and other L1 makes object requests before this
    // L1's first object request to L2.
    this.remoteObjectManager.clear((GroupID) remoteNode);
  }

  protected void changeStateToStarting() {
    this.state = STARTING;
  }

  private void waitUntilRunning() {
    boolean isInterrupted = false;
    try {
      while (this.state != RUNNING) {
        if (this.state == SHUTDOWN) { throw new TCNotRunningException(); }
        if (this.state == REJOIN_IN_PROGRESS) { throw new PlatformRejoinException(); }
        try {
          wait();
        } catch (final InterruptedException e) {
          isInterrupted = true;
        }
      }
    } finally {
      Util.selfInterruptIfNeeded(isInterrupted);
    }
  }

  private void assertPausedOrRejoinInProgress(final Object message) {
    State current = this.state;
    if (!(current == PAUSED || current == REJOIN_IN_PROGRESS)) { throw new AssertionError(message + ": " + current); }
  }

  private void assertNotPaused(final Object message) {
    if (this.state == PAUSED) { throw new AssertionError(message + ": " + this.state); }
  }

  private void assertNotRunning(final Object message) {
    if (this.state == RUNNING) { throw new AssertionError(message + ": " + this.state); }
  }

  protected synchronized boolean isPaused() {
    return this.state == PAUSED;
  }

  private synchronized boolean isShutdown() {
    return this.state == SHUTDOWN;
  }

  @Override
  public TraversedReferences getPortableObjects(final Class clazz, final Object start, final TraversedReferences addTo) {
    final TCClass tcc = this.clazzFactory.getOrCreate(clazz, this);
    return tcc.getPortableObjects(start, addTo);
  }

  @Override
  public void setTransactionManager(final ClientTransactionManager txManager) {
    this.clientTxManager = txManager;
  }

  @Override
  public ClientTransactionManager getTransactionManager() {
    return this.clientTxManager;
  }

  private LocalLookupContext getLocalLookupContext() {
    return this.localLookupContext.get();
  }

  private void markCreateInProgress(final ObjectLookupState ols, final LocalLookupContext lookupContext) {
    Assert.assertTrue(ols.getOwner() == Thread.currentThread());
    lookupContext.getObjectCreationCount().increment();
  }

  private synchronized ObjectLookupState lookupDone(final ObjectLookupState lookupState) {
    try {
      ObjectLookupState removed = this.objectLatchStateMap.remove(lookupState.getObjectID());
      if (removed != lookupState) {
        // removed can be null if rejoin cleans up state during lookup.
        if (lookupState.getSession() != currentSession) {
          throw new PlatformRejoinException("lookup failed for ObjectID" + lookupState.getObjectID() + " due to rejoin");
        } else {
          throw new AssertionError("wrong removal of lookup state " + removed + " " + lookupState);
        }

      }
      return lookupState;
    } finally {
      getLocalLookupContext().getObjectCreationCount().decrement();
    }
  }

  // For testing purposes
  protected Map getObjectLatchStateMap() {
    return this.objectLatchStateMap;
  }

  private TCObject create(final Object pojo, final GroupID gid) {
    traverse(pojo, new AddManagedObjectAction(), gid);
    return basicLookup(pojo);
  }

  @Override
  public void shutdown(boolean fromShutdownHook) {
    this.objectStore.shutdown(fromShutdownHook);
    synchronized (this) {
      this.state = SHUTDOWN;
      if (this.reaper != null) {
        try {
          stopThread(this.reaper);
        } finally {
          this.reaper = null;
        }
      }
      notifyAll();
    }
  }

  private static void stopThread(final StoppableThread thread) {
    try {
      thread.stopAndWait(STOP_WAIT);
    } finally {
      if (thread.isAlive()) {
        staticLogger.warn(thread.getName() + " is still alive");
      }
    }
  }

  @Override
  public TCObject lookupOrCreate(final Object pojo) {
    if (pojo == null) { return TCObjectFactory.NULL_TC_OBJECT; }
    return lookupOrCreateIfNecesary(pojo, GroupID.NULL_ID);
  }

  @Override
  public TCObject lookupOrCreate(final Object pojo, final GroupID gid) {
    if (pojo == null) { return TCObjectFactory.NULL_TC_OBJECT; }
    return lookupOrCreateIfNecesary(pojo, gid);
  }

  private TCObject lookupOrCreateIfNecesary(final Object pojo, final GroupID gid) {
    Assert.assertNotNull(pojo);
    TCObject obj = basicLookup(pojo);
    if (obj == null || obj.isNew()) {
      executePreCreateMethods(pojo);
      obj = create(pojo, gid);
    }
    return obj;
  }

  private void executePreCreateMethods(final Object pojo) {
    final TCClass tcClass = this.clazzFactory.getOrCreate(pojo.getClass(), this);

    for (final Method m : tcClass.getPreCreateMethods()) {
      executeMethod(pojo, m, "preCreate method (" + m.getName() + ") failed on object of " + pojo.getClass());
    }
  }

  private void executeMethod(final Object pojo, final Method method, final String loggingMessage) {
    // This method used to use beanshell, but I changed it to reflection to hopefully avoid a deadlock -- CDV-130

    try {
      method.invoke(pojo, new Object[] {});
    } catch (Throwable t) {
      if (t instanceof InvocationTargetException) {
        t = t.getCause();
      }
      this.logger.warn(loggingMessage, t);

      wrapIfNeededAndThrow(t);
    }
  }

  private static void wrapIfNeededAndThrow(final Throwable t) {
    if (t instanceof Error) { throw (Error) t; }
    if (t instanceof RuntimeException) { throw (RuntimeException) t; }
    throw new RuntimeException(t);
  }

  private TCObject lookupExistingLiteralRootOrNull(final String rootName, GroupID gid) {
    final ObjectID rootID = this.rootsHolder.getRootIDForName(rootName, gid);
    return basicLookupByID(rootID);
  }

  @Override
  public TCObject lookupExistingOrNull(final Object pojo) {
    return basicLookup(pojo);
  }

  @Override
  public ObjectID lookupExistingObjectID(final Object pojo) {
    if (LiteralValues.isLiteralInstance(pojo)) { return ObjectID.NULL_ID; }
    if (pojo instanceof TCObjectSelf) { return ((TCObjectSelf) pojo).getObjectID(); }

    final TCObject obj = basicLookup(pojo);
    if (obj == null) { throw new AssertionError("Missing object ID for: Object of class " + pojo.getClass().getName()
                                                + " [Identity Hashcode : 0x"
                                                + Integer.toHexString(System.identityHashCode(pojo)) + "] "); }
    return obj.getObjectID();
  }

  /**
   * Prefetch object by ID, faulting into the JVM if necessary, Async lookup and will not cause ObjectNotFoundException
   * like lookupObject. Non-existent objects are ignored by the server.
   * 
   * @param id Object identifier
   * @throws AbortedOperationException
   */
  @Override
  public void preFetchObject(final ObjectID id) throws AbortedOperationException {
    if (id.isNull()) return;

    synchronized (this) {
      if (basicHasLocal(id) || this.objectLatchStateMap.get(id) != null) { return; }
      // We are temporarily marking lookup in progress so that no other thread sneaks in under us and does a lookup
      // while we are calling prefetch
    }
    this.remoteObjectManager.preFetchObject(id);
  }

  @Override
  public Object lookupObjectQuiet(ObjectID id) throws ClassNotFoundException, AbortedOperationException {
    return lookupObject(id, null, false, true);
  }

  @Override
  public Object lookupObjectNoDepth(final ObjectID id) throws ClassNotFoundException, AbortedOperationException {
    return lookupObject(id, null, true, false);
  }

  @Override
  public Object lookupObject(final ObjectID objectID) throws ClassNotFoundException, AbortedOperationException {
    return lookupObject(objectID, null, false, false);
  }

  @Override
  public Object lookupObject(final ObjectID id, final ObjectID parentContext) throws ClassNotFoundException,
      AbortedOperationException {
    return lookupObject(id, parentContext, false, false);
  }

  private Object lookupObject(final ObjectID objectID, final ObjectID parentContext, final boolean noDepth,
                              final boolean quiet) throws ClassNotFoundException, AbortedOperationException {
    if (objectID.isNull()) { return null; }
    Object o = null;
    while (o == null) {
      final TCObject tco = lookup(objectID, parentContext, noDepth, quiet);
      if (tco == null) { throw new AssertionError("TCObject was null for " + objectID);// continue;
      }

      o = tco.getPeerObject();
      if (o == null) {
        reap(objectID);
      }
    }
    return o;
  }

  private void reap(final ObjectID objectID) {
    synchronized (this) {
      final TCObjectImpl tcobj = (TCObjectImpl) basicLookupByID(objectID);
      if (tcobj == null) {
        if (this.logger.isDebugEnabled()) {
          this.logger.debug(System.identityHashCode(this) + " Entry removed before reaper got the chance: " + objectID);
        }
      } else {
        if (tcobj.isNull()) {
          this.objectStore.remove(tcobj);
          // Calling remove from within the synchronized block to make sure there are no races between the lookups and
          // remove.
          this.remoteObjectManager.removed(objectID);
        }
      }
    }

  }

  @Override
  public boolean isManaged(final Object pojo) {
    return pojo != null && !LiteralValues.isLiteral(pojo.getClass().getName()) && lookupExistingOrNull(pojo) != null;
  }

  @Override
  public boolean isCreationInProgress() {
    return getLocalLookupContext().getObjectCreationCount().get() > 0 ? true : false;
  }

  // Done

  @Override
  public TCObject lookup(final ObjectID id) throws ClassNotFoundException, AbortedOperationException {
    return lookup(id, null, false, false);
  }

  @Override
  public TCObject lookupQuiet(final ObjectID id) throws ClassNotFoundException, AbortedOperationException {
    return lookup(id, null, false, true);
  }

  private synchronized ObjectLookupState startLookup(ObjectID oid) {
    if (this.state == REJOIN_IN_PROGRESS) { throw new PlatformRejoinException("Unable to start lookup for objectID"
                                                                              + oid
                                                                              + " due to rejoin in progress state"); }
    ObjectLookupState ols;
    TCObject local = basicLookupByID(oid);

    if (local != null) { return new ObjectLookupState(local); }

    ols = this.objectLatchStateMap.get(oid);
    if (ols != null) {
      // if the object is being created, add to the wait set and return the object
    } else {
      ols = new ObjectLookupState(oid);
      final Object old = this.objectLatchStateMap.put(oid, ols);
      Assert.assertNull(old);
    }
    return ols;
  }

  private TCObject lookup(final ObjectID id, final ObjectID parentContext, final boolean noDepth, final boolean quiet)
      throws AbortedOperationException, ClassNotFoundException {
    TCObject obj = null;
    ObjectLookupState ols = null;

    final LocalLookupContext lookupContext = getLocalLookupContext();

    if (lookupContext.getCallStackCount().increment() == 1) {
      // first time
      this.clientTxManager.disableTransactionLogging();
    }

    while (obj == null) {
      ols = startLookup(id);
      if (!ols.isOwner()) {
        obj = ols.waitForObject();
      } else {
        break;
      }
    }

    try {
      // retrieving object required, first looking up the DNA from the remote server, and creating
      // a pre-init TCObject, then hydrating the object
      if (ols.isOwner()) {
        Assert.assertNull(obj);
        markCreateInProgress(ols, lookupContext);
        try {
          DNA dna = noDepth ? this.remoteObjectManager.retrieve(id, NO_DEPTH)
              : (parentContext == null ? this.remoteObjectManager.retrieve(id) : this.remoteObjectManager
                  .retrieveWithParentContext(id, parentContext));
          obj = createObjectWithDNA(dna);
        } catch (AbortedOperationException t) {
          throw t;
        } catch (final Throwable t) {
          if (!quiet) {
            logger.warn("Exception retrieving object " + id, t);
          }
          this.remoteObjectManager.removed(id);
          // remove the object creating in progress from the list.
          if (t instanceof ClassNotFoundException) { throw (ClassNotFoundException) t; }
          if (t instanceof RuntimeException) { throw (RuntimeException) t; }
          throw new RuntimeException(t);
        } finally {
          lookupDone(ols).setObject(obj);
        }
      }
    } finally {
      if (lookupContext.getCallStackCount().decrement() == 0) {
        this.clientTxManager.enableTransactionLogging();
      }
    }
    return obj;
  }

  private TCObject createObjectWithDNA(DNA dna) throws ClassNotFoundException {
    TCObject obj = null;

    Class clazz = this.classProvider.getClassFor(Namespace.parseClassNameIfNecessary(dna.getTypeName()));
    TCClass tcClazz = clazzFactory.getOrCreate(clazz, this);
    Object pojo = createNewPojoObject(tcClazz, dna);
    obj = this.factory.getNewInstance(dna.getObjectID(), pojo, clazz, false);

    Assert.assertFalse(dna.isDelta());
    // now hydrate the object, this could call resolveReferences which would call this method recursively

    if (obj instanceof TCObjectSelf) {
      obj.hydrate(dna, false, null);
    } else {
      obj.hydrate(dna, false, newWeakObjectReference(dna.getObjectID(), pojo));
    }

    basicAddLocal(obj);
    return obj;
  }

  @Override
  public TCObject addLocalPrefetch(DNA dna) throws ClassNotFoundException, AbortedOperationException {
    remoteObjectManager.addObject(dna);
    return lookup(dna.getObjectID(), null, true, true);
  }

  @Override
  public void removedTCObjectSelfFromStore(TCObjectSelf tcoSelf) {
    synchronized (this) {
      // Calling remove from within the synchronized block to make sure there are no races between the lookups and
      // remove.
      if (logger.isDebugEnabled()) {
        logger.debug("XXX Removing TCObjectSelf from L1 with ObjectID=" + tcoSelf.getObjectID());
      }

      this.remoteObjectManager.removed(tcoSelf.getObjectID());
    }
  }

  @Override
  public synchronized TCObject lookupIfLocal(final ObjectID id) {
    return basicLookupByID(id);
  }

  protected synchronized Set addAllObjectIDs(final Set oids, final NodeID remoteNode) {
    return this.objectStore.addAllObjectIDs(oids);
  }

  @Override
  public Object lookupRoot(final String rootName) {
    return lookupRoot(rootName, this.rootsHolder.getGroupIDForRoot(rootName));
  }

  @Override
  public Object lookupRoot(final String rootName, GroupID gid) {
    try {
      return lookupRootOptionallyCreateOrReplace(rootName, null, false, true, false, gid);
    } catch (final ClassNotFoundException e) {
      throw new TCClassNotFoundException(e);
    }
  }

  /**
   * Check to see if the root is already in existence on the server. If it is then get it if not then create it.
   */
  @Override
  public Object lookupOrCreateRoot(final String rootName, final Object root) {
    try {
      return lookupOrCreateRoot(rootName, root, true, false, this.rootsHolder.getGroupIDForRoot(rootName));
    } catch (final ClassNotFoundException e) {
      throw new TCClassNotFoundException(e);
    }
  }

  /**
   * Check to see if the root is already in existence on the server. If it is then get it if not then create it.
   * 
   * @throws AbortedOperationException
   */
  @Override
  public Object lookupOrCreateRoot(final String rootName, final Object root, final GroupID gid) {
    try {
      return lookupOrCreateRoot(rootName, root, true, false, gid);
    } catch (final ClassNotFoundException e) {
      throw new TCClassNotFoundException(e);
    }
  }

  /**
   * This method must be called within a DSO synchronized context. Currently, this is called in a setter method of a
   * replaceable root.
   * 
   * @throws AbortedOperationException
   */
  @Override
  public Object createOrReplaceRoot(final String rootName, final Object root) {
    final Object existingRoot = lookupRoot(rootName);
    if (existingRoot == null) {
      return lookupOrCreateRoot(rootName, root, false);
    } else if (isLiteralPojo(root)) {
      final TCObject tcObject = lookupExistingLiteralRootOrNull(rootName, rootsHolder.getGroupIDForRoot(rootName));
      tcObject.literalValueChanged(root, existingRoot);
      return root;
    } else {
      return lookupOrCreateRoot(rootName, root, false);
    }
  }

  @Override
  public Object lookupOrCreateRootNoDepth(final String rootName, final Object root) {
    try {
      return lookupOrCreateRoot(rootName, root, true, true, this.rootsHolder.getGroupIDForRoot(rootName));
    } catch (final ClassNotFoundException e) {
      throw new TCClassNotFoundException(e);
    }
  }

  @Override
  public Object lookupOrCreateRoot(final String rootName, final Object root, final boolean dsoFinal) {
    try {
      return lookupOrCreateRoot(rootName, root, dsoFinal, false, this.rootsHolder.getGroupIDForRoot(rootName));
    } catch (final ClassNotFoundException e) {
      throw new TCClassNotFoundException(e);
    }
  }

  private boolean isLiteralPojo(final Object pojo) {
    return !(pojo instanceof Class) && LiteralValues.isLiteralInstance(pojo);
  }

  private Object lookupOrCreateRoot(final String rootName, final Object root, final boolean dsoFinal,
                                    final boolean noDepth, GroupID gid) throws ClassNotFoundException {
    if (root != null) {
      // this will throw an exception if root is not portable
      checkPortabilityOfRoot(root, rootName, root.getClass());
    }

    return lookupRootOptionallyCreateOrReplace(rootName, root, true, dsoFinal, noDepth, gid);
  }

  private void checkPortabilityOfTraversedReference(final TraversedReference reference, final Class referringClass) {
    if (!portability.isPortableInstance(reference.getValue())) {
      //
      throw new TCNonPortableObjectError("Attempt to share an instance of a non-portable class ("
                                         + reference.getValue().getClass().getName()
                                         + ") referenced by a portable class (" + referringClass.getName() + ").");
    }
  }

  private void checkPortabilityOfRoot(final Object root, final String rootName, final Class rootType)
      throws TCNonPortableObjectError {
    if (!portability.isPortableInstance(root)) {
      //
      throw new TCNonPortableObjectError("Attempt to share an instance of a non-portable class (" + rootType.getName()
                                         + ") by assigning it to a root (" + rootName + ").");
    }
  }

  @Override
  public void checkPortabilityOfField(final Object fieldValue, final String fieldName, final Object pojo)
      throws TCNonPortableObjectError {
    if (!portability.isPortableInstance(fieldValue)) {
      //
      throw new TCNonPortableObjectError("Attempt to set the field (" + pojo.getClass().getName() + "." + fieldName
                                         + ") of a shared object to an instance of a non-portable class ("
                                         + fieldValue.getClass().getName() + ".");
    }
  }

  @Override
  public void checkPortabilityOfLogicalAction(final LogicalOperation method, final Object[] params, final int index,
                                              final Object pojo) throws TCNonPortableObjectError {
    final Object param = params[index];
    if (!portability.isPortableInstance(param)) {
      //
      throw new TCNonPortableObjectError("Attempt to share an instance of a non-portable class ("
                                         + param.getClass().getName() + ") by"
                                         + " passing it as an argument to a method (" + method
                                         + ") of a logically-managed class (" + pojo.getClass().getName() + ".");
    }
  }

  private boolean rootLookupInProgress(final String rootName, GroupID gid) {
    return this.rootsHolder.isLookupInProgress(rootName, gid);
  }

  private void markRootLookupInProgress(final String rootName, GroupID gid) {
    final boolean wasAdded = this.rootsHolder.markRootLookupInProgress(rootName, gid);
    if (!wasAdded) { throw new AssertionError("Attempt to mark a root lookup that is already in progress."); }
  }

  private void markRootLookupNotInProgress(final String rootName, GroupID gid) {
    final boolean removed = this.rootsHolder.unmarkRootLookupInProgress(rootName, gid);
    if (!removed) { throw new AssertionError("Attempt to unmark a root lookup that wasn't in progress."); }
  }

  @Override
  public void replaceRootIDIfNecessary(String rootName, ObjectID newRootID) {
    replaceRootIDIfNecessary(rootName, new GroupID(newRootID.getGroupID()), newRootID);
  }

  public synchronized void replaceRootIDIfNecessary(final String rootName, final GroupID gid, final ObjectID newRootID) {
    waitUntilRunning();

    final ObjectID oldRootID = this.rootsHolder.getRootIDForName(rootName, gid);
    if (oldRootID == null || oldRootID.equals(newRootID)) { return; }

    this.rootsHolder.addRoot(rootName, newRootID);
  }

  private Object lookupRootOptionallyCreateOrReplace(final String rootName, final Object rootPojo,
                                                     final boolean create, final boolean dsoFinal,
                                                     final boolean noDepth, GroupID gid) throws ClassNotFoundException {
    final boolean replaceRootIfExistWhenCreate = !dsoFinal && create;

    ObjectID rootID = null;
    gid = gid == null || GroupID.NULL_ID.equals(gid) ? rootsHolder.getGroupIDForRoot(rootName) : gid;

    boolean lookupInProgress = false;
    boolean isInterrupted = false;

    try {
      synchronized (this) {
        while (true) {
          if (!replaceRootIfExistWhenCreate) {
            rootID = this.rootsHolder.getRootIDForName(rootName, gid);
            if (rootID != null) {
              break;
            }
          } else {
            rootID = ObjectID.NULL_ID;
          }
          if (!rootLookupInProgress(rootName, gid)) {
            lookupInProgress = true;
            markRootLookupInProgress(rootName, gid);
            break;
          } else {
            try {
              wait();
            } catch (final InterruptedException e) {
              logger.debug("root lookup interrupted", e);
              isInterrupted = true;
            }
          }
        }
      }
    } finally {
      Util.selfInterruptIfNeeded(isInterrupted);
    }

    boolean retrieveNeeded = lookupInProgress && !replaceRootIfExistWhenCreate;

    boolean isNew = retrieveNeeded || (rootID.isNull() && create);
    if (retrieveNeeded) {
      rootID = this.remoteObjectManager.retrieveRootID(rootName, gid);
    }

    if (rootID.isNull() && create) {
      Assert.assertNotNull(rootPojo);
      // TODO:: Optimize this, do lazy instantiation
      TCObject root = null;
      if (isLiteralPojo(rootPojo)) {
        throw new UnsupportedOperationException("Literal Roots are Not supported");
      } else {
        root = lookupOrCreate(rootPojo, gid);
      }
      rootID = root.getObjectID();
      this.clientTxManager.createRoot(rootName, rootID);
    }
    synchronized (this) {
      if (isNew && !rootID.isNull()) {
        this.rootsHolder.addRoot(rootName, rootID);
      }
      if (lookupInProgress) {
        markRootLookupNotInProgress(rootName, gid);
        notifyAll();
      }
    }

    try {
      return lookupObject(rootID, null, noDepth, false);
    } catch (AbortedOperationException e) {
      throw new TCRuntimeException(e);
    }
  }

  private TCObject basicLookupByID(final ObjectID id) {
    if (!Thread.holdsLock(this)) { throw new AssertionError("not holding lock"); }
    return this.objectStore.get(id);
  }

  private boolean basicHasLocal(final ObjectID id) {
    return basicLookupByID(id) != null;
  }

  private TCObject basicLookup(final Object obj) {
    if (obj == null) { return null; }
    if (obj instanceof TCObjectSelf) {
      TCObjectSelf self = (TCObjectSelf) obj;
      if (self.isInitialized()) {
        return self;
      } else {
        // return null when not initialized yet, as __tc_managed() is itself and not null
        return null;
      }
    }

    if (obj instanceof Manageable) { return ((Manageable) obj).__tc_managed(); }

    return this.pojoToManaged.get(obj);
  }

  private synchronized void basicAddLocal(final TCObject obj) {
    final ObjectID id = obj.getObjectID();
    if (basicHasLocal(id)) { throw Assert.failure("Attempt to add an object that already exists: Object of class "
                                                  + obj.getClass() + " [Identity Hashcode : 0x"
                                                  + Integer.toHexString(System.identityHashCode(obj)) + "] "); }
    this.objectStore.add(obj);

    final Object pojo = obj.getPeerObject();

    if (pojo != null) {
      if (pojo instanceof Manageable) {
        final Manageable m = (Manageable) pojo;
        if (m.__tc_managed() == null) {
          m.__tc_managed(obj);
        } else {
          Assert.assertTrue(m.__tc_managed() == obj);
        }
      } else {
        if (!isLiteralPojo(pojo)) {
          this.pojoToManaged.put(pojo, obj);
        }
      }
    }
  }

  private void traverse(final Object root, final TraversalAction action, final GroupID gid) {
    // if set this will be final exception thrown
    Throwable exception = null;

    final PostCreateMethodGatherer postCreate = (PostCreateMethodGatherer) action;
    try {
      this.traverser.traverse(root, this.traverseTest, action, gid);
    } catch (final Throwable t) {
      exception = t;
    } finally {
      // even if we're throwing an exception from the traversal the postCreate methods for the objects that became
      // shared should still be called
      for (final Entry<Object, List<Method>> entry : postCreate.getPostCreateMethods().entrySet()) {
        final Object target = entry.getKey();

        for (final Method method : entry.getValue()) {
          try {
            executeMethod(target, method,
                          "postCreate method (" + method.getName() + ") failed on object of " + target.getClass());
          } catch (final Throwable t) {
            if (exception == null) {
              exception = t;
            } else {
              // exceptions are already logged, no need to do it here
            }
          }
        }
      }
    }

    if (exception != null) {
      wrapIfNeededAndThrow(exception);
    }
  }

  private class AddManagedObjectAction implements TraversalAction, PostCreateMethodGatherer {
    private final Map<Object, List<Method>> toCall = new IdentityHashMap<Object, List<Method>>();

    @Override
    public final void visit(final List objects, final GroupID gid) {
      for (final Object pojo : objects) {
        final List<Method> postCreateMethods = ClientObjectManagerImpl.this.clazzFactory
            .getOrCreate(pojo.getClass(), ClientObjectManagerImpl.this).getPostCreateMethods();
        if (!postCreateMethods.isEmpty()) {
          final Object prev = this.toCall.put(pojo, postCreateMethods);
          Assert.assertNull(prev);
        }
      }

      final List tcObjects = basicCreateIfNecessary(objects, gid);
      for (final Iterator i = tcObjects.iterator(); i.hasNext();) {
        ClientObjectManagerImpl.this.clientTxManager.createObject((TCObject) i.next());
      }
    }

    @Override
    public final Map<Object, List<Method>> getPostCreateMethods() {
      return this.toCall;
    }
  }

  private class NewObjectTraverseTest implements TraverseTest {

    @Override
    public boolean shouldTraverse(final Object object) {
      // literals should be skipped -- without this check, literal members (field values, array element values, in
      // collection, etc) of newly shared instances would get TCObjects and ObjectIDs assigned to them.
      if (LiteralValues.isLiteralInstance(object)) { return false; }

      final TCObject tco = basicLookup(object);
      if (tco == null) { return true; }
      return tco.isNew();
    }

    @Override
    public void checkPortability(final TraversedReference reference, final Class referringClass)
        throws TCNonPortableObjectError {
      checkPortabilityOfTraversedReference(reference, referringClass);
      executePreCreateMethods(reference.getValue());
    }
  }

  private TCObject basicCreateIfNecessary(final Object pojo, final GroupID gid) {
    TCObject obj = null;

    if ((obj = basicLookup(pojo)) == null) {
      obj = this.factory.getNewInstance(nextObjectID(this.clientTxManager.getCurrentTransaction(), pojo, gid), pojo,
                                        pojo.getClass(), true);
      this.clientTxManager.createObject(obj);
      basicAddLocal(obj);
    }
    return obj;
  }

  private List basicCreateIfNecessary(final List pojos, final GroupID gid) {
    reserveObjectIds(pojos.size(), gid);

    final List tcObjects = new ArrayList(pojos.size());
    for (final Iterator i = pojos.iterator(); i.hasNext();) {
      tcObjects.add(basicCreateIfNecessary(i.next(), gid));
    }
    return tcObjects;
  }

  private void reserveObjectIds(final int size, final GroupID gid) {
    this.idProvider.reserve(size, gid);
  }

  private ObjectID nextObjectID(final ClientTransaction txn, final Object pojo, final GroupID gid) {
    return this.idProvider.next(txn, pojo, gid);
  }

  @Override
  public WeakReference createNewPeer(final TCClass clazz, final DNA dna) {
    return newWeakObjectReference(dna.getObjectID(), createNewPojoObject(clazz, dna));
  }

  private Object createNewPojoObject(TCClass clazz, DNA dna) {
    if (clazz.isUseNonDefaultConstructor()) {
      try {
        return this.factory.getNewPeerObject(clazz, dna);
      } catch (final IOException e) {
        throw new TCRuntimeException(e);
      } catch (final ClassNotFoundException e) {
        throw new TCRuntimeException(e);
      }
    } else {
      return createNewPojoObject(clazz, dna.getArraySize(), dna.getObjectID(), dna.getParentObjectID());
    }
  }

  @Override
  public WeakReference createNewPeer(final TCClass clazz, final int size, final ObjectID id, final ObjectID parentID) {
    return newWeakObjectReference(id, createNewPojoObject(clazz, size, id, parentID));
  }

  private Object createNewPojoObject(TCClass clazz, int size, ObjectID id, ObjectID parentID) {
    try {
      if (clazz.isIndexed()) {
        final Object array = this.factory.getNewArrayInstance(clazz, size);
        return array;
      } else if (parentID.isNull()) {
        return this.factory.getNewPeerObject(clazz);
      } else {
        return this.factory.getNewPeerObject(clazz, lookupObject(parentID));
      }
    } catch (final Exception e) {
      throw new TCRuntimeException(e);
    }
  }

  @Override
  public WeakReference newWeakObjectReference(final ObjectID oid, final Object referent) {
    return new WeakObjectReference(oid, referent, this.referenceQueue);
  }

  @Override
  public TCClass getOrCreateClass(final Class clazz) {
    return this.clazzFactory.getOrCreate(clazz, this);
  }

  @Override
  public boolean isPortableClass(final Class clazz) {
    return this.portability.isPortableClass(clazz);
  }

  @Override
  public boolean isPortableInstance(final Object obj) {
    return this.portability.isPortableInstance(obj);
  }

  private void startReaper() {
    this.reaper = new StoppableThread("Reaper") {
      @Override
      public void run() {
        while (true) {
          ObjectID objectID = null;
          try {
            if (isStopRequested()) { return; }

            final WeakObjectReference wor = (WeakObjectReference) ClientObjectManagerImpl.this.referenceQueue
                .remove(POLL_TIME);

            if (wor != null) {
              objectID = wor.getObjectID();
              reap(objectID);
            }
          } catch (final InterruptedException e) {
            return;
          } catch (final PlatformRejoinException e) {
            staticLogger.info("Ignoring " + PlatformRejoinException.class.getSimpleName() + " while reaping oid "
                              + objectID, e);
          }
        }
      }
    };
    this.reaper.setDaemon(true);
    this.reaper.start();
  }

  public void dumpToLogger() {
    final DumpLoggerWriter writer = new DumpLoggerWriter();
    final PrintWriter pw = new PrintWriter(writer);
    final PrettyPrinterImpl prettyPrinter = new PrettyPrinterImpl(pw);
    prettyPrinter.autoflush(false);
    prettyPrinter.visit(this);
    writer.flush();
  }

  @Override
  public synchronized PrettyPrinter prettyPrint(final PrettyPrinter out) {
    out.print(this.getClass().getName()).flush();
    out.indent().print("roots Map: ").print(Integer.valueOf(this.rootsHolder.size())).flush();
    out.indent().print("idToManaged size: ").print(Integer.valueOf(this.objectStore.size())).flush();
    out.indent().print("pojoToManaged size: ").print(Integer.valueOf(this.pojoToManaged.size())).flush();
    return out;
  }

  private static final class ObjectStore {

    private final ConcurrentHashMap objectStoreMap = new ConcurrentHashMap<ObjectID, TCObject>(10240, 0.75f, 128);
    private final TCObjectSelfStore tcObjectSelfStore;

    ObjectStore(TCObjectSelfStore tcObjectSelfStore) {
      this.tcObjectSelfStore = tcObjectSelfStore;
    }

    public void cleanup() {
      objectStoreMap.clear();
    }

    public void shutdown(boolean fromShutdownHook) {
      tcObjectSelfStore.shutdown(fromShutdownHook);
    }

    public int size() {
      return this.objectStoreMap.size();
    }

    public void add(final TCObject obj) {
      // Ignoring this currently as this is expected to be added in tc object self store
      if (obj instanceof TCObjectSelf) {
        this.tcObjectSelfStore.addTCObjectSelfTemp((TCObjectSelf) obj);
        return;
      }

      this.objectStoreMap.put(obj.getObjectID(), obj);
    }

    public TCObject get(final ObjectID id) {
      TCObject tc = (TCObject) this.objectStoreMap.get(id);
      if (tc == null) {
        tc = (TCObject) tcObjectSelfStore.getById(id);
      }
      return tc;
    }

    public Set addAllObjectIDs(final Set oids) {
      oids.addAll(this.objectStoreMap.keySet());
      this.tcObjectSelfStore.addAllObjectIDs(oids);
      return oids;
    }

    public void remove(final TCObject tcobj) {
      if (tcobj instanceof TCObjectSelf) { throw new AssertionError(
                                                                    "TCObjectSelf should not have called removed from here: "
                                                                        + tcobj); }
      this.objectStoreMap.remove(tcobj.getObjectID());
    }

    public boolean contains(final ObjectID objectID) {
      return this.objectStoreMap.containsKey(objectID) || this.tcObjectSelfStore.contains(objectID);
    }

  }

  private static class LocalLookupContext {
    private final Counter callStackCount      = new Counter(0);
    private final Counter objectCreationCount = new Counter(0);

    public Counter getCallStackCount() {
      return this.callStackCount;
    }

    public Counter getObjectCreationCount() {
      return this.objectCreationCount;
    }
  }

  private class ObjectLookupState {
    boolean                isSet = false;
    private final ObjectID objectID;
    private final Thread   owner;
    private TCObject       object;
    private final int      session;

    public ObjectLookupState(final ObjectID objectID) {
      this.objectID = objectID;
      this.owner = Thread.currentThread();
      this.session = currentSession;

    }

    public ObjectLookupState(final TCObject set) {
      this.objectID = set.getObjectID();
      this.object = set;
      this.isSet = true;
      this.owner = null;
      this.session = currentSession;
    }

    public ObjectID getObjectID() {
      return this.objectID;
    }

    public synchronized void setObject(final TCObject obj) {
      this.object = obj;
      this.isSet = true;
      notifyAll();
    }

    public Thread getOwner() {
      return owner;
    }

    public boolean isOwner() {
      return Thread.currentThread() == owner;
    }

    @Override
    public String toString() {
      return "ObjectLookupState [" + this.objectID + " , " + this.owner.getName() + ", " + this.isSet + " ]";
    }

    public synchronized TCObject waitForObject() throws AbortedOperationException {
      boolean isInterrupted = false;
      try {
        while (this.object == null && !isSet) {
          wait(CONCURRENT_LOOKUP_TIMED_WAIT);
        }
      } catch (InterruptedException ie) {
        AbortedOperationUtil.throwExceptionIfAborted(abortableOperationManager);
        isInterrupted = true;
      } finally {
        Util.selfInterruptIfNeeded(isInterrupted);
      }
      return this.object;
    }

    public int getSession() {
      return session;
    }
  }

  private interface PostCreateMethodGatherer {
    Map<Object, List<Method>> getPostCreateMethods();
  }

  @Override
  public void initializeTCClazzIfRequired(TCObjectSelf tcObjectSelf) {
    this.factory.initClazzIfRequired(tcObjectSelf.getClass(), tcObjectSelf);
  }

}
