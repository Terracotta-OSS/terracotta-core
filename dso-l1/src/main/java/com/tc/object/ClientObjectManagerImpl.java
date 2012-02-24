/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import com.google.common.collect.MapMaker;
import com.tc.exception.TCClassNotFoundException;
import com.tc.exception.TCNonPortableObjectError;
import com.tc.exception.TCNotRunningException;
import com.tc.exception.TCRuntimeException;
import com.tc.logging.ClientIDLogger;
import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.GroupID;
import com.tc.net.NodeID;
import com.tc.object.appevent.ApplicationEvent;
import com.tc.object.appevent.ApplicationEventContext;
import com.tc.object.appevent.NonPortableEventContext;
import com.tc.object.appevent.NonPortableEventContextFactory;
import com.tc.object.appevent.NonPortableFieldSetContext;
import com.tc.object.appevent.NonPortableObjectEvent;
import com.tc.object.appevent.NonPortableRootContext;
import com.tc.object.bytecode.Manageable;
import com.tc.object.bytecode.hook.impl.ArrayManager;
import com.tc.object.cache.CacheStats;
import com.tc.object.cache.ConcurrentClockEvictionPolicy;
import com.tc.object.cache.Evictable;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.dna.api.DNA;
import com.tc.object.handshakemanager.ClientHandshakeCallback;
import com.tc.object.idprovider.api.ObjectIDProvider;
import com.tc.object.loaders.ClassProvider;
import com.tc.object.loaders.Namespace;
import com.tc.object.logging.RuntimeLogger;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.object.msg.JMXMessage;
import com.tc.object.net.DSOClientMessageChannel;
import com.tc.object.tx.ClientTransaction;
import com.tc.object.tx.ClientTransactionManager;
import com.tc.object.util.ToggleableStrongReference;
import com.tc.object.walker.ObjectGraphWalker;
import com.tc.text.ConsoleNonPortableReasonFormatter;
import com.tc.text.ConsoleParagraphFormatter;
import com.tc.text.DumpLoggerWriter;
import com.tc.text.NonPortableReasonFormatter;
import com.tc.text.ParagraphFormatter;
import com.tc.text.PrettyPrintable;
import com.tc.text.PrettyPrinter;
import com.tc.text.PrettyPrinterImpl;
import com.tc.text.StringFormatter;
import com.tc.util.Assert;
import com.tc.util.Counter;
import com.tc.util.NonPortableReason;
import com.tc.util.State;
import com.tc.util.ToggleableReferenceManager;
import com.tc.util.Util;
import com.tc.util.VicariousThreadLocal;
import com.tc.util.concurrent.ResetableLatch;
import com.tc.util.concurrent.StoppableThread;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;

public class ClientObjectManagerImpl implements ClientObjectManager, ClientHandshakeCallback, PortableObjectProvider,
    Evictable, PrettyPrintable {

  private static final long                     CONCURRENT_LOOKUP_TIMED_WAIT = 1000;
  // REFERENCE_MAP_SEG must be power of 2
  private static final int                      REFERENCE_MAP_SEGS           = 32;
  private static final State                    PAUSED                       = new State("PAUSED");
  private static final State                    RUNNING                      = new State("RUNNING");
  private static final State                    STARTING                     = new State("STARTING");
  private static final State                    SHUTDOWN                     = new State("SHUTDOWN");

  private static final TCLogger                 staticLogger                 = TCLogging
                                                                                 .getLogger(ClientObjectManager.class);

  private static final long                     POLL_TIME                    = 1000;
  private static final long                     STOP_WAIT                    = POLL_TIME * 3;

  private static final int                      NO_DEPTH                     = 0;

  private static final int                      COMMIT_SIZE                  = 100;

  private State                                 state                        = RUNNING;
  private final Map                             roots                        = new HashMap();
  private final ConcurrentMap<Object, TCObject> pojoToManaged                = new MapMaker()
                                                                                 .concurrencyLevel(REFERENCE_MAP_SEGS)
                                                                                 .weakKeys().makeMap();

  private final ClassProvider                   classProvider;
  private final RemoteObjectManager             remoteObjectManager;
  private final Traverser                       traverser;
  private final TraverseTest                    traverseTest;
  private final DSOClientConfigHelper           clientConfiguration;
  private final TCClassFactory                  clazzFactory;
  private final Set                             rootLookupsInProgress        = new HashSet();
  private final ObjectIDProvider                idProvider;
  private final TCObjectFactory                 factory;
  private final ObjectStore                     objectStore;

  private ClientTransactionManager              txManager;

  private StoppableThread                       reaper                       = null;
  private final TCLogger                        logger;
  private final RuntimeLogger                   runtimeLogger;
  private final NonPortableEventContextFactory  appEventContextFactory;

  private final Portability                     portability;
  private final DSOClientMessageChannel         channel;
  private final ToggleableReferenceManager      referenceManager;
  private final ReferenceQueue                  referenceQueue               = new ReferenceQueue();

  private final boolean                         sendErrors                   = System.getProperty("project.name") != null;

  private final Map                             objectLatchStateMap          = new HashMap();
  private final ThreadLocal                     localLookupContext           = new VicariousThreadLocal() {

                                                                               @Override
                                                                               protected synchronized Object initialValue() {
                                                                                 return new LocalLookupContext();
                                                                               }

                                                                             };
  private final Semaphore                       creationSemaphore            = new Semaphore(1, true);

  public ClientObjectManagerImpl(final RemoteObjectManager remoteObjectManager,
                                 final DSOClientConfigHelper clientConfiguration, final ObjectIDProvider idProvider,
                                 final RuntimeLogger runtimeLogger, final ClientIDProvider provider,
                                 final ClassProvider classProvider, final TCClassFactory classFactory,
                                 final TCObjectFactory objectFactory, final Portability portability,
                                 final DSOClientMessageChannel channel,
                                 final ToggleableReferenceManager referenceManager, TCObjectSelfStore tcObjectSelfStore) {
    this.objectStore = new ObjectStore(tcObjectSelfStore);
    this.remoteObjectManager = remoteObjectManager;
    this.clientConfiguration = clientConfiguration;
    this.idProvider = idProvider;
    this.runtimeLogger = runtimeLogger;
    this.portability = portability;
    this.channel = channel;
    this.referenceManager = referenceManager;
    this.logger = new ClientIDLogger(provider, TCLogging.getLogger(ClientObjectManager.class));
    this.classProvider = classProvider;
    this.traverseTest = new NewObjectTraverseTest();
    this.traverser = new Traverser(this);
    this.clazzFactory = classFactory;
    this.factory = objectFactory;
    this.factory.setObjectManager(this);
    this.appEventContextFactory = new NonPortableEventContextFactory(provider);

    startReaper();
    ensureKeyClassesLoaded();
  }

  private void ensureKeyClassesLoaded() {
    // load LocalLookupContext early to avoid ClassCircularityError: DEV-1386
    new LocalLookupContext();

    /*
     * Exercise isManaged path early to preload classes and avoid ClassCircularityError during any subsequent calls
     */
    isManaged(new Object());
  }

  public Class getClassFor(final String className) throws ClassNotFoundException {
    return this.classProvider.getClassFor(className);
  }

  public synchronized boolean isLocal(final ObjectID objectID) {
    if (null == objectID) { return false; }

    if (this.objectStore.contains(objectID)) { return true; }

    return this.remoteObjectManager.isInDNACache(objectID);
  }

  public synchronized void pause(final NodeID remote, final int disconnected) {
    assertNotPaused("Attempt to pause while PAUSED");
    this.state = PAUSED;
    notifyAll();
  }

  public synchronized void unpause(final NodeID remote, final int disconnected) {
    assertNotRunning("Attempt to unpause while RUNNING");
    this.state = RUNNING;
    notifyAll();
  }

  public synchronized void initializeHandshake(final NodeID thisNode, final NodeID remoteNode,
                                               final ClientHandshakeMessage handshakeMessage) {
    if (isShutdown()) return;
    assertPaused("Attempt to initiateHandshake while not PAUSED. " + thisNode + " <--> " + remoteNode);
    changeStateToStarting();
    addAllObjectIDs(handshakeMessage.getObjectIDs(), remoteNode);

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

  private void assertPaused(final Object message) {
    if (this.state != PAUSED) { throw new AssertionError(message + ": " + this.state); }
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

  public TraversedReferences getPortableObjects(final Class clazz, final Object start, final TraversedReferences addTo) {
    final TCClass tcc = this.clazzFactory.getOrCreate(clazz, this);
    return tcc.getPortableObjects(start, addTo);
  }

  public void setTransactionManager(final ClientTransactionManager txManager) {
    this.txManager = txManager;
  }

  public ClientTransactionManager getTransactionManager() {
    return this.txManager;
  }

  private LocalLookupContext getLocalLookupContext() {
    return (LocalLookupContext) this.localLookupContext.get();
  }

  private ObjectLatchState getObjectLatchState(final ObjectID id) {
    return (ObjectLatchState) this.objectLatchStateMap.get(id);
  }

  private ObjectLatchState markLookupInProgress(final ObjectID id) {
    final ResetableLatch latch = getLocalLookupContext().getLatch();
    final ObjectLatchState ols = new ObjectLatchState(id, latch);
    final Object old = this.objectLatchStateMap.put(id, ols);
    Assert.assertNull(old);
    return ols;
  }

  private synchronized void markCreateInProgress(final ObjectLatchState ols, final TCObject object,
                                                 final LocalLookupContext lookupContext) {
    final ResetableLatch latch = lookupContext.getLatch();
    // Make sure this thread owns this object lookup
    Assert.assertTrue(ols.getLatch() == latch);
    ols.setObject(object);
    ols.markCreateState();
    lookupContext.getObjectCreationCount().increment();
  }

  private synchronized void lookupDone(final ObjectID id, final boolean decrementCount) {
    this.objectLatchStateMap.remove(id);
    if (decrementCount) {
      getLocalLookupContext().getObjectCreationCount().decrement();
    }
  }

  // For testing purposes
  protected Map getObjectLatchStateMap() {
    return this.objectLatchStateMap;
  }

  private TCObject create(final Object pojo, final NonPortableEventContext context, final GroupID gid) {
    traverse(pojo, context, new AddManagedObjectAction(), gid);
    return basicLookup(pojo);
  }

  public void shutdown() {
    this.objectStore.shutdown();
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

  public TCObject lookupOrCreate(final Object pojo) {
    if (pojo == null) { return TCObjectFactory.NULL_TC_OBJECT; }
    return lookupOrCreateIfNecesary(pojo, this.appEventContextFactory.createNonPortableEventContext(pojo),
                                    GroupID.NULL_ID);
  }

  public TCObject lookupOrCreate(final Object pojo, final GroupID gid) {
    if (pojo == null) { return TCObjectFactory.NULL_TC_OBJECT; }
    return lookupOrCreateIfNecesary(pojo, this.appEventContextFactory.createNonPortableEventContext(pojo), gid);
  }

  private TCObject lookupOrCreate(final Object pojo, final NonPortableEventContext context) {
    if (pojo == null) { return TCObjectFactory.NULL_TC_OBJECT; }
    return lookupOrCreateIfNecesary(pojo, context, GroupID.NULL_ID);
  }

  private TCObject lookupOrCreateIfNecesary(final Object pojo, final NonPortableEventContext context, final GroupID gid) {
    Assert.assertNotNull(pojo);
    TCObject obj = basicLookup(pojo);
    if (obj == null || obj.isNew()) {
      executePreCreateMethods(pojo);
      obj = create(pojo, context, gid);
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

  private TCObject lookupExistingLiteralRootOrNull(final String rootName) {
    final ObjectID rootID = (ObjectID) this.roots.get(rootName);
    return basicLookupByID(rootID);
  }

  public TCObject lookupExistingOrNull(final Object pojo) {
    return basicLookup(pojo);
  }

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
   */
  public void preFetchObject(final ObjectID id) {
    if (id.isNull()) return;

    synchronized (this) {
      if (basicHasLocal(id) || getObjectLatchState(id) != null) { return; }
      // We are temporarily marking lookup in progress so that no other thread sneaks in under us and does a lookup
      // while we are calling prefetch
      markLookupInProgress(id);
    }
    this.remoteObjectManager.preFetchObject(id);
    synchronized (this) {
      lookupDone(id, false);
      notifyAll();
    }
  }

  public Object lookupObjectQuiet(ObjectID id) throws ClassNotFoundException {
    return lookupObject(id, null, false, true);
  }

  public Object lookupObjectNoDepth(final ObjectID id) throws ClassNotFoundException {
    return lookupObject(id, null, true, false);
  }

  public Object lookupObject(final ObjectID objectID) throws ClassNotFoundException {
    return lookupObject(objectID, null, false, false);
  }

  public Object lookupObject(final ObjectID id, final ObjectID parentContext) throws ClassNotFoundException {
    return lookupObject(id, parentContext, false, false);
  }

  private Object lookupObject(final ObjectID objectID, final ObjectID parentContext, final boolean noDepth,
                              final boolean quiet) throws ClassNotFoundException {
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

  public boolean isManaged(final Object pojo) {
    return pojo != null && !LiteralValues.isLiteral(pojo.getClass().getName()) && lookupExistingOrNull(pojo) != null;
  }

  public boolean isCreationInProgress() {
    return getLocalLookupContext().getObjectCreationCount().get() > 0 ? true : false;
  }

  // Done

  public TCObject lookup(final ObjectID id) throws ClassNotFoundException {
    return lookup(id, null, false, false);
  }

  private TCObject lookup(final ObjectID id, final ObjectID parentContext, final boolean noDepth, final boolean quiet)
      throws ClassNotFoundException {
    TCObject obj = null;
    boolean retrieveNeeded = false;
    boolean isInterrupted = false;

    final LocalLookupContext lookupContext = getLocalLookupContext();

    if (lookupContext.getCallStackCount().increment() == 1) {
      // first time
      this.txManager.disableTransactionLogging();
      lookupContext.getLatch().reset();
    }

    try {
      ObjectLatchState ols;
      try {
        synchronized (this) {
          while (true) {
            obj = basicLookupByID(id);
            if (obj != null) {
              // object exists in local cache
              return obj;
            }
            ols = getObjectLatchState(id);
            if (ols != null && ols.isCreateState()) {
              // if the object is being created, add to the wait set and return the object
              lookupContext.getObjectLatchWaitSet().add(ols);
              return ols.getObject();
            } else if (ols != null && ols.isLookupState()) {
              // the object is being looked up, wait.
              try {
                wait(CONCURRENT_LOOKUP_TIMED_WAIT); // using a timed out to avoid needing to catch all notify conditions
              } catch (final InterruptedException ie) {
                isInterrupted = true;
              }
            } else {
              // otherwise, we need to lookup the object
              retrieveNeeded = true;
              ols = markLookupInProgress(id);
              break;
            }
          }
        }
      } finally {
        Util.selfInterruptIfNeeded(isInterrupted);
      }

      // retrieving object required, first looking up the DNA from the remote server, and creating
      // a pre-init TCObject, then hydrating the object
      if (retrieveNeeded) {
        boolean createInProgressSet = false;
        try {
          final DNA dna = noDepth ? this.remoteObjectManager.retrieve(id, NO_DEPTH)
              : (parentContext == null ? this.remoteObjectManager.retrieve(id) : this.remoteObjectManager
                  .retrieveWithParentContext(id, parentContext));
          Class clazz = this.classProvider.getClassFor(Namespace.parseClassNameIfNecessary(dna.getTypeName()));
          TCClass tcClazz = clazzFactory.getOrCreate(clazz, this);
          Object pojo = createNewPojoObject(tcClazz, dna);
          obj = this.factory.getNewInstance(id, pojo, clazz, false);

          // object is retrieved, now you want to make this as Creation in progress
          markCreateInProgress(ols, obj, lookupContext);
          createInProgressSet = true;

          Assert.assertFalse(dna.isDelta());
          // now hydrate the object, this could call resolveReferences which would call this method recursively
          if (obj instanceof TCObjectSelf) {
            obj.hydrate(dna, false, null);
          } else {
            obj.hydrate(dna, false, newWeakObjectReference(id, pojo));
          }
          if (this.runtimeLogger.getFaultDebug()) {
            this.runtimeLogger.updateFaultStats(dna.getTypeName());
          }
        } catch (final Throwable t) {
          if (!quiet) {
            logger.warn("Exception retrieving object " + id, t);
          }
          // remove the object creating in progress from the list.
          lookupDone(id, createInProgressSet);
          this.remoteObjectManager.removed(id);
          if (t instanceof ClassNotFoundException) { throw (ClassNotFoundException) t; }
          if (t instanceof RuntimeException) { throw (RuntimeException) t; }
          throw new RuntimeException(t);
        }
        basicAddLocal(obj, true);
      }
    } finally {
      if (lookupContext.getCallStackCount().decrement() == 0) {
        // release your own local latch
        lookupContext.getLatch().release();
        final Set waitSet = lookupContext.getObjectLatchWaitSet();
        waitAndClearLatchSet(waitSet);
        // enabled transaction logging
        this.txManager.enableTransactionLogging();
      }
    }
    return obj;

  }

  public void removedTCObjectSelfFromStore(TCObjectSelf tcoSelf) {
    synchronized (this) {
      // Calling remove from within the synchronized block to make sure there are no races between the lookups and
      // remove.
      if (logger.isDebugEnabled()) {
        logger.debug("XXX Removing TCObjectSelf from L1 with ObjectID=" + tcoSelf.getObjectID());
      }

      this.remoteObjectManager.removed(tcoSelf.getObjectID());
    }
    if (ClientObjectManagerImpl.this.runtimeLogger.getFlushDebug()) {
      this.runtimeLogger.updateFlushStats(tcoSelf.getClass().getName());
    }
  }

  private void waitAndClearLatchSet(final Set waitSet) {
    boolean isInterrupted = false;
    // now wait till all the other objects you are waiting for releases there latch.
    for (final Iterator iter = waitSet.iterator(); iter.hasNext();) {
      final ObjectLatchState ols = (ObjectLatchState) iter.next();
      while (true) {
        try {
          ols.getLatch().acquire();
          break;
        } catch (final InterruptedException e) {
          isInterrupted = true;
        }
      }

    }
    Util.selfInterruptIfNeeded(isInterrupted);
    waitSet.clear();
  }

  public synchronized TCObject lookupIfLocal(final ObjectID id) {
    return basicLookupByID(id);
  }

  protected synchronized Set addAllObjectIDs(final Set oids, final NodeID remoteNode) {
    return this.objectStore.addAllObjectIDs(oids);
  }

  public Object lookupRoot(final String rootName) {
    try {
      return lookupRootOptionallyCreateOrReplace(rootName, null, false, true, false);
    } catch (final ClassNotFoundException e) {
      throw new TCClassNotFoundException(e);
    }
  }

  /**
   * Check to see if the root is already in existence on the server. If it is then get it if not then create it.
   */
  public Object lookupOrCreateRoot(final String rootName, final Object root) {
    try {
      return lookupOrCreateRoot(rootName, root, true, false);
    } catch (final ClassNotFoundException e) {
      throw new TCClassNotFoundException(e);
    }
  }

  /**
   * This method must be called within a DSO synchronized context. Currently, this is called in a setter method of a
   * replaceable root.
   */
  public Object createOrReplaceRoot(final String rootName, final Object root) {
    final Object existingRoot = lookupRoot(rootName);
    if (existingRoot == null) {
      return lookupOrCreateRoot(rootName, root, false);
    } else if (isLiteralPojo(root)) {
      final TCObject tcObject = lookupExistingLiteralRootOrNull(rootName);
      tcObject.literalValueChanged(root, existingRoot);
      return root;
    } else {
      return lookupOrCreateRoot(rootName, root, false);
    }
  }

  public Object lookupOrCreateRootNoDepth(final String rootName, final Object root) {
    try {
      return lookupOrCreateRoot(rootName, root, true, true);
    } catch (final ClassNotFoundException e) {
      throw new TCClassNotFoundException(e);
    }
  }

  public Object lookupOrCreateRoot(final String rootName, final Object root, final boolean dsoFinal) {
    try {
      return lookupOrCreateRoot(rootName, root, dsoFinal, false);
    } catch (final ClassNotFoundException e) {
      throw new TCClassNotFoundException(e);
    }
  }

  private boolean isLiteralPojo(final Object pojo) {
    return !(pojo instanceof Class) && LiteralValues.isLiteralInstance(pojo);
  }

  private Object lookupOrCreateRoot(final String rootName, final Object root, final boolean dsoFinal,
                                    final boolean noDepth) throws ClassNotFoundException {
    if (root != null) {
      // this will throw an exception if root is not portable
      checkPortabilityOfRoot(root, rootName, root.getClass());
    }

    return lookupRootOptionallyCreateOrReplace(rootName, root, true, dsoFinal, noDepth);
  }

  private void checkPortabilityOfTraversedReference(final TraversedReference reference, final Class referringClass,
                                                    final NonPortableEventContext context) {
    final NonPortableReason reason = checkPortabilityOf(reference.getValue());
    if (reason != null) {
      reason.addDetail("Referring class", referringClass.getName());
      if (!reference.isAnonymous()) {
        final String fullyQualifiedFieldname = reference.getFullyQualifiedReferenceName();
        reason.setUltimateNonPortableFieldName(fullyQualifiedFieldname);
        reason.addDetail(NonPortableFieldSetContext.FIELD_NAME_LABEL, fullyQualifiedFieldname);
      }
      dumpObjectHierarchy(context.getPojo(), context);
      if (this.sendErrors) {
        storeObjectHierarchy(context.getPojo(), context);
      }
      throwNonPortableException(context.getPojo(), reason, context,
                                "Attempt to share an instance of a non-portable class referenced by a portable class.");
    }
  }

  private void checkPortabilityOfRoot(final Object root, final String rootName, final Class rootType)
      throws TCNonPortableObjectError {
    final NonPortableReason reason = checkPortabilityOf(root);
    if (reason != null) {
      final NonPortableRootContext context = this.appEventContextFactory.createNonPortableRootContext(rootName, root);
      dumpObjectHierarchy(root, context);
      if (this.sendErrors) {
        storeObjectHierarchy(root, context);
      }
      throwNonPortableException(root, reason, context,
                                "Attempt to share an instance of a non-portable class by assigning it to a root.");
    }
  }

  public void checkPortabilityOfField(final Object fieldValue, final String fieldName, final Object pojo)
      throws TCNonPortableObjectError {
    final NonPortableReason reason = checkPortabilityOf(fieldValue);
    if (reason != null) {
      final NonPortableFieldSetContext context = this.appEventContextFactory
          .createNonPortableFieldSetContext(pojo, fieldName, fieldValue);
      dumpObjectHierarchy(fieldValue, context);
      if (this.sendErrors) {
        storeObjectHierarchy(pojo, context);
      }
      throwNonPortableException(pojo, reason, context,
                                "Attempt to set the field of a shared object to an instance of a non-portable class.");
    }
  }

  /**
   * This is used by the senders of ApplicationEvents to provide a version of a logically-managed pojo in the state it
   * would have been in had the ApplicationEvent not occurred.
   */
  public Object cloneAndInvokeLogicalOperation(Object pojo, String methodName, final Object[] params) {
    try {
      final Class c = pojo.getClass();
      final Object o = c.newInstance();
      if (o instanceof Map) {
        ((Map) o).putAll((Map) pojo);
      } else if (o instanceof Collection) {
        ((Collection) o).addAll((Collection) pojo);
      }
      final Method[] methods = c.getMethods();
      methodName = methodName.substring(0, methodName.indexOf('('));
      for (Method m : methods) {
        final Class[] paramTypes = m.getParameterTypes();
        if (m.getName().equals(methodName) && params.length == paramTypes.length) {
          for (int j = 0; j < paramTypes.length; j++) {
            if (!paramTypes[j].isAssignableFrom(params[j].getClass())) {
              m = null;
              break;
            }
          }
          if (m != null) {
            m.invoke(o, params);
            break;
          }
        }
      }
      pojo = o;
    } catch (final Exception e) {
      this.logger.error("Unable to clone logical object", e);
    }
    return pojo;
  }

  public void checkPortabilityOfLogicalAction(final Object[] params, final int index, final String methodName,
                                              final Object pojo) throws TCNonPortableObjectError {
    final Object param = params[index];
    final NonPortableReason reason = checkPortabilityOf(param);
    if (reason != null) {
      final NonPortableEventContext context = this.appEventContextFactory
          .createNonPortableLogicalInvokeContext(pojo, methodName, params, index);
      dumpObjectHierarchy(params[index], context);
      if (this.sendErrors) {
        storeObjectHierarchy(cloneAndInvokeLogicalOperation(pojo, methodName, params), context);
      }
      throwNonPortableException(pojo, reason, context,
                                "Attempt to share an instance of a non-portable class by"
                                    + " passing it as an argument to a method of a logically-managed class.");
    }
  }

  private void throwNonPortableException(final Object obj, final NonPortableReason reason,
                                         final NonPortableEventContext context, final String message)
      throws TCNonPortableObjectError {
    // XXX: The message should probably be part of the context
    reason.setMessage(message);
    context.addDetailsTo(reason);

    // Send this event to L2
    final JMXMessage jmxMsg = this.channel.getJMXMessage();
    jmxMsg.setJMXObject(new NonPortableObjectEvent(context, reason));
    jmxMsg.send();

    final StringWriter formattedReason = new StringWriter();
    final PrintWriter out = new PrintWriter(formattedReason);
    final StringFormatter sf = new StringFormatter();

    final ParagraphFormatter pf = new ConsoleParagraphFormatter(80, sf);
    final NonPortableReasonFormatter reasonFormatter = new ConsoleNonPortableReasonFormatter(out, ": ", sf, pf);
    reason.accept(reasonFormatter);
    reasonFormatter.flush();

    throw new TCNonPortableObjectError(formattedReason.getBuffer().toString());
  }

  private NonPortableReason checkPortabilityOf(final Object obj) {
    if (!isPortableInstance(obj)) { return this.portability.getNonPortableReason(obj.getClass()); }
    return null;
  }

  private boolean rootLookupInProgress(final String rootName) {
    return this.rootLookupsInProgress.contains(rootName);
  }

  private void markRootLookupInProgress(final String rootName) {
    final boolean wasAdded = this.rootLookupsInProgress.add(rootName);
    if (!wasAdded) { throw new AssertionError("Attempt to mark a root lookup that is already in progress."); }
  }

  private void markRootLookupNotInProgress(final String rootName) {
    final boolean removed = this.rootLookupsInProgress.remove(rootName);
    if (!removed) { throw new AssertionError("Attempt to unmark a root lookup that wasn't in progress."); }
  }

  public synchronized void replaceRootIDIfNecessary(final String rootName, final ObjectID newRootID) {
    waitUntilRunning();

    final ObjectID oldRootID = (ObjectID) this.roots.get(rootName);
    if (oldRootID == null || oldRootID.equals(newRootID)) { return; }

    this.roots.put(rootName, newRootID);
  }

  private Object lookupRootOptionallyCreateOrReplace(final String rootName, final Object rootPojo,
                                                     final boolean create, final boolean dsoFinal, final boolean noDepth)
      throws ClassNotFoundException {
    final boolean replaceRootIfExistWhenCreate = !dsoFinal && create;

    ObjectID rootID = null;

    boolean retrieveNeeded = false;
    boolean isNew = false;
    boolean lookupInProgress = false;
    boolean isInterrupted = false;

    try {
      synchronized (this) {
        while (true) {
          if (!replaceRootIfExistWhenCreate) {
            rootID = (ObjectID) this.roots.get(rootName);
            if (rootID != null) {
              break;
            }
          } else {
            rootID = ObjectID.NULL_ID;
          }
          if (!rootLookupInProgress(rootName)) {
            lookupInProgress = true;
            markRootLookupInProgress(rootName);
            break;
          } else {
            try {
              wait();
            } catch (final InterruptedException e) {
              e.printStackTrace();
              isInterrupted = true;
            }
          }
        }
      }
    } finally {
      Util.selfInterruptIfNeeded(isInterrupted);
    }

    retrieveNeeded = lookupInProgress && !replaceRootIfExistWhenCreate;

    isNew = retrieveNeeded || (rootID.isNull() && create);

    if (retrieveNeeded) {
      rootID = this.remoteObjectManager.retrieveRootID(rootName);
    }

    if (rootID.isNull() && create) {
      Assert.assertNotNull(rootPojo);
      // TODO:: Optimize this, do lazy instantiation
      TCObject root = null;
      if (isLiteralPojo(rootPojo)) {
        root = basicCreate(rootPojo);
      } else {
        root = lookupOrCreate(rootPojo, this.appEventContextFactory.createNonPortableRootContext(rootName, rootPojo));
      }
      rootID = root.getObjectID();
      this.txManager.createRoot(rootName, rootID);
    }

    synchronized (this) {
      if (isNew && !rootID.isNull()) {
        this.roots.put(rootName, rootID);
      }
      if (lookupInProgress) {
        markRootLookupNotInProgress(rootName);
        notifyAll();
      }
    }

    return lookupObject(rootID, null, noDepth, false);
  }

  private TCObject basicCreate(final Object rootPojo) {
    reserveObjectIds(1, GroupID.NULL_ID);
    return basicCreateIfNecessary(rootPojo, GroupID.NULL_ID);
  }

  private TCObject basicLookupByID(final ObjectID id) {
    return this.objectStore.get(id);
  }

  private boolean basicHasLocal(final ObjectID id) {
    return basicLookupByID(id) != null;
  }

  private TCObject basicLookup(final Object obj) {
    if (obj == null) { return null; }
    if (obj instanceof TCObjectSelf) {
      TCObjectSelf self = (TCObjectSelf) obj;
      if (self.isInitialized()) { return self; }
    }

    if (obj instanceof Manageable) { return ((Manageable) obj).__tc_managed(); }

    return this.pojoToManaged.get(obj);
  }

  private void basicAddLocal(final TCObject obj, final boolean fromLookup) {
    synchronized (this) {
      final ObjectID id = obj.getObjectID();
      if (basicHasLocal(id)) { throw Assert.failure("Attempt to add an object that already exists: Object of class "
                                                    + obj.getClass() + " [Identity Hashcode : 0x"
                                                    + Integer.toHexString(System.identityHashCode(obj)) + "] "); }
      this.objectStore.add(obj);

      final Object pojo = obj.getPeerObject();

      if (pojo != null) {
        if (pojo.getClass().isArray()) {
          ArrayManager.register(pojo, obj);
        }

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
      lookupDone(id, fromLookup);
      notifyAll();
    }
  }

  private void traverse(final Object root, final NonPortableEventContext context, final TraversalAction action,
                        final GroupID gid) {
    // if set this will be final exception thrown
    Throwable exception = null;

    final PostCreateMethodGatherer postCreate = (PostCreateMethodGatherer) action;
    try {
      this.traverser.traverse(root, this.traverseTest, context, action, gid);
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

  private void dumpObjectHierarchy(final Object root, final NonPortableEventContext context) {
    // the catch is not in the called method so that when/if there is an OOME, the logging might have a chance of
    // actually working (as opposed to just throwing another OOME)
    try {
      dumpObjectHierarchy0(root, context);
    } catch (final Throwable t) {
      this.logger.error("error walking non-portable object instance of type " + root.getClass().getName(), t);
    }
  }

  private void dumpObjectHierarchy0(final Object root, final NonPortableEventContext context) {
    if (this.runtimeLogger.getNonPortableDump()) {
      final NonPortableWalkVisitor visitor = new NonPortableWalkVisitor(CustomerLogging.getDSORuntimeLogger(), this,
                                                                        this.clientConfiguration, root);
      final ObjectGraphWalker walker = new ObjectGraphWalker(root, visitor, visitor);
      walker.walk();
    }
  }

  public void sendApplicationEvent(final Object pojo, final ApplicationEvent event) {
    final JMXMessage jmxMsg = this.channel.getJMXMessage();
    storeObjectHierarchy(pojo, event.getApplicationEventContext());
    jmxMsg.setJMXObject(event);
    jmxMsg.send();
  }

  public void storeObjectHierarchy(final Object root, final ApplicationEventContext context) {
    try {
      final WalkVisitor wv = new WalkVisitor(this, this.clientConfiguration, context);
      final ObjectGraphWalker walker = new ObjectGraphWalker(root, wv, wv);
      walker.walk();
      context.setTreeModel(wv.getTreeModel());
    } catch (final Throwable t) {
      t.printStackTrace();
    }
  }

  public ToggleableStrongReference getOrCreateToggleRef(final ObjectID id, final Object peer) {
    // We don't need ObjectID param anymore, but it is useful when debugging so I didn't remove it
    return this.referenceManager.getOrCreateFor(peer);
  }

  private class AddManagedObjectAction implements TraversalAction, PostCreateMethodGatherer {
    private final Map<Object, List<Method>> toCall = new IdentityHashMap<Object, List<Method>>();

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
        ClientObjectManagerImpl.this.txManager.createObject((TCObject) i.next());
      }
    }

    public final Map<Object, List<Method>> getPostCreateMethods() {
      return this.toCall;
    }
  }

  private class NewObjectTraverseTest implements TraverseTest {

    public boolean shouldTraverse(final Object object) {
      // literals should be skipped -- without this check, literal members (field values, array element values, in
      // collection, etc) of newly shared instances would get TCObjects and ObjectIDs assigned to them.
      if (LiteralValues.isLiteralInstance(object)) { return false; }

      final TCObject tco = basicLookup(object);
      if (tco == null) { return true; }
      return tco.isNew();
    }

    public void checkPortability(final TraversedReference reference, final Class referringClass,
                                 final NonPortableEventContext context) throws TCNonPortableObjectError {
      checkPortabilityOfTraversedReference(reference, referringClass, context);
      executePreCreateMethods(reference.getValue());
    }
  }

  private TCObject basicCreateIfNecessary(final Object pojo, final GroupID gid) {
    TCObject obj = null;

    if ((obj = basicLookup(pojo)) == null) {
      obj = this.factory.getNewInstance(nextObjectID(this.txManager.getCurrentTransaction(), pojo, gid), pojo,
                                        pojo.getClass(), true);
      this.txManager.createObject(obj);
      basicAddLocal(obj, false);
      if (this.runtimeLogger.getNewManagedObjectDebug()) {
        this.runtimeLogger.newManagedObject(obj);
      }
    }
    return obj;
  }

  private List basicCreateIfNecessary(final List pojos, final GroupID gid) {
    canCreate();
    try {
      reserveObjectIds(pojos.size(), gid);

      synchronized (this) {
        waitUntilRunning();
        final List tcObjects = new ArrayList(pojos.size());
        for (final Iterator i = pojos.iterator(); i.hasNext();) {
          tcObjects.add(basicCreateIfNecessary(i.next(), gid));
        }
        return tcObjects;
      }
    } finally {
      allowCreation();
    }
  }

  private void allowCreation() {
    this.creationSemaphore.release();
  }

  private void canCreate() {
    this.creationSemaphore.acquireUninterruptibly();
  }

  private void reserveObjectIds(final int size, final GroupID gid) {
    this.idProvider.reserve(size, gid);
  }

  private ObjectID nextObjectID(final ClientTransaction txn, final Object pojo, final GroupID gid) {
    return this.idProvider.next(txn, pojo, gid);
  }

  public WeakReference createNewPeer(final TCClass clazz, final DNA dna) {
    return newWeakObjectReference(dna.getObjectID(), createNewPojoObject(clazz, dna));
  }

  private Object createNewPojoObject(TCClass clazz, DNA dna) {
    if (clazz.isUseNonDefaultConstructor()) {
      try {
        return this.factory.getNewPeerObject(clazz, dna);
      } catch (final Exception e) {
        throw new TCRuntimeException(e);
      }
    } else {
      return createNewPojoObject(clazz, dna.getArraySize(), dna.getObjectID(), dna.getParentObjectID());
    }
  }

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

  public WeakReference newWeakObjectReference(final ObjectID oid, final Object referent) {
    if (this.runtimeLogger.getFlushDebug()) {
      return new LoggingWeakObjectReference(oid, referent, this.referenceQueue);
    } else {
      return new WeakObjectReference(oid, referent, this.referenceQueue);
    }
  }

  public TCClass getOrCreateClass(final Class clazz) {
    return this.clazzFactory.getOrCreate(clazz, this);
  }

  public boolean isPortableClass(final Class clazz) {
    return this.portability.isPortableClass(clazz);
  }

  public boolean isPortableInstance(final Object obj) {
    return this.portability.isPortableInstance(obj);
  }

  private void startReaper() {
    this.reaper = new StoppableThread("Reaper") {
      @Override
      public void run() {
        while (true) {
          try {
            if (isStopRequested()) { return; }

            final WeakObjectReference wor = (WeakObjectReference) ClientObjectManagerImpl.this.referenceQueue
                .remove(POLL_TIME);

            if (wor != null) {
              final ObjectID objectID = wor.getObjectID();
              reap(objectID);
              if (ClientObjectManagerImpl.this.runtimeLogger.getFlushDebug()) {
                updateFlushStats(wor);
              }
            }
          } catch (final InterruptedException e) {
            return;
          }
        }
      }
    };
    this.reaper.setDaemon(true);
    this.reaper.start();
  }

  private void updateFlushStats(final WeakObjectReference wor) {
    String className = wor.getObjectType();
    if (className == null) {
      className = "UNKNOWN";
    }
    this.runtimeLogger.updateFlushStats(className);
  }

  // XXX::: Cache eviction doesn't clear it from the cache. it happens in reap().
  public void evictCache(final CacheStats stat) {
    final int size = objectStore_size();
    int toEvict = stat.getObjectCountToEvict(size);
    if (toEvict <= 0) { return; }
    // Cache is full
    final boolean debug = this.logger.isDebugEnabled();
    int totalReferencesCleared = 0;
    int toClear = toEvict;
    while (toEvict > 0 && toClear > 0) {
      final int maxCount = Math.min(COMMIT_SIZE, toClear);
      final Collection removalCandidates = this.objectStore.getRemovalCandidates(maxCount);
      if (removalCandidates.isEmpty()) {
        break; // couldnt find any more
      }
      for (final Iterator i = removalCandidates.iterator(); i.hasNext() && toClear > 0;) {
        final TCObject removed = (TCObject) i.next();
        if (removed != null) {
          final Object pr = removed.getPeerObject();
          if (pr != null) {
            // We don't want to take dso locks while clearing since it will happen inside the scope of the resolve lock
            // (see CDV-596)
            this.txManager.disableTransactionLogging();
            final int cleared;
            try {
              cleared = removed.clearReferences(toClear);
            } finally {
              this.txManager.enableTransactionLogging();
            }

            totalReferencesCleared += cleared;
            if (debug) {
              this.logger.debug("Clearing:" + removed.getObjectID() + " class:" + pr.getClass() + " Total cleared =  "
                                + totalReferencesCleared);
            }
            toClear -= cleared;
          }
        }
      }
      toEvict -= removalCandidates.size();
    }
    // TODO:: Send the correct set of targetObjects2GC
    stat.objectEvicted(totalReferencesCleared, objectStore_size(), Collections.EMPTY_LIST, false);
  }

  private int objectStore_size() {
    return this.objectStore.size();
  }

  public void dumpToLogger() {
    final DumpLoggerWriter writer = new DumpLoggerWriter();
    final PrintWriter pw = new PrintWriter(writer);
    final PrettyPrinterImpl prettyPrinter = new PrettyPrinterImpl(pw);
    prettyPrinter.autoflush(false);
    prettyPrinter.visit(this);
    writer.flush();
  }

  public synchronized PrettyPrinter prettyPrint(final PrettyPrinter out) {
    out.print(this.getClass().getName()).flush();
    out.indent().print("roots Map: ").print(Integer.valueOf(this.roots.size())).flush();
    out.indent().print("idToManaged size: ").print(Integer.valueOf(this.objectStore.size())).flush();
    out.indent().print("pojoToManaged size: ").print(Integer.valueOf(this.pojoToManaged.size())).flush();
    return out;
  }

  private static final class ObjectStore {

    private final ConcurrentHashMap             cacheManaged   = new ConcurrentHashMap<ObjectID, TCObject>(10240,
                                                                                                           0.75f, 128);
    private final ConcurrentHashMap             cacheUnmanaged = new ConcurrentHashMap<ObjectID, TCObject>(10240,
                                                                                                           0.75f, 128);
    private final ConcurrentClockEvictionPolicy cache;
    private final TCObjectSelfStore             tcObjectSelfStore;

    ObjectStore(TCObjectSelfStore tcObjectSelfStore) {
      this.tcObjectSelfStore = tcObjectSelfStore;
      this.cache = new ConcurrentClockEvictionPolicy(this.cacheManaged);
    }

    public void shutdown() {
      tcObjectSelfStore.shutdown();
    }

    public int size() {
      return this.cacheManaged.size() + this.cacheUnmanaged.size();
    }

    public Collection getRemovalCandidates(final int maxCount) {
      return this.cache.getRemovalCandidates(maxCount);
    }

    public void add(final TCObject obj) {
      // Ignoring this currently as this is expected to be added in tc object self store
      if (obj instanceof TCObjectSelf) {
        this.tcObjectSelfStore.addTCObjectSelfTemp((TCObjectSelf) obj);
        return;
      }

      if (obj.isCacheManaged()) {
        this.cache.add(obj);
      } else {
        this.cacheUnmanaged.put(obj.getObjectID(), obj);
      }
    }

    public TCObject get(final ObjectID id) {
      TCObject tc = (TCObject) this.cacheUnmanaged.get(id);
      if (tc == null) {
        tc = (TCObject) this.cacheManaged.get(id);
      }
      if (tc == null) {
        tc = (TCObject) tcObjectSelfStore.getById(id);
      }
      return tc;
    }

    public Set addAllObjectIDs(final Set oids) {
      oids.addAll(this.cacheManaged.keySet());
      oids.addAll(this.cacheUnmanaged.keySet());
      this.tcObjectSelfStore.addAllObjectIDs(oids);
      return oids;
    }

    public void remove(final TCObject tcobj) {
      if (tcobj instanceof TCObjectSelf) { throw new AssertionError(
                                                                    "TCObjectSelf should not have called removed from here: "
                                                                        + tcobj); }

      if (tcobj.isCacheManaged()) {
        this.cache.remove(tcobj);
      } else {
        this.cacheUnmanaged.remove(tcobj.getObjectID());
      }
    }

    public boolean contains(final ObjectID objectID) {
      return this.cacheUnmanaged.containsKey(objectID) || this.cacheManaged.containsKey(objectID)
             || this.tcObjectSelfStore.contains(objectID);
    }

  }

  private static class LocalLookupContext {
    private final ResetableLatch latch               = new ResetableLatch();
    private final Counter        callStackCount      = new Counter(0);
    private final Counter        objectCreationCount = new Counter(0);
    private final Set            objectLatchWaitSet  = new HashSet();

    public ResetableLatch getLatch() {
      return this.latch;
    }

    public Counter getCallStackCount() {
      return this.callStackCount;
    }

    public Counter getObjectCreationCount() {
      return this.objectCreationCount;
    }

    public Set getObjectLatchWaitSet() {
      return this.objectLatchWaitSet;
    }

  }

  private static class ObjectLatchState {

    private static final State   CREATE_STATE = new State("CREATE-STATE");

    private static final State   LOOKUP_STATE = new State("LOOKUP-STATE");

    private final ObjectID       objectID;

    private final ResetableLatch latch;

    private State                state        = LOOKUP_STATE;

    private TCObject             object;

    public ObjectLatchState(final ObjectID objectID, final ResetableLatch latch) {
      this.objectID = objectID;
      this.latch = latch;
    }

    public void setObject(final TCObject obj) {
      this.object = obj;
    }

    public ResetableLatch getLatch() {
      return this.latch;
    }

    public TCObject getObject() {
      return this.object;
    }

    public boolean isLookupState() {
      return LOOKUP_STATE.equals(this.state);
    }

    public boolean isCreateState() {
      return CREATE_STATE.equals(this.state);
    }

    public void markCreateState() {
      this.state = CREATE_STATE;
    }

    @Override
    public String toString() {
      return "ObjectLatchState [" + this.objectID + " , " + this.latch + ", " + this.state + " ]";
    }
  }

  private interface PostCreateMethodGatherer {
    Map<Object, List<Method>> getPostCreateMethods();
  }

  public void initializeTCClazzIfRequired(TCObjectSelf tcObjectSelf) {
    this.factory.initClazzIfRequired(tcObjectSelf.getClass(), tcObjectSelf);
  }
}
