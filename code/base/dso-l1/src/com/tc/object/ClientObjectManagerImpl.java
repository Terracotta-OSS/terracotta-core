/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import com.tc.exception.TCClassNotFoundException;
import com.tc.exception.TCNonPortableObjectError;
import com.tc.exception.TCRuntimeException;
import com.tc.logging.ClientIDLogger;
import com.tc.logging.CustomerLogging;
import com.tc.logging.DumpHandler;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.NodeID;
import com.tc.object.appevent.ApplicationEvent;
import com.tc.object.appevent.ApplicationEventContext;
import com.tc.object.appevent.NonPortableEventContext;
import com.tc.object.appevent.NonPortableEventContextFactory;
import com.tc.object.appevent.NonPortableFieldSetContext;
import com.tc.object.appevent.NonPortableObjectEvent;
import com.tc.object.appevent.NonPortableRootContext;
import com.tc.object.bytecode.Manageable;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.cache.CacheStats;
import com.tc.object.cache.Evictable;
import com.tc.object.cache.EvictionPolicy;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.dna.api.DNA;
import com.tc.object.handshakemanager.ClientHandshakeCallback;
import com.tc.object.idprovider.api.ObjectIDProvider;
import com.tc.object.loaders.ClassProvider;
import com.tc.object.loaders.LoaderDescription;
import com.tc.object.loaders.Namespace;
import com.tc.object.logging.RuntimeLogger;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.object.msg.JMXMessage;
import com.tc.object.net.DSOClientMessageChannel;
import com.tc.object.tx.ClientTransaction;
import com.tc.object.tx.ClientTransactionManager;
import com.tc.object.util.IdentityWeakHashMap;
import com.tc.object.util.ToggleableStrongReference;
import com.tc.object.walker.ObjectGraphWalker;
import com.tc.text.ConsoleNonPortableReasonFormatter;
import com.tc.text.ConsoleParagraphFormatter;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ClientObjectManagerImpl implements ClientObjectManager, ClientHandshakeCallback, PortableObjectProvider,
    Evictable, DumpHandler, PrettyPrintable {

  private static final long                    CONCURRENT_LOOKUP_TIMED_WAIT = 1000;
  private static final State                   PAUSED                       = new State("PAUSED");
  private static final State                   RUNNING                      = new State("RUNNING");

  private static final TCLogger                staticLogger                 = TCLogging
                                                                                .getLogger(ClientObjectManager.class);

  private static final long                    POLL_TIME                    = 1000;
  private static final long                    STOP_WAIT                    = POLL_TIME * 3;

  private static final int                     NO_DEPTH                     = 0;

  private static final int                     COMMIT_SIZE                  = 100;

  private State                                state                        = RUNNING;
  private final Object                         shutdownLock                 = new Object();
  private final Map                            roots                        = new HashMap();
  private final Map                            idToManaged                  = new HashMap();
  private final Map                            pojoToManaged                = new IdentityWeakHashMap();
  private final ClassProvider                  classProvider;
  private final RemoteObjectManager            remoteObjectManager;
  private final EvictionPolicy                 cache;
  private final Traverser                      traverser;
  private final Traverser                      shareObjectsTraverser;
  private final TraverseTest                   traverseTest;
  private final DSOClientConfigHelper          clientConfiguration;
  private final TCClassFactory                 clazzFactory;
  private final Set                            rootLookupsInProgress        = new HashSet();
  private final ObjectIDProvider               idProvider;
  private final TCObjectFactory                factory;

  private ClientTransactionManager             txManager;

  private StoppableThread                      reaper                       = null;
  private final TCLogger                       logger;
  private final RuntimeLogger                  runtimeLogger;
  private final NonPortableEventContextFactory appEventContextFactory;

  private final Collection                     pendingCreateTCObjects       = new ArrayList();
  private final Collection                     pendingCreatePojos           = new ArrayList();

  private final Portability                    portability;
  private final DSOClientMessageChannel        channel;
  private final ToggleableReferenceManager     referenceManager;
  private final ReferenceQueue                 referenceQueue               = new ReferenceQueue();

  private final boolean                        sendErrors                   = System.getProperty("project.name") != null;

  private final Map                            objectLatchStateMap          = new HashMap();
  private final ThreadLocal                    localLookupContext           = new ThreadLocal() {

                                                                              @Override
                                                                              protected synchronized Object initialValue() {
                                                                                return new LocalLookupContext();
                                                                              }

                                                                            };

  public ClientObjectManagerImpl(final RemoteObjectManager remoteObjectManager,
                                 final DSOClientConfigHelper clientConfiguration, final ObjectIDProvider idProvider,
                                 final EvictionPolicy cache, final RuntimeLogger runtimeLogger,
                                 final ClientIDProvider provider, final ClassProvider classProvider,
                                 final TCClassFactory classFactory, final TCObjectFactory objectFactory,
                                 final Portability portability, final DSOClientMessageChannel channel,
                                 final ToggleableReferenceManager referenceManager) {
    this.remoteObjectManager = remoteObjectManager;
    this.cache = cache;
    this.clientConfiguration = clientConfiguration;
    this.idProvider = idProvider;
    this.runtimeLogger = runtimeLogger;
    this.portability = portability;
    this.channel = channel;
    this.referenceManager = referenceManager;
    this.logger = new ClientIDLogger(provider, TCLogging.getLogger(ClientObjectManager.class));
    this.classProvider = classProvider;
    this.traverseTest = new NewObjectTraverseTest();
    this.traverser = new Traverser(new AddManagedObjectAction(), this);
    this.shareObjectsTraverser = new Traverser(new SharedObjectsAction(), this);
    this.clazzFactory = classFactory;
    this.factory = objectFactory;
    this.factory.setObjectManager(this);
    this.appEventContextFactory = new NonPortableEventContextFactory(provider);

    if (this.logger.isDebugEnabled()) {
      this.logger.debug("Starting up ClientObjectManager:" + System.identityHashCode(this) + ". Cache SIZE = "
                        + cache.getCacheCapacity());
    }
    startReaper();
    ensureLocalLookupContextLoaded();
  }

  private void ensureLocalLookupContextLoaded() {
    // load LocalLookupContext early to avoid ClassCircularityError: DEV-1386
    new LocalLookupContext();
  }

  public Class getClassFor(final String className, final LoaderDescription desc) throws ClassNotFoundException {
    return this.classProvider.getClassFor(className, desc);
  }

  public synchronized boolean isLocal(final ObjectID objectID) {
    if (null == objectID) { return false; }

    if (idToManaged.containsKey(objectID)) { return true; }

    return remoteObjectManager.isPrefetched(objectID);
  }

  public synchronized void pause(final NodeID remote, final int disconnected) {
    assertNotPaused("Attempt to pause while PAUSED");
    this.state = PAUSED;
    notifyAll();
  }

  public synchronized void unpause(final NodeID remote, final int disconnected) {
    assertPaused("Attempt to unpause while not PAUSED");
    this.state = RUNNING;
    notifyAll();
  }

  public synchronized void initializeHandshake(final NodeID thisNode, final NodeID remoteNode,
                                               final ClientHandshakeMessage handshakeMessage) {
    assertPaused("Attempt to initiateHandshake while not PAUSED");
    addAllObjectIDs(handshakeMessage.getObjectIDs());

    // Ignore objects reaped before handshaking otherwise those won't be in the list sent to L2 at handshaking.
    // Leave an inconsistent state between L1 and L2. Reaped object is in L1 removeObjects but L2 doesn't aware
    // and send objects over. This can happen when L2 restarted and other L1 makes object requests before this
    // L1's first object request to L2.
    this.remoteObjectManager.clear();
  }

  private void waitUntilRunning() {
    boolean isInterrupted = false;

    while (this.state != RUNNING) {
      try {
        wait();
      } catch (InterruptedException e) {
        isInterrupted = true;
      }
    }
    Util.selfInterruptIfNeeded(isInterrupted);
  }

  private void assertPaused(final Object message) {
    if (this.state != PAUSED) { throw new AssertionError(message + ": " + this.state); }
  }

  private void assertNotPaused(final Object message) {
    if (this.state == PAUSED) { throw new AssertionError(message + ": " + this.state); }
  }

  protected synchronized boolean isPaused() {
    return this.state == PAUSED;
  }

  public TraversedReferences getPortableObjects(final Class clazz, final Object start, final TraversedReferences addTo) {
    TCClass tcc = this.clazzFactory.getOrCreate(clazz, this);
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
    ResetableLatch latch = getLocalLookupContext().getLatch();
    ObjectLatchState ols = new ObjectLatchState(id, latch);
    Object old = this.objectLatchStateMap.put(id, ols);
    Assert.assertNull(old);
    return ols;
  }

  private synchronized void markCreateInProgress(final ObjectLatchState ols, final TCObject object,
                                                 final LocalLookupContext lookupContext) {
    ResetableLatch latch = lookupContext.getLatch();
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

  private TCObject create(final Object pojo, final NonPortableEventContext context) {
    addToManagedFromRoot(pojo, context);
    return basicLookup(pojo);
  }

  private TCObject share(final Object pojo, final NonPortableEventContext context) {
    addToSharedFromRoot(pojo, context);
    return basicLookup(pojo);
  }

  public void shutdown() {
    synchronized (this.shutdownLock) {
      if (this.reaper != null) {
        try {
          stopThread(this.reaper);
        } finally {
          this.reaper = null;
        }
      }
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
    return lookupOrCreateIfNecesary(pojo, this.appEventContextFactory.createNonPortableEventContext(pojo));
  }

  private TCObject lookupOrCreate(final Object pojo, final NonPortableEventContext context) {
    if (pojo == null) { return TCObjectFactory.NULL_TC_OBJECT; }
    return lookupOrCreateIfNecesary(pojo, context);
  }

  public TCObject lookupOrShare(final Object pojo) {
    if (pojo == null) { return TCObjectFactory.NULL_TC_OBJECT; }
    return lookupOrShareIfNecesary(pojo, this.appEventContextFactory.createNonPortableEventContext(pojo));
  }

  private TCObject lookupOrShareIfNecesary(final Object pojo, final NonPortableEventContext context) {
    Assert.assertNotNull(pojo);
    TCObject obj = basicLookup(pojo);
    if (obj == null || obj.isNew()) {
      obj = share(pojo, context);
    }
    return obj;
  }

  private TCObject lookupOrCreateIfNecesary(final Object pojo, final NonPortableEventContext context) {
    Assert.assertNotNull(pojo);
    TCObject obj = basicLookup(pojo);
    if (obj == null || obj.isNew()) {
      executePreCreateMethod(pojo);
      obj = create(pojo, context);
    }
    return obj;
  }

  private void executePreCreateMethod(final Object pojo) {
    String onLookupMethodName = this.clientConfiguration.getPreCreateMethodIfDefined(pojo.getClass().getName());
    if (onLookupMethodName != null) {
      executeMethod(pojo, onLookupMethodName, "preCreate method (" + onLookupMethodName + ") failed on object of "
                                              + pojo.getClass());
    }
  }

  /**
   * This method is created for situations in which a method needs to be taken place when an object moved from
   * non-shared to shared. The method could be an instrumented method. For instance, for ConcurrentHashMap, we need to
   * re-hash the objects already in the map because the hashing algorithm is different when a ConcurrentHashMap is
   * shared. The rehash method is an instrumented method. This should be executed only once.
   */
  private void executePostCreateMethod(final Object pojo) {
    String onLookupMethodName = this.clientConfiguration.getPostCreateMethodIfDefined(pojo.getClass().getName());
    if (onLookupMethodName != null) {
      executeMethod(pojo, onLookupMethodName, "postCreate method (" + onLookupMethodName + ") failed on object of "
                                              + pojo.getClass());
    }
  }

  private void executeMethod(final Object pojo, final String onLookupMethodName, final String loggingMessage) {
    // This method used to use beanshell, but I changed it to reflection to hopefully avoid a deadlock -- CDV-130

    try {
      Method m = pojo.getClass().getDeclaredMethod(onLookupMethodName, new Class[] {});
      m.setAccessible(true);
      m.invoke(pojo, new Object[] {});
    } catch (Throwable t) {
      if (t instanceof InvocationTargetException) {
        t = t.getCause();
      }
      this.logger.warn(loggingMessage, t);
      if (!(t instanceof RuntimeException)) {
        t = new RuntimeException(t);
      }
      throw (RuntimeException) t;
    }
  }

  private TCObject lookupExistingLiteralRootOrNull(final String rootName) {
    ObjectID rootID = (ObjectID) this.roots.get(rootName);
    return basicLookupByID(rootID);
  }

  public TCObject lookupExistingOrNull(final Object pojo) {
    return basicLookup(pojo);
  }

  public synchronized ObjectID lookupExistingObjectID(final Object pojo) {
    TCObject obj = basicLookup(pojo);
    if (obj == null) { throw new AssertionError("Missing object ID for:" + pojo); }
    return obj.getObjectID();
  }

  public void markReferenced(final TCObject tcobj) {
    this.cache.markReferenced(tcobj);
  }

  public Object lookupObjectNoDepth(final ObjectID id) throws ClassNotFoundException {
    return lookupObject(id, null, true);
  }

  public Object lookupObject(final ObjectID objectID) throws ClassNotFoundException {
    return lookupObject(objectID, null, false);
  }

  public Object lookupObject(final ObjectID id, final ObjectID parentContext) throws ClassNotFoundException {
    return lookupObject(id, parentContext, false);
  }

  private Object lookupObject(final ObjectID objectID, final ObjectID parentContext, final boolean noDepth)
      throws ClassNotFoundException {
    if (objectID.isNull()) { return null; }
    Object o = null;
    while (o == null) {
      final TCObject tco = lookup(objectID, parentContext, noDepth);
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
      TCObjectImpl tcobj = (TCObjectImpl) basicLookupByID(objectID);
      if (tcobj == null) {
        if (this.logger.isDebugEnabled()) {
          this.logger.debug(System.identityHashCode(this) + " Entry removed before reaper got the chance: " + objectID);
        }
      } else {
        if (tcobj.isNull()) {
          this.idToManaged.remove(objectID);
          this.cache.remove(tcobj);
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
    return lookup(id, null, false);
  }

  private TCObject lookup(final ObjectID id, final ObjectID parentContext, final boolean noDepth)
      throws ClassNotFoundException {
    TCObject obj = null;
    boolean retrieveNeeded = false;
    boolean isInterrupted = false;

    LocalLookupContext lookupContext = getLocalLookupContext();

    if (lookupContext.getCallStackCount().increment() == 1) {
      // first time
      this.txManager.disableTransactionLogging();
      lookupContext.getLatch().reset();
    }

    try {
      ObjectLatchState ols;
      synchronized (this) {
        while (true) {
          ols = getObjectLatchState(id);
          obj = basicLookupByID(id);
          if (obj != null) {
            // object exists in local cache
            return obj;
          } else if (ols != null && ols.isCreateState()) {
            // if the object is being created, add to the wait set and return the object
            lookupContext.getObjectLatchWaitSet().add(ols);
            return ols.getObject();
          } else if (ols != null && ols.isLookupState()) {
            // the object is being looked up, wait.
            try {
              wait(CONCURRENT_LOOKUP_TIMED_WAIT); // using a timed out to avoid needing to catch all notify conditions
            } catch (InterruptedException ie) {
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
      Util.selfInterruptIfNeeded(isInterrupted);

      // retrieving object required, first looking up the DNA from the remote server, and creating
      // a pre-init TCObject, then hydrating the object
      if (retrieveNeeded) {
        boolean createInProgressSet = false;
        try {
          DNA dna = noDepth ? this.remoteObjectManager.retrieve(id, NO_DEPTH)
              : (parentContext == null ? this.remoteObjectManager.retrieve(id) : this.remoteObjectManager
                  .retrieveWithParentContext(id, parentContext));
          // TODO: make DNA.getDefiningLoaderDescription() return LoaderDescription
          LoaderDescription desc = LoaderDescription.fromString(dna.getDefiningLoaderDescription());
          obj = this.factory.getNewInstance(id, this.classProvider.getClassFor(Namespace.parseClassNameIfNecessary(dna
              .getTypeName()), desc), false);

          // object is retrieved, now you want to make this as Creation in progress
          markCreateInProgress(ols, obj, lookupContext);
          createInProgressSet = true;

          Assert.assertFalse(dna.isDelta());
          // now hydrate the object, this could call resolveReferences which would call this method recursively
          obj.hydrate(dna, false);
          if (this.runtimeLogger.getFaultDebug()) {
            this.runtimeLogger.updateFaultStats(dna.getTypeName());
          }
        } catch (Throwable t) {
          // remove the object creating in progress from the list.
          lookupDone(id, createInProgressSet);
          this.logger.warn("Exception: ", t);
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
        Set waitSet = lookupContext.getObjectLatchWaitSet();
        waitAndClearLatchSet(waitSet);
        // enabled transaction logging
        this.txManager.enableTransactionLogging();
      }
    }
    return obj;

  }

  private void waitAndClearLatchSet(final Set waitSet) {
    boolean isInterrupted = false;
    // now wait till all the other objects you are waiting for releases there latch.
    for (Iterator iter = waitSet.iterator(); iter.hasNext();) {
      ObjectLatchState ols = (ObjectLatchState) iter.next();
      while (true) {
        try {
          ols.getLatch().acquire();
          break;
        } catch (InterruptedException e) {
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

  synchronized Set addAllObjectIDs(final Set oids) {
    for (Iterator i = this.idToManaged.keySet().iterator(); i.hasNext();) {
      oids.add(i.next());
    }
    return oids;
  }

  public Object lookupRoot(final String rootName) {
    try {
      return lookupRootOptionallyCreateOrReplace(rootName, null, false, true, false);
    } catch (ClassNotFoundException e) {
      throw new TCClassNotFoundException(e);
    }
  }

  /**
   * Check to see if the root is already in existence on the server. If it is then get it if not then create it.
   */
  public Object lookupOrCreateRoot(final String rootName, final Object root) {
    try {
      return lookupOrCreateRoot(rootName, root, true, false);
    } catch (ClassNotFoundException e) {
      throw new TCClassNotFoundException(e);
    }
  }

  /**
   * This method must be called within a DSO synchronized context. Currently, this is called in a setter method of a
   * replaceable root.
   */
  public Object createOrReplaceRoot(final String rootName, final Object root) {
    Object existingRoot = lookupRoot(rootName);
    if (existingRoot == null) {
      return lookupOrCreateRoot(rootName, root, false);
    } else if (isLiteralPojo(root)) {
      TCObject tcObject = lookupExistingLiteralRootOrNull(rootName);
      tcObject.literalValueChanged(root, existingRoot);
      return root;
    } else {
      return lookupOrCreateRoot(rootName, root, false);
    }
  }

  public Object lookupOrCreateRootNoDepth(final String rootName, final Object root) {
    try {
      return lookupOrCreateRoot(rootName, root, true, true);
    } catch (ClassNotFoundException e) {
      throw new TCClassNotFoundException(e);
    }
  }

  public Object lookupOrCreateRoot(final String rootName, final Object root, final boolean dsoFinal) {
    try {
      return lookupOrCreateRoot(rootName, root, dsoFinal, false);
    } catch (ClassNotFoundException e) {
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
      this.checkPortabilityOfRoot(root, rootName, root.getClass());
    }

    return lookupRootOptionallyCreateOrReplace(rootName, root, true, dsoFinal, noDepth);
  }

  private void checkPortabilityOfTraversedReference(final TraversedReference reference, final Class referringClass,
                                                    final NonPortableEventContext context) {
    NonPortableReason reason = checkPortabilityOf(reference.getValue());
    if (reason != null) {
      reason.addDetail("Referring class", referringClass.getName());
      if (!reference.isAnonymous()) {
        String fullyQualifiedFieldname = reference.getFullyQualifiedReferenceName();
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
    NonPortableReason reason = checkPortabilityOf(root);
    if (reason != null) {
      NonPortableRootContext context = this.appEventContextFactory.createNonPortableRootContext(rootName, root);
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
    NonPortableReason reason = checkPortabilityOf(fieldValue);
    if (reason != null) {
      NonPortableFieldSetContext context = this.appEventContextFactory.createNonPortableFieldSetContext(pojo,
                                                                                                        fieldName,
                                                                                                        fieldValue);
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
      Class c = pojo.getClass();
      Object o = c.newInstance();
      if (o instanceof Map) {
        ((Map) o).putAll((Map) pojo);
      } else if (o instanceof Collection) {
        ((Collection) o).addAll((Collection) pojo);
      }
      Method[] methods = c.getMethods();
      methodName = methodName.substring(0, methodName.indexOf('('));
      for (Method m : methods) {
        Class[] paramTypes = m.getParameterTypes();
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
    } catch (Exception e) {
      this.logger.error("Unable to clone logical object", e);
    }
    return pojo;
  }

  public void checkPortabilityOfLogicalAction(final Object[] params, final int index, final String methodName,
                                              final Object pojo) throws TCNonPortableObjectError {
    Object param = params[index];
    NonPortableReason reason = checkPortabilityOf(param);
    if (reason != null) {
      NonPortableEventContext context = this.appEventContextFactory
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
    JMXMessage jmxMsg = this.channel.getJMXMessage();
    jmxMsg.setJMXObject(new NonPortableObjectEvent(context, reason));
    jmxMsg.send();

    StringWriter formattedReason = new StringWriter();
    PrintWriter out = new PrintWriter(formattedReason);
    StringFormatter sf = new StringFormatter();

    ParagraphFormatter pf = new ConsoleParagraphFormatter(80, sf);
    NonPortableReasonFormatter reasonFormatter = new ConsoleNonPortableReasonFormatter(out, ": ", sf, pf);
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
    boolean wasAdded = this.rootLookupsInProgress.add(rootName);
    if (!wasAdded) { throw new AssertionError("Attempt to mark a root lookup that is already in progress."); }
  }

  private void markRootLookupNotInProgress(final String rootName) {
    boolean removed = this.rootLookupsInProgress.remove(rootName);
    if (!removed) { throw new AssertionError("Attempt to unmark a root lookup that wasn't in progress."); }
  }

  public synchronized void replaceRootIDIfNecessary(final String rootName, final ObjectID newRootID) {
    waitUntilRunning();

    ObjectID oldRootID = (ObjectID) this.roots.get(rootName);
    if (oldRootID == null || oldRootID.equals(newRootID)) { return; }

    this.roots.put(rootName, newRootID);
  }

  private Object lookupRootOptionallyCreateOrReplace(final String rootName, final Object rootPojo,
                                                     final boolean create, final boolean dsoFinal, final boolean noDepth)
      throws ClassNotFoundException {
    boolean replaceRootIfExistWhenCreate = !dsoFinal && create;

    ObjectID rootID = null;

    boolean retrieveNeeded = false;
    boolean isNew = false;
    boolean lookupInProgress = false;
    boolean isInterrupted = false;

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
          } catch (InterruptedException e) {
            e.printStackTrace();
            isInterrupted = true;
          }
        }
      }
    }
    Util.selfInterruptIfNeeded(isInterrupted);

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
        root = basicCreateIfNecessary(rootPojo);
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

    return lookupObject(rootID, null, noDepth);
  }

  private TCObject basicLookupByID(final ObjectID id) {
    return (TCObject) this.idToManaged.get(id);
  }

  private boolean basicHasLocal(final ObjectID id) {
    return basicLookupByID(id) != null;
  }

  private TCObject basicLookup(final Object obj) {
    TCObject tcobj;
    if (obj instanceof Manageable) {
      tcobj = ((Manageable) obj).__tc_managed();
    } else {
      synchronized (this.pojoToManaged) {
        tcobj = (TCObject) this.pojoToManaged.get(obj);
      }
    }
    return tcobj;
  }

  private void basicAddLocal(final TCObject obj, final boolean fromLookup) {
    synchronized (this) {
      ObjectID id = obj.getObjectID();
      if (basicHasLocal(id)) { throw Assert.failure("Attempt to add an object that already exists: " + obj); }
      this.idToManaged.put(id, obj);

      Object pojo = obj.getPeerObject();

      if (pojo != null) {
        if (pojo.getClass().isArray()) {
          ManagerUtil.register(pojo, obj);
        }

        synchronized (this.pojoToManaged) {
          if (pojo instanceof Manageable) {
            Manageable m = (Manageable) pojo;
            if (m.__tc_managed() == null) {
              m.__tc_managed(obj);
            } else {
              Assert.assertTrue(m.__tc_managed() == obj);
            }
          } else {
            if (!isLiteralPojo(pojo)) {
              this.pojoToManaged.put(obj.getPeerObject(), obj);
            }
          }
        }
      }
      this.cache.add(obj);
      lookupDone(id, fromLookup);
      notifyAll();
    }
  }

  private void addToManagedFromRoot(final Object root, final NonPortableEventContext context) {
    this.traverser.traverse(root, this.traverseTest, context);
  }

  private void dumpObjectHierarchy(final Object root, final NonPortableEventContext context) {
    // the catch is not in the called method so that when/if there is an OOME, the logging might have a chance of
    // actually working (as opposed to just throwing another OOME)
    try {
      dumpObjectHierarchy0(root, context);
    } catch (Throwable t) {
      this.logger.error("error walking non-portable object instance of type " + root.getClass().getName(), t);
    }
  }

  private void dumpObjectHierarchy0(final Object root, final NonPortableEventContext context) {
    if (this.runtimeLogger.getNonPortableDump()) {
      NonPortableWalkVisitor visitor = new NonPortableWalkVisitor(CustomerLogging.getDSORuntimeLogger(), this,
                                                                  this.clientConfiguration, root);
      ObjectGraphWalker walker = new ObjectGraphWalker(root, visitor, visitor);
      walker.walk();
    }
  }

  public void sendApplicationEvent(final Object pojo, final ApplicationEvent event) {
    JMXMessage jmxMsg = this.channel.getJMXMessage();
    storeObjectHierarchy(pojo, event.getApplicationEventContext());
    jmxMsg.setJMXObject(event);
    jmxMsg.send();
  }

  public void storeObjectHierarchy(final Object root, final ApplicationEventContext context) {
    try {
      WalkVisitor wv = new WalkVisitor(this, this.clientConfiguration, context);
      ObjectGraphWalker walker = new ObjectGraphWalker(root, wv, wv);
      walker.walk();
      context.setTreeModel(wv.getTreeModel());
    } catch (Throwable t) {
      t.printStackTrace();
    }
  }

  private void addToSharedFromRoot(final Object root, final NonPortableEventContext context) {
    this.shareObjectsTraverser.traverse(root, this.traverseTest, context);
  }

  public ToggleableStrongReference getOrCreateToggleRef(final ObjectID id, final Object peer) {
    // We don't need ObjectID param anymore, but it is useful when debugging so I didn't remove it
    return this.referenceManager.getOrCreateFor(peer);
  }

  private class AddManagedObjectAction implements TraversalAction {
    public void visit(final List objects) {
      List tcObjects = basicCreateIfNecessary(objects);
      for (Iterator i = tcObjects.iterator(); i.hasNext();) {
        ClientObjectManagerImpl.this.txManager.createObject((TCObject) i.next());
      }
    }
  }

  private class SharedObjectsAction implements TraversalAction {
    public void visit(final List objects) {
      basicShareObjectsIfNecessary(objects);
    }
  }

  private class NewObjectTraverseTest implements TraverseTest {

    public boolean shouldTraverse(final Object object) {
      // literals should be skipped -- without this check, literal members (field values, array element values, in
      // collection, etc) of newly shared instances would get TCObjects and ObjectIDs assigned to them.
      if (LiteralValues.isLiteralInstance(object)) { return false; }

      TCObject tco = basicLookup(object);
      if (tco == null) { return true; }
      return tco.isNew();
    }

    public void checkPortability(final TraversedReference reference, final Class referringClass,
                                 final NonPortableEventContext context) throws TCNonPortableObjectError {
      ClientObjectManagerImpl.this.checkPortabilityOfTraversedReference(reference, referringClass, context);
    }
  }

  private TCObject basicCreateIfNecessary(final Object pojo) {
    TCObject obj = null;

    if ((obj = basicLookup(pojo)) == null) {
      obj = this.factory.getNewInstance(nextObjectID(this.txManager.getCurrentTransaction(), pojo), pojo, pojo
          .getClass(), true);
      this.txManager.createObject(obj);
      basicAddLocal(obj, false);
      executePostCreateMethod(pojo);
      if (this.runtimeLogger.getNewManagedObjectDebug()) {
        this.runtimeLogger.newManagedObject(obj);
      }
    }
    return obj;
  }

  private synchronized List basicCreateIfNecessary(final List pojos) {
    waitUntilRunning();
    List tcObjects = new ArrayList(pojos.size());
    for (Iterator i = pojos.iterator(); i.hasNext();) {
      tcObjects.add(basicCreateIfNecessary(i.next()));
    }
    return tcObjects;
  }

  private TCObject basicShareObjectIfNecessary(final Object pojo) {
    TCObject obj = null;

    if ((obj = basicLookup(pojo)) == null) {
      obj = this.factory.getNewInstance(nextObjectID(this.txManager.getCurrentTransaction(), pojo), pojo, pojo
          .getClass(), true);
      this.pendingCreateTCObjects.add(obj);
      this.pendingCreatePojos.add(pojo);
      basicAddLocal(obj, false);
    }
    return obj;
  }

  private synchronized List basicShareObjectsIfNecessary(final List pojos) {
    waitUntilRunning();
    List tcObjects = new ArrayList(pojos.size());
    for (Iterator i = pojos.iterator(); i.hasNext();) {
      tcObjects.add(basicShareObjectIfNecessary(i.next()));
    }
    return tcObjects;
  }

  public synchronized void addPendingCreateObjectsToTransaction() {
    for (Iterator i = this.pendingCreateTCObjects.iterator(); i.hasNext();) {
      TCObject tcObject = (TCObject) i.next();
      this.txManager.createObject(tcObject);
    }
    this.pendingCreateTCObjects.clear();
    this.pendingCreatePojos.clear();
  }

  public synchronized boolean hasPendingCreateObjects() {
    return !this.pendingCreateTCObjects.isEmpty();
  }

  private ObjectID nextObjectID(final ClientTransaction txn, final Object pojo) {
    return this.idProvider.next(txn, pojo);
  }

  public WeakReference createNewPeer(final TCClass clazz, final DNA dna) {
    if (clazz.isUseNonDefaultConstructor()) {
      try {
        return newWeakObjectReference(dna.getObjectID(), this.factory.getNewPeerObject(clazz, dna));
      } catch (Exception e) {
        throw new TCRuntimeException(e);
      }
    } else {
      return createNewPeer(clazz, dna.getArraySize(), dna.getObjectID(), dna.getParentObjectID());
    }
  }

  public WeakReference createNewPeer(final TCClass clazz, final int size, final ObjectID id, final ObjectID parentID) {
    try {
      if (clazz.isIndexed()) {
        Object array = this.factory.getNewArrayInstance(clazz, size);
        return newWeakObjectReference(id, array);
      } else if (parentID.isNull()) {
        return newWeakObjectReference(id, this.factory.getNewPeerObject(clazz));
      } else {
        return newWeakObjectReference(id, this.factory.getNewPeerObject(clazz, lookupObject(parentID)));
      }
    } catch (Exception e) {
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

            WeakObjectReference wor = (WeakObjectReference) ClientObjectManagerImpl.this.referenceQueue
                .remove(POLL_TIME);

            if (wor != null) {
              ObjectID objectID = wor.getObjectID();
              reap(objectID);
              if (ClientObjectManagerImpl.this.runtimeLogger.getFlushDebug()) {
                updateFlushStats(wor);
              }
            }
          } catch (InterruptedException e) {
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

  // XXX::: Cache eviction doesnt clear it from the cache. it happens in reap().
  public void evictCache(final CacheStats stat) {
    int size = idToManaged_size();
    int toEvict = stat.getObjectCountToEvict(size);
    if (toEvict <= 0) { return; }
    // Cache is full
    boolean debug = this.logger.isDebugEnabled();
    int totalReferencesCleared = 0;
    int toClear = toEvict;
    while (toEvict > 0 && toClear > 0) {
      int maxCount = Math.min(COMMIT_SIZE, toClear);
      Collection removalCandidates = this.cache.getRemovalCandidates(maxCount);
      if (removalCandidates.isEmpty()) {
        break; // couldnt find any more
      }
      for (Iterator i = removalCandidates.iterator(); i.hasNext() && toClear > 0;) {
        TCObject removed = (TCObject) i.next();
        if (removed != null) {
          Object pr = removed.getPeerObject();
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
    stat.objectEvicted(totalReferencesCleared, idToManaged_size(), Collections.EMPTY_LIST);
  }

  // XXX:: Not synchronizing to improve performance, should be called only during cache eviction
  private int idToManaged_size() {
    return this.idToManaged.size();
  }

  public String dump() {
    StringWriter writer = new StringWriter();
    PrintWriter pw = new PrintWriter(writer);
    new PrettyPrinterImpl(pw).visit(this);
    writer.flush();
    return writer.toString();
  }

  public void dumpToLogger() {
    this.logger.info(dump());
  }

  public synchronized PrettyPrinter prettyPrint(final PrettyPrinter out) {
    out.println(getClass().getName());
    out.indent().print("roots Map: ").println(new Integer(this.roots.size()));
    out.indent().print("idToManaged size: ").println(new Integer(this.idToManaged.size()));
    out.indent().print("pojoToManaged size: ").println(new Integer(this.pojoToManaged.size()));
    return out;
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

    public ObjectID getObjectID() {
      return this.objectID;
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

}