/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.abortable.AbortableOperationManager;
import com.tc.abortable.AbortedOperationException;
import com.tc.cluster.DsoCluster;
import com.tc.exception.TCClassNotFoundException;
import com.tc.logging.TCLogger;
import com.tc.management.TunneledDomainUpdater;
import com.tc.net.GroupID;
import com.tc.object.ObjectID;
import com.tc.object.ServerEventDestination;
import com.tc.object.TCObject;
import com.tc.object.loaders.ClassProvider;
import com.tc.object.locks.LockID;
import com.tc.object.locks.LockLevel;
import com.tc.object.locks.TerracottaLocking;
import com.tc.object.metadata.MetaDataDescriptor;
import com.tc.object.tx.TransactionCompleteListener;
import com.tc.operatorevent.TerracottaOperatorEvent.EventSubsystem;
import com.tc.operatorevent.TerracottaOperatorEvent.EventType;
import com.tc.platform.PlatformService;
import com.tc.properties.TCProperties;
import com.tc.search.SearchQueryResults;
import com.tc.server.ServerEventType;
import com.tc.util.concurrent.TaskRunner;
import com.terracottatech.search.NVPair;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import javax.management.MBeanServer;

/**
 * The Manager interface
 */
public interface Manager extends TerracottaLocking {

  /** This class's class path: com/tc/object/bytecode/Manager */
  public static final String CLASS = "com/tc/object/bytecode/Manager";
  /** Bytecode type definition for this class */
  public static final String TYPE  = "L" + CLASS + ";";

  /**
   * Determine whether this class is physically instrumented
   * 
   * @param clazz Class
   * @return True if physically instrumented
   */
  public boolean isPhysicallyInstrumented(Class clazz);

  /**
   * Initialize the Manager
   */
  public void init();

  /**
   * Stop the manager
   */
  public void stop();

  /**
   * Look up or create a new root object
   * 
   * @param name Root name
   * @param object Root object to use if none exists yet
   * @return The root object actually used, may or may not == object
   * @throws AbortedOperationException
   */
  public Object lookupOrCreateRoot(String name, Object object);

  /**
   * Look up or create a new root object in the particular group
   * 
   * @param name Root name
   * @param object Root object to use if none exists yet
   * @param gid group id
   * @return The root object actually used, may or may not == object
   * @throws AbortedOperationException
   */
  public Object lookupOrCreateRoot(final String name, final Object object, GroupID gid);

  /**
   * Look up a new root object in the particular group
   * 
   * @param name Root name
   * @param gid group id
   * @return The root object actually used, may or may not == object
   * @throws AbortedOperationException
   */
  public Object lookupRoot(final String name, GroupID gid);

  /**
   * Look up or create a new root object. Objects faulted in to arbitrary depth.
   * 
   * @param name Root name
   * @param obj Root object to use if none exists yet
   * @return The root object actually used, may or may not == object
   * @throws AbortedOperationException
   */
  public Object lookupOrCreateRootNoDepth(String name, Object obj) throws AbortedOperationException;

  /**
   * Create or replace root, typically used for replaceable roots.
   * 
   * @param rootName Root name
   * @param object Root object
   * @return Root object used
   * @throws AbortedOperationException
   */
  public Object createOrReplaceRoot(String rootName, Object object) throws AbortedOperationException;

  /**
   * Look up object by ID, faulting into the JVM if necessary
   * 
   * @param id Object identifier
   * @return The actual object
   * @throws AbortedOperationException
   */
  public Object lookupObject(ObjectID id) throws ClassNotFoundException, AbortedOperationException;

  /**
   * Prefetch object by ID, faulting into the JVM if necessary, Async lookup and will not cause ObjectNotFoundException
   * like lookupObject. Non-existent objects are ignored by the server.
   * 
   * @param id Object identifier
   * @throws AbortedOperationException
   */
  public void preFetchObject(ObjectID id) throws AbortedOperationException;

