/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

// import com.partitions.TCNoPartitionError;

import com.tc.abortable.AbortableOperationManager;
import com.tc.abortable.AbortedOperationException;
import com.tc.exception.TCClassNotFoundException;
import com.tc.logging.TCLogger;
import com.tc.net.GroupID;
import com.tc.object.LogicalOperation;
import com.tc.object.ObjectID;
import com.tc.object.TCObject;
import com.tc.object.locks.LockID;
import com.tc.object.locks.LockLevel;
import com.tc.object.tx.TransactionCompleteListener;
import com.tc.operatorevent.TerracottaOperatorEvent.EventLevel;
import com.tc.operatorevent.TerracottaOperatorEvent.EventSubsystem;
import com.tc.operatorevent.TerracottaOperatorEvent.EventType;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * A bunch of static methods that make calling Manager method much easier from instrumented classes
 */
public class ManagerUtil {

  /** This class name */
  public static final String      CLASS        = "com/tc/object/bytecode/ManagerUtil";
  /** This class type */
  public static final String      TYPE         = "L" + CLASS + ";";

  private static final Manager    NULL_MANAGER = NullManager.getInstance();

  private static volatile boolean ENABLED      = false;

  private static String           SINGLETON_INIT_INFO;
  private static volatile Manager SINGLETON    = null;

  /**
   * Called when initialization has proceeded enough that the Manager can be used.
   */
  public static void enable() {
    ENABLED = true;
  }

  public static void enableSingleton(final Manager singleton) {
    if (singleton == null) { throw new NullPointerException("null singleton"); }

    synchronized (ManagerUtil.class) {
      if (SINGLETON != null) {
        //
        throw new IllegalStateException(SINGLETON_INIT_INFO);
      }

      SINGLETON = singleton;
      SINGLETON_INIT_INFO = captureInitInfo();
    }

    enable();
  }

  protected static void clearSingleton() {
    SINGLETON = null;
  }

  private static String captureInitInfo() {
    StringWriter sw = new StringWriter();
    sw.append("The singleton instance was initialized at " + new Date() + " by thread ["
              + Thread.currentThread().getName() + "] with stack:\n");
    PrintWriter pw = new PrintWriter(sw);
    new Throwable().printStackTrace(pw);
    pw.close();
    return sw.toString();
  }

  public static boolean isManagerEnabled() {
    return ENABLED;
  }

  public static Manager getManager() {
    if (!ENABLED) { return NULL_MANAGER; }

    Manager rv = SINGLETON;
    if (rv != null) return rv;

    return NULL_MANAGER;
  }

  /**
   * Get the named logger
   *
   * @param loggerName Logger name
   * @return The logger
   */
  public static TCLogger getLogger(final String loggerName) {
    return getManager().getLogger(loggerName);
  }

  /**
   * Get JVM Client identifier
   *
   * @return Client identifier
   */
  protected static String getClientID() {
    return getManager().getClientID().toString();
  }

  /**
   * Get Unique Client identifier
   *
   * @return Unique Client identifier
   */
  protected static String getUUID() {
    return getManager().getUUID();
  }

  /**
   * Look up or create a new root object
   *
   * @param name Root name
   * @param object Root object to use if none exists yet
   * @return The root object actually used, may or may not == object
   */
  protected static Object lookupOrCreateRoot(final String name, final Object object) {
    return getManager().lookupOrCreateRoot(name, object);
  }

  /**
   * Look up or create a new root object in the particular group id
   */
  protected static Object lookupOrCreateRoot(final String name, final Object object, GroupID gid) {
    return getManager().lookupOrCreateRoot(name, object, gid);
  }

  /**
   * Look up or create a new root object in the particular group id
   */
  protected static Object lookupRoot(final String name, GroupID gid) {
    return getManager().lookupRoot(name, gid);
  }

  /**
   * Look up or create a new root object. Objects faulted in to arbitrary depth.
   *
   * @param name Root name
   * @param obj Root object to use if none exists yet
   * @return The root object actually used, may or may not == object
   * @throws AbortedOperationException
   */
  protected static Object lookupOrCreateRootNoDepth(final String name, final Object obj)
      throws AbortedOperationException {
    return getManager().lookupOrCreateRootNoDepth(name, obj);
  }