  /**
   * Look up object by ID, faulting into the JVM if necessary, This method also passes the parent Object context so that
   * more intelligent prefetching is possible at the L2.
   * 
   * @param id Object identifier of the object we are looking up
   * @param parentContext Object identifier of the parent object
   * @return The actual object
   * @throws AbortedOperationException
   * @throws TCClassNotFoundException If a class is not found during faulting
   */
  public Object lookupObject(ObjectID id, ObjectID parentContext) throws ClassNotFoundException,
      AbortedOperationException;

  /**
   * Find managed object, which may be null
   * 
   * @param obj The object instance
   * @return The TCObject
   */
  public TCObject lookupExistingOrNull(Object obj);

  /**
   * Find or create new TCObject
   * 
   * @param obj The object instance
   * @return The TCObject
   */
  public TCObject lookupOrCreate(Object obj);

  public TCObject lookupOrCreate(Object obj, GroupID gid);

  /**
   * Perform invoke on logical managed object
   * 
   * @param object The object
   * @param methodName The method to call
   * @param params The parameters to the method
   */
  public void logicalInvoke(Object object, String methodName, Object[] params);

  /**
   * Perform invoke on logical managed object in lock
   * 
   * @param object The object
   * @param lockObject The lock object
   * @param methodName The method to call
   * @param params The parameters to the method
   * @throws AbortedOperationException
   */
  public void logicalInvokeWithTransaction(Object object, Object lockObject, String methodName, Object[] params)
      throws AbortedOperationException;

  /**
   * Perform distributed method call
   * 
   * @param receiver The receiver object
   * @param method The method to call
   * @param params The parameter values
   * @param runOnAllNodes True if should run on all nodes, false just for this node
   */
  public boolean distributedMethodCall(Object receiver, String method, Object[] params, boolean runOnAllNodes);

  /**
   * Commit DMI call
   */
  public void distributedMethodCallCommit();

  /**
   * Lookup root by name
   * 
   * @param name Name of root
   * @return Root object
   * @throws AbortedOperationException
   */
  public Object lookupRoot(String name) throws AbortedOperationException;

  /**
   * Check whether current context has write access
   * 
   * @param context Context object
   * @throws com.tc.object.util.ReadOnlyException If in read-only transaction
   */
  public void checkWriteAccess(Object context);

  /**
   * @return true if obj is an instance of a {@link com.tc.object.LiteralValues literal type}, e.g., Class, Integer,
   *         etc.
   */
  public boolean isLiteralInstance(Object obj);

  /**
   * Check whether an object is managed
   * 
   * @param object Instance
   * @return True if managed
   */
  public boolean isManaged(Object object);

  /**
   * @return true if obj is an instance of a {@link com.tc.object.LiteralValues literal type} and is suitable for
   *         cluster-wide locking,
   */
  public boolean isLiteralAutolock(final Object o);

  /**
   * Retrieve the customer change applicator that was registered for a particular class.
   * 
   * @param clazz The class for which the custom change application has to be returned
   * @return the instance of the custom change applicator; or {@code null} if no custom applicator was registered for
   *         this class
   */
  public Object getChangeApplicator(Class clazz);

  /**
   * Check whether object is logically instrumented
   * 
   * @param object Instance
   * @return True if logically instrumented
   */
  public boolean isLogical(Object object);

  /**
   * Check whether field is a root
   * 
   * @param field Field
   * @return True if root
   */
  public boolean isRoot(Field field);

  /**
   * Get JVM Client identifier
   * 
   * @return Client identifier
   */
  public String getClientID();

  /**
   * Get unique Client identifier
   * 
   * @return unique Client identifier
   */
  public String getUUID();

  /**
   * Get the named logger
   * 
   * @param loggerName Logger name
   * @return The logger
   */
  public TCLogger getLogger(String loggerName);

  /**
   * @return TCProperties
   */
  public TCProperties getTCProperties();