  /**
   * Create or replace root, typically used for replaceable roots.
   *
   * @param rootName Root name
   * @param object Root object
   * @return Root object used
   * @throws AbortedOperationException
   */
  protected static Object createOrReplaceRoot(final String rootName, final Object object)
      throws AbortedOperationException {
    return getManager().createOrReplaceRoot(rootName, object);
  }

  /**
   * Begin volatile lock by field offset in the class
   *
   * @param pojo Instance containing field
   * @param fieldOffset Field offset in pojo
   * @param level Lock level
   * @throws AbortedOperationException
   */
  protected static void beginVolatile(final Object pojo, final long fieldOffset, final LockLevel level)
      throws AbortedOperationException {
    TCObject TCObject = lookupExistingOrNull(pojo);
    beginVolatile(TCObject, TCObject.getFieldNameByOffset(fieldOffset), level);
  }

  /**
   * Commit volatile lock by field offset in the class
   *
   * @param pojo Instance containing field
   * @param fieldOffset Field offset in pojo
   * @throws AbortedOperationException
   */
  protected static void commitVolatile(final Object pojo, final long fieldOffset, final LockLevel level)
      throws AbortedOperationException {
    TCObject TCObject = lookupExistingOrNull(pojo);
    commitVolatile(TCObject, TCObject.getFieldNameByOffset(fieldOffset), level);
  }

  /**
   * Begin volatile lock
   *
   * @param TCObject TCObject to lock
   * @param fieldName Field name holding volatile object
   * @param level Lock type
   * @throws AbortedOperationException
   */
  protected static void beginVolatile(final TCObject TCObject, final String fieldName, final LockLevel level)
      throws AbortedOperationException {
    Manager mgr = getManager();
    LockID lock = mgr.generateLockIdentifier(TCObject, fieldName);
    mgr.lock(lock, level);
  }


  protected static boolean tryBeginLock(final Object obj, final LockLevel level) throws AbortedOperationException {
    Manager mgr = getManager();
    LockID lock = mgr.generateLockIdentifier(obj);
    return mgr.tryLock(lock, level);
  }

  protected static boolean tryBeginLock(final Object obj, final LockLevel level, final long time, final TimeUnit unit)
      throws InterruptedException, AbortedOperationException {
    Manager mgr = getManager();
    LockID lock = mgr.generateLockIdentifier(obj);
    return mgr.tryLock(lock, level, unit.toMillis(time));
  }


  /**
   * Commit volatile lock
   *
   * @param TCObject Volatile object TCObject
   * @param fieldName Field holding the volatile object
   * @throws AbortedOperationException
   */
  protected static void commitVolatile(final TCObject TCObject, final String fieldName, final LockLevel level)
      throws AbortedOperationException {
    Manager mgr = getManager();
    LockID lock = mgr.generateLockIdentifier(TCObject, fieldName);
    mgr.unlock(lock, level);
  }


  protected static void pinLock0(final String lockID, long awardID) {
    Manager mgr = getManager();
    LockID lock = mgr.generateLockIdentifier(lockID);
    mgr.pinLock(lock, awardID);
  }

  protected static void unpinLock0(final String lockID, long awardID) {
    Manager mgr = getManager();
    LockID lock = mgr.generateLockIdentifier(lockID);
    mgr.unpinLock(lock, awardID);
  }

  /**
   * Find managed object, which may be null
   *
   * @param pojo The object instance
   * @return The TCObject
   */
  protected static TCObject lookupExistingOrNull(final Object pojo) {
    return getManager().lookupExistingOrNull(pojo);
  }

  /**
   * Perform invoke on logical managed object
   *
   * @param object The object
   * @param methodName The method to call
   * @param params The parameters to the method
   */
  protected static void logicalInvoke(final Object object, final LogicalOperation method, final Object[] params) {
    getManager().logicalInvoke(object, method, params);
  }

  /**
   * Perform invoke on logical managed object in lock
   *
   * @param object The object
   * @param lockObject The lock object
   * @param methodName The method to call
   * @param params The parameters to the method
   * @throws AbortedOperationException
   */
  protected static void logicalInvokeWithTransaction(final Object object, final Object lockObject,
                                                     final LogicalOperation method, final Object[] params)
      throws AbortedOperationException {
    getManager().logicalInvokeWithTransaction(object, lockObject, method, params);
  }

  /**
   * Lookup root by name
   *
   * @param name Name of root
   * @return Root object
   * @throws AbortedOperationException
   */
  protected static Object lookupRoot(final String name) throws AbortedOperationException {
    return getManager().lookupRoot(name);
  }

  /**
   * Look up object by ID, faulting into the JVM if necessary
   *
   * @param id Object identifier
   * @return The actual object
   * @throws AbortedOperationException
   * @throws TCClassNotFoundException If a class is not found during faulting
   */
  protected static Object lookupObject(final ObjectID id) throws AbortedOperationException {
    try {
      return getManager().lookupObject(id);
    } catch (ClassNotFoundException e) {
      throw new TCClassNotFoundException(e);
    }
  }

  /**
   * Prefetch object by ID, faulting into the JVM if necessary, Async lookup and will not cause ObjectNotFoundException
   * like lookupObject. Non-existent objects are ignored by the server.
   *
   * @param id Object identifier
   * @throws AbortedOperationException
   */
  protected static void preFetchObject(final ObjectID id) throws AbortedOperationException {
    getManager().preFetchObject(id);
  }

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
  protected static Object lookupObjectWithParentContext(final ObjectID id, final ObjectID parentContext)
      throws AbortedOperationException {
    try {
      return getManager().lookupObject(id, parentContext);
    } catch (ClassNotFoundException e) {
      throw new TCClassNotFoundException(e);
    }
  }

  /**
   * Find or create new TCObject
   *
   * @param obj The object instance
   * @return The TCObject
   */
  protected static TCObject lookupOrCreate(final Object obj) {
    return getManager().lookupOrCreate(obj);
  }

  /**
   * Find or create new TCObject
   *
   * @param obj The object instance
   * @return The TCObject
   */
  protected static TCObject lookupOrCreate(final Object obj, GroupID gid) {
    return getManager().lookupOrCreate(obj, gid);
  }

  /**
   * Check whether current context has write access
   *
   * @param context Context object
   * @throws com.tc.object.util.ReadOnlyException If in read-only transaction
   */
  protected static void checkWriteAccess(final Object context) {
    getManager().checkWriteAccess(context);
  }

  /**
   * Check whether an object is managed
   *
   * @param obj Instance
   * @return True if managed
   */
  protected static boolean isManaged(final Object obj) {
    return getManager().isManaged(obj);
  }

  /**
   * Check whether object is logically instrumented
   *
   * @param obj Instance
   * @return True if logically instrumented
   */
  protected static boolean isLogical(final Object obj) {
    return getManager().isLogical(obj);
  }

  /**
   * Check whether field is a root
   *
   * @param field Field
   * @return True if root
   */
  protected static boolean isRoot(final Field field) {
    return getManager().isRoot(field);
  }

  /**
   * Perform notify on obj
   *
   * @param obj Instance
   * @throws AbortedOperationException
   */
  protected static void objectNotify(final Object obj) throws AbortedOperationException {
    Manager mgr = getManager();
    LockID lock = mgr.generateLockIdentifier(obj);
    mgr.notify(lock, obj);
  }

  /**
   * Perform notifyAll on obj
   *
   * @param obj Instance
   * @throws AbortedOperationException
   */
  protected static void objectNotifyAll(final Object obj) throws AbortedOperationException {
    Manager mgr = getManager();
    LockID lock = mgr.generateLockIdentifier(obj);
    mgr.notifyAll(lock, obj);
  }

  /**
   * Perform untimed wait on obj
   *
   * @param obj Instance
   * @throws AbortedOperationException
   */
  protected static void objectWait(final Object obj) throws InterruptedException, AbortedOperationException {
    Manager mgr = getManager();
    LockID lock = mgr.generateLockIdentifier(obj);
    mgr.wait(lock, obj);
  }

  /**
   * Perform timed wait on obj
   *
   * @param obj Instance
   * @param millis Wait time
   * @throws AbortedOperationException
   */
  protected static void objectWait(final Object obj, final long millis) throws InterruptedException,
      AbortedOperationException {
    Manager mgr = getManager();
    LockID lock = mgr.generateLockIdentifier(obj);
    mgr.wait(lock, obj, millis);
  }

  /**
   * Perform timed wait on obj
   *
   * @param obj Instance
   * @param millis Wait time
   * @param nanos More wait time
   * @throws AbortedOperationException
   */
  protected static void objectWait(final Object obj, long millis, final int nanos) throws InterruptedException,
      AbortedOperationException {
    if (nanos >= 500000 || (nanos != 0 && millis == 0)) {
      millis++;
    }

    objectWait(obj, millis);
  }