  /**
   * Returns true if the field represented by the offset is a portable field, i.e., not static and not dso transient
   * 
   * @param pojo Object
   * @param fieldOffset The index
   * @return true if the field is portable and false otherwise
   */
  public boolean isFieldPortableByOffset(Object pojo, long fieldOffset);

  /**
   * Get the ClassProvider associated with this Manager
   */
  public ClassProvider getClassProvider();

  /**
   * Get the TunneledDomainUpdater associated with this Manager
   */
  public TunneledDomainUpdater getTunneledDomainUpdater();

  /**
   * Retrieves the DSO cluster instance.
   * 
   * @return the DSO cluster instance for this manager
   */
  public DsoCluster getDsoCluster();

  /**
   * Retrieves the MBean server that's used by this Terracotta client
   * 
   * @return the MBean server for this client
   */
  public MBeanServer getMBeanServer();

  /**
   * Used by BulkLoad to wait for all current transactions completed
   * 
   * @throws AbortedOperationException
   */
  public void waitForAllCurrentTransactionsToComplete() throws AbortedOperationException;

  /**
   * Registers a hook that will be called before shutting down this client
   */
  public void registerBeforeShutdownHook(Runnable beforeShutdownHook);

  public void unregisterBeforeShutdownHook(Runnable beforeShutdownHook);

  MetaDataDescriptor createMetaDataDescriptor(String category);

  public SearchQueryResults executeQuery(String cachename, List queryStack, boolean includeKeys, boolean includeValues,
                                         Set<String> attributeSet, List<NVPair> sortAttributes,
                                         List<NVPair> aggregators, int maxResults, int batchSize, boolean waitForTxn)
      throws AbortedOperationException;

  public SearchQueryResults executeQuery(String cachename, List queryStack, Set<String> attributeSet,
                                         Set<String> groupByAttribues, List<NVPair> sortAttributes,
                                         List<NVPair> aggregators, int maxResults, int batchSize, boolean waitForTxn)
      throws AbortedOperationException;

  public NVPair createNVPair(String name, Object value);

  void verifyCapability(String capability);

  void fireOperatorEvent(EventType eventLevel, EventSubsystem subsystem, String eventMessage);

  void stopImmediate();

  void initForTests(CountDownLatch latch);

  public GroupID[] getGroupIDs();

  void lockIDWait(final LockID lock, final long timeout) throws InterruptedException, AbortedOperationException;

  void lockIDNotifyAll(final LockID lock) throws AbortedOperationException;

  void lockIDNotify(final LockID lock) throws AbortedOperationException;

  /**
   * Register an object with given name if null is mapped currently to the name. Otherwise returns old mapped object.
   * 
   * @param name Name to use for registering the object
   * @param object Object to register
   * @return the previous value associated with the specified name, or same 'object' if there was no mapping for the
   *         name
   */
  <T> T registerObjectByNameIfAbsent(String name, T object);

  /**
   * Lookup and return an already registered object by name if it exists, otherwise null.
   * 
   * @return lookup and return an already registered object by name if it exists, otherwise null
   * @throws ClassCastException if a mapping exists for name, but is of different type other than expectedType
   */
  <T> T lookupRegisteredObjectByName(String name, Class<T> expectedType);

  void addTransactionCompleteListener(TransactionCompleteListener listener);

  AbortableOperationManager getAbortableOperationManager();

  PlatformService getPlatformService();

  void throttlePutIfNecessary(ObjectID object) throws AbortedOperationException;

  void beginAtomicTransaction(LockID lock, LockLevel level) throws AbortedOperationException;

  void commitAtomicTransaction(LockID lock, LockLevel level) throws AbortedOperationException;

  void registerServerEventListener(ServerEventDestination destination, Set<ServerEventType> listenTo);

  void unregisterServerEventListener(ServerEventDestination destination);

  int getRejoinCount();

  boolean isRejoinInProgress();

  TaskRunner getTastRunner();
}