  /**
   * @return true if obj is an instance of a {@link com.tc.object.LiteralValues literal type}, e.g., Class, Integer,
   *         etc.
   */
  protected static boolean isLiteralInstance(final Object obj) {
    return getManager().isLiteralInstance(obj);
  }

  /**
   * Check whether an object is locked at this lockLevel
   *
   * @param obj Lock
   * @param level Lock level
   * @return True if locked at this level
   * @throws AbortedOperationException
   * @throws NullPointerException If obj is null
   */
  protected static boolean isLocked(final Object obj, final LockLevel level) throws AbortedOperationException {
    Manager mgr = getManager();
    LockID lock = mgr.generateLockIdentifier(obj);
    return mgr.isLocked(lock, level);
  }


  /**
   * Acquire lock (interruptibly).
   *
   * @param obj The object monitor
   * @param level The lock level
   * @throws NullPointerException If obj is null
   * @throws InterruptedException If interrupted while entering or waiting
   * @throws AbortedOperationException
   */
  protected static void beginLockInterruptibly(Object obj, LockLevel level) throws InterruptedException,
      AbortedOperationException {
    Manager mgr = getManager();
    LockID lock = mgr.generateLockIdentifier(obj);
    mgr.lockInterruptibly(lock, level);
  }

  /**
   * Get number of locks held locally on this object
   *
   * @param obj The lock object
   * @param level The lock level
   * @return Lock count
   * @throws AbortedOperationException
   * @throws NullPointerException If obj is null
   */
  protected static int localHeldCount(final Object obj, final LockLevel level) throws AbortedOperationException {
    Manager mgr = getManager();
    LockID lock = mgr.generateLockIdentifier(obj);
    return mgr.localHoldCount(lock, level);
  }

  /**
   * Check whether this lock is held by the current thread
   *
   * @param obj The lock
   * @param level The lock level
   * @return True if held by current thread
   * @throws AbortedOperationException
   * @throws NullPointerException If obj is null
   */
  protected static boolean isHeldByCurrentThread(final Object obj, final LockLevel level)
      throws AbortedOperationException {
    Manager mgr = getManager();
    LockID lock = mgr.generateLockIdentifier(obj);
    return mgr.isLockedByCurrentThread(lock, level);
  }

  /**
   * Check whether this lock is held by the current thread
   *
   * @param lockId The lock ID
   * @param level The lock level
   * @return True if held by current thread
   * @throws AbortedOperationException
   */
  protected static boolean isLockHeldByCurrentThread(final String lockId, final LockLevel level)
      throws AbortedOperationException {
    Manager mgr = getManager();
    LockID lock = mgr.generateLockIdentifier(lockId);
    return mgr.isLockedByCurrentThread(lock, level);
  }

  /**
   * Number in queue waiting on this lock
   *
   * @param obj The object
   * @return Number of waiters
   * @throws AbortedOperationException
   * @throws NullPointerException If obj is null
   */
  protected static int queueLength(final Object obj) throws AbortedOperationException {
    Manager mgr = getManager();
    LockID lock = mgr.generateLockIdentifier(obj);
    return mgr.globalPendingCount(lock);
  }

  /**
   * Number in queue waiting on this wait()
   *
   * @param obj The object
   * @return Number of waiters
   * @throws AbortedOperationException
   * @throws NullPointerException If obj is null
   */
  protected static int waitLength(final Object obj) throws AbortedOperationException {
    Manager mgr = getManager();
    LockID lock = mgr.generateLockIdentifier(obj);
    return mgr.globalWaitingCount(lock);
  }

  private ManagerUtil() {
    // not for protected instantiation
  }

  /**
                                                   boolean includeValues, Set<String> attributeSet,
                                                   List<NVPair> sortAttributes, List<NVPair> aggregators,
                                                   int maxResults, int batchSize, boolean waitForTxn)
      throws AbortedOperationException {
    return getManager().executeQuery(cachename, queryStack, includeKeys, includeValues, attributeSet, sortAttributes,
                                     aggregators, maxResults, batchSize, waitForTxn);
  }

  protected static SearchQueryResults executeQuery(String cachename, List queryStack, Set<String> attributeSet,
                                                   Set<String> groupByAttributes, List<NVPair> sortAttributes,
                                                   List<NVPair> aggregators, int maxResults, int batchSize,
                                                   boolean waitForTxn) throws AbortedOperationException {
    return getManager().executeQuery(cachename, queryStack, attributeSet, groupByAttributes, sortAttributes,
                                     aggregators, maxResults, batchSize, waitForTxn);
  }

  protected static NVPair createNVPair(String name, Object value) {
    return getManager().createNVPair(name, value);
  }

  /**
   * Begin lock
   *
   * @param lockID Lock identifier
   * @param level Lock type
   * @throws AbortedOperationException
   */
  protected static void beginLock(final Object lockID, final LockLevel level) throws AbortedOperationException {
    Manager mgr = getManager();
    LockID lock = mgr.generateLockIdentifier(lockID);
    mgr.lock(lock, level);
  }

  /**
   * Commit lock
   *
   * @param lockID Lock name
   * @throws AbortedOperationException
   */
  protected static void commitLock(final Object lockID, final LockLevel level) throws AbortedOperationException {
    Manager mgr = getManager();
    LockID lock = mgr.generateLockIdentifier(lockID);
    mgr.unlock(lock, level);
  }


  protected static void pinLock0(final long lockID, long awardID) {
    Manager mgr = getManager();
    LockID lock = mgr.generateLockIdentifier(lockID);
    mgr.pinLock(lock, awardID);
  }

  protected static void unpinLock0(final long lockID, long awardID) {
    Manager mgr = getManager();
    LockID lock = mgr.generateLockIdentifier(lockID);
    mgr.unpinLock(lock, awardID);
  }

  /**
   * Check whether this lock is held by the current thread
   *
   * @param lockId The lock ID
   * @param level The lock level
   * @return True if held by current thread
   * @throws AbortedOperationException
   */
  protected static boolean isLockHeldByCurrentThread(final long lockId, final LockLevel level)
      throws AbortedOperationException {
    Manager mgr = getManager();
    LockID lock = mgr.generateLockIdentifier(lockId);
    return mgr.isLockedByCurrentThread(lock, level);
  }

  protected static void verifyCapability(String capability) {
    getManager().verifyCapability(capability);
  }

  protected static void fireOperatorEvent(EventLevel coreOperatorEventLevel, EventSubsystem coreEventSubsytem,
                                          EventType eventType, String eventMessage) {
    getManager().fireOperatorEvent(coreOperatorEventLevel, coreEventSubsytem, eventType, eventMessage);
  }

  protected static GroupID[] getGroupIDs() {
    return getManager().getGroupIDs();
  }

  protected static void lockIDWait(final Object lockID, long time, TimeUnit unit) throws InterruptedException,
      AbortedOperationException {
    Manager mgr = getManager();
    LockID lock = mgr.generateLockIdentifier(lockID);
    mgr.lockIDWait(lock, unit.toMillis(time));
  }

  protected static void lockIDNotifyAll(final Object lockID) throws AbortedOperationException {
    Manager mgr = getManager();
    LockID lock = mgr.generateLockIdentifier(lockID);
    mgr.lockIDNotifyAll(lock);
  }

  protected static void lockIDNotify(final Object lockID) throws AbortedOperationException {
    Manager mgr = getManager();
    LockID lock = mgr.generateLockIdentifier(lockID);
    mgr.lockIDNotify(lock);
  }

  protected static void registerBeforeShutdownHook(Runnable r) {
    Manager mgr = getManager();
    mgr.registerBeforeShutdownHook(r);
  }

  public static <T> T registerObjectByNameIfAbsent(String name, T object) {
    Manager mgr = getManager();
    return mgr.registerObjectByNameIfAbsent(name, object);
  }

  public static <T> T lookupRegisteredObjectByName(String name, Class<T> expectedType) {
    Manager mgr = getManager();
    return mgr.lookupRegisteredObjectByName(name, expectedType);
  }

  protected static void addTransactionCompleteListener(TransactionCompleteListener listener) {
    Manager mgr = getManager();
    mgr.addTransactionCompleteListener(listener);
  }

  protected static AbortableOperationManager getAbortableOperationManager() {
    Manager mgr = getManager();
    return mgr.getAbortableOperationManager();
  }

  public static void throttlePutIfNecessary(ObjectID object) throws AbortedOperationException {
    getManager().throttlePutIfNecessary(object);
  }
}
