/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

// import com.partitions.TCNoPartitionError;

import com.tc.exception.TCClassNotFoundException;
import com.tc.logging.TCLogger;
import com.tc.object.ObjectID;
import com.tc.object.TCObject;
import com.tc.object.bytecode.hook.impl.ArrayManager;
import com.tc.object.bytecode.hook.impl.ClassProcessorHelper;
import com.tc.object.locks.LockID;
import com.tc.object.locks.LockLevel;
import com.tc.object.metadata.MetaDataDescriptor;
import com.tc.object.metadata.NVPair;
import com.tc.operatorevent.TerracottaOperatorEvent.EventSubsystem;
import com.tc.operatorevent.TerracottaOperatorEvent.EventType;
import com.tc.properties.TCProperties;
import com.tc.search.SearchQueryResults;
import com.tc.statistics.StatisticRetrievalAction;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.Date;
import java.util.List;
import java.util.Set;

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

  public static void clearSingleton() {
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

    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    rv = ClassProcessorHelper.getManager(loader);
    if (rv == null) { return NULL_MANAGER; }
    return rv;
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
   * Determine whether this class is physically instrumented
   * 
   * @param clazz Class
   * @return True if physically instrumented
   */
  public static boolean isPhysicallyInstrumented(final Class clazz) {
    return getManager().isPhysicallyInstrumented(clazz);
  }

  /**
   * Get JVM Client identifier
   * 
   * @return Client identifier
   */
  public static String getClientID() {
    return getManager().getClientID();
  }

  /**
   * Get Unique Client identifier
   * 
   * @return Unique Client identifier
   */
  public static String getUUID() {
    return getManager().getUUID();
  }

  public static void registerStatisticRetrievalAction(StatisticRetrievalAction sra) {
    getManager().registerStatisticRetrievalAction(sra);
  }

  /**
   * Look up or create a new root object
   * 
   * @param name Root name
   * @param object Root object to use if none exists yet
   * @return The root object actually used, may or may not == object
   */
  public static Object lookupOrCreateRoot(final String name, final Object object) {
    return getManager().lookupOrCreateRoot(name, object);
  }

  /**
   * Look up or create a new root object. Objects faulted in to arbitrary depth.
   * 
   * @param name Root name
   * @param obj Root object to use if none exists yet
   * @return The root object actually used, may or may not == object
   */
  public static Object lookupOrCreateRootNoDepth(final String name, final Object obj) {
    return getManager().lookupOrCreateRootNoDepth(name, obj);
  }

  /**
   * Create or replace root, typically used for replaceable roots.
   * 
   * @param rootName Root name
   * @param object Root object
   * @return Root object used
   */
  public static Object createOrReplaceRoot(final String rootName, final Object object) {
    return getManager().createOrReplaceRoot(rootName, object);
  }

  /**
   * Begin volatile lock by field offset in the class
   * 
   * @param pojo Instance containing field
   * @param fieldOffset Field offset in pojo
   * @param type Lock level
   */
  public static void beginVolatile(final Object pojo, final long fieldOffset, final int type) {
    TCObject TCObject = lookupExistingOrNull(pojo);
    beginVolatile(TCObject, TCObject.getFieldNameByOffset(fieldOffset), type);
  }

  /**
   * Commit volatile lock by field offset in the class
   * 
   * @param pojo Instance containing field
   * @param fieldOffset Field offset in pojo
   */
  public static void commitVolatile(final Object pojo, final long fieldOffset, final int type) {
    TCObject TCObject = lookupExistingOrNull(pojo);
    commitVolatile(TCObject, TCObject.getFieldNameByOffset(fieldOffset), type);
  }

  /**
   * Begin volatile lock
   * 
   * @param TCObject TCObject to lock
   * @param fieldName Field name holding volatile object
   * @param type Lock type
   */
  public static void beginVolatile(final TCObject TCObject, final String fieldName, final int type) {
    Manager mgr = getManager();
    LockID lock = mgr.generateLockIdentifier(TCObject, fieldName);
    mgr.lock(lock, LockLevel.fromInt(type));
  }

  /**
   * Begin lock
   * 
   * @param lockID Lock identifier
   * @param type Lock type
   */
  public static void beginLock(final String lockID, final int type) {
    Manager mgr = getManager();
    LockID lock = mgr.generateLockIdentifier(lockID);
    mgr.lock(lock, LockLevel.fromInt(type));
  }

  /**
   * Begins a lock without associating any transaction context.
   */
  @Deprecated
  public static void beginLockWithoutTxn(final String lockID, final int type) {
    beginLock(lockID, type);
  }

  /**
   * Begin lock
   * 
   * @param lockID Lock identifier
   * @param type Lock type
   * @param contextInfo
   */
  @Deprecated
  public static void beginLock(final String lockID, final int type, final String contextInfo) {
    beginLock(lockID, type);
  }

  /**
   * Try to begin lock
   * 
   * @param lockID Lock identifier
   * @param type Lock type
   * @return True if lock was successful
   */
  public static boolean tryBeginLock(final String lockID, final int type) {
    Manager mgr = getManager();
    LockID lock = mgr.generateLockIdentifier(lockID);
    return mgr.tryLock(lock, LockLevel.fromInt(type));
  }

  /**
   * Try to begin lock within a specific timespan
   * 
   * @param lockID Lock identifier
   * @param type Lock type
   * @param timeoutInNanos Timeout in nanoseconds
   * @return True if lock was successful
   */
  public static boolean tryBeginLock(final String lockID, final int type, final long timeoutInNanos)
      throws InterruptedException {
    Manager mgr = getManager();
    LockID lock = mgr.generateLockIdentifier(lockID);
    return mgr.tryLock(lock, LockLevel.fromInt(type), timeoutInNanos / 1000000);
  }

  /**
   * Commit volatile lock
   * 
   * @param TCObject Volatile object TCObject
   * @param fieldName Field holding the volatile object
   */
  public static void commitVolatile(final TCObject TCObject, final String fieldName, final int type) {
    Manager mgr = getManager();
    LockID lock = mgr.generateLockIdentifier(TCObject, fieldName);
    mgr.unlock(lock, LockLevel.fromInt(type));
  }

  /**
   * Commit lock
   * 
   * @param lockID Lock name
   */
  public static void commitLock(final String lockID, final int type) {
    Manager mgr = getManager();
    LockID lock = mgr.generateLockIdentifier(lockID);
    mgr.unlock(lock, LockLevel.fromInt(type));
  }

  public static void pinLock(final String lockID) {
    Manager mgr = getManager();
    LockID lock = mgr.generateLockIdentifier(lockID);
    mgr.pinLock(lock);
  }

  public static void unpinLock(final String lockID) {
    Manager mgr = getManager();
    LockID lock = mgr.generateLockIdentifier(lockID);
    mgr.unpinLock(lock);
  }

  /**
   * Find managed object, which may be null
   * 
   * @param pojo The object instance
   * @return The TCObject
   */
  public static TCObject lookupExistingOrNull(final Object pojo) {
    return getManager().lookupExistingOrNull(pojo);
  }

  /**
   * Perform invoke on logical managed object
   * 
   * @param object The object
   * @param methodName The method to call
   * @param params The parameters to the method
   */
  public static void logicalInvoke(final Object object, final String methodName, final Object[] params) {
    getManager().logicalInvoke(object, methodName, params);
  }

  /**
   * Perform invoke on logical managed object in lock
   * 
   * @param object The object
   * @param lockObject The lock object
   * @param methodName The method to call
   * @param params The parameters to the method
   */
  public static void logicalInvokeWithTransaction(final Object object, final Object lockObject,
                                                  final String methodName, final Object[] params) {
    getManager().logicalInvokeWithTransaction(object, lockObject, methodName, params);
  }

  /**
   * Commit DMI call
   */
  public static void distributedMethodCallCommit() {
    getManager().distributedMethodCallCommit();
  }

  /**
   * Perform distributed method call on just this node
   * 
   * @param receiver The receiver object
   * @param method The method to call
   * @param params The parameter values
   */
  public static boolean prunedDistributedMethodCall(final Object receiver, final String method, final Object[] params) {
    return getManager().distributedMethodCall(receiver, method, params, false);
  }

  /**
   * Perform distributed method call on all nodes
   * 
   * @param receiver The receiver object
   * @param method The method to call
   * @param params The parameter values
   */
  public static boolean distributedMethodCall(final Object receiver, final String method, final Object[] params) {
    return getManager().distributedMethodCall(receiver, method, params, true);
  }

  /**
   * Lookup root by name
   * 
   * @param name Name of root
   * @return Root object
   */
  public static Object lookupRoot(final String name) {
    return getManager().lookupRoot(name);
  }

  /**
   * Look up object by ID, faulting into the JVM if necessary
   * 
   * @param id Object identifier
   * @return The actual object
   * @throws TCClassNotFoundException If a class is not found during faulting
   */
  public static Object lookupObject(final ObjectID id) {
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
   */
  public static void preFetchObject(final ObjectID id) {
    getManager().preFetchObject(id);
  }

  /**
   * Look up object by ID, faulting into the JVM if necessary, This method also passes the parent Object context so that
   * more intelligent prefetching is possible at the L2.
   * 
   * @param id Object identifier of the object we are looking up
   * @param parentContext Object identifier of the parent object
   * @return The actual object
   * @throws TCClassNotFoundException If a class is not found during faulting
   */
  public static Object lookupObjectWithParentContext(final ObjectID id, final ObjectID parentContext) {
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
  public static TCObject lookupOrCreate(final Object obj) {
    return getManager().lookupOrCreate(obj);
  }

  /**
   * Check whether current context has write access
   * 
   * @param context Context object
   * @throws com.tc.object.util.ReadOnlyException If in read-only transaction
   */
  public static void checkWriteAccess(final Object context) {
    getManager().checkWriteAccess(context);
  }

  /**
   * Check whether an object is managed
   * 
   * @param obj Instance
   * @return True if managed
   */
  public static boolean isManaged(final Object obj) {
    return getManager().isManaged(obj);
  }

  /**
   * Check whether an object is shared
   * 
   * @param obj Instance
   * @return True if shared
   */
  public static boolean isDsoMonitored(final Object obj) {
    return getManager().isDsoMonitored(obj);
  }

  /**
   * Check whether dso MonitorExist is required
   * 
   * @return True if required
   */
  public static boolean isDsoMonitorEntered(final Object obj) {
    return getManager().isDsoMonitorEntered(obj);
  }

  /**
   * Check whether object is logically instrumented
   * 
   * @param obj Instance
   * @return True if logically instrumented
   */
  public static boolean isLogical(final Object obj) {
    return getManager().isLogical(obj);
  }

  /**
   * Check whether field is a root
   * 
   * @param field Field
   * @return True if root
   */
  public static boolean isRoot(final Field field) {
    return getManager().isRoot(field);
  }

  /**
   * Perform notify on obj
   * 
   * @param obj Instance
   */
  public static void objectNotify(final Object obj) {
    Manager mgr = getManager();
    LockID lock = mgr.generateLockIdentifier(obj);
    mgr.notify(lock, obj);
  }

  /**
   * Perform notifyAll on obj
   * 
   * @param obj Instance
   */
  public static void objectNotifyAll(final Object obj) {
    Manager mgr = getManager();
    LockID lock = mgr.generateLockIdentifier(obj);
    mgr.notifyAll(lock, obj);
  }

  /**
   * Perform untimed wait on obj
   * 
   * @param obj Instance
   */
  public static void objectWait(final Object obj) throws InterruptedException {
    Manager mgr = getManager();
    LockID lock = mgr.generateLockIdentifier(obj);
    mgr.wait(lock, obj);
  }

  /**
   * Perform timed wait on obj
   * 
   * @param obj Instance
   * @param millis Wait time
   */
  public static void objectWait(final Object obj, final long millis) throws InterruptedException {
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
   */
  public static void objectWait(final Object obj, long millis, final int nanos) throws InterruptedException {
    if (nanos >= 500000 || (nanos != 0 && millis == 0)) {
      millis++;
    }

    objectWait(obj, millis);
  }

  /**
   * Enter synchronized monitor
   * 
   * @param obj Object
   * @param type Lock type
   */
  public static void monitorEnter(final Object obj, final int type) {
    Manager mgr = getManager();
    LockID lock = mgr.generateLockIdentifier(obj);
    mgr.lock(lock, LockLevel.fromInt(type));
  }

  /**
   * Enter synchronized monitor
   * 
   * @param obj Object
   * @param type Lock type
   * @param contextInfo Configuration text of the lock
   */
  @Deprecated
  public static void monitorEnter(final Object obj, final int type, final String contextInfo) {
    monitorEnter(obj, type);
  }

  /**
   * Exit synchronized monitor
   * 
   * @param obj Object
   */
  public static void monitorExit(final Object obj, final int type) {
    Manager mgr = getManager();
    LockID lock = mgr.generateLockIdentifier(obj);
    mgr.unlock(lock, LockLevel.fromInt(type));
  }

  @Deprecated
  public static void instrumentationMonitorEnter(final Object obj, final int type) {
    Manager mgr = getManager();
    LockID lock = mgr.generateLockIdentifier(obj);
    mgr.monitorEnter(lock, LockLevel.fromInt(type));
  }

  @Deprecated
  public static void instrumentationMonitorExit(final Object obj, final int type) {
    Manager mgr = getManager();
    LockID lock = mgr.generateLockIdentifier(obj);
    mgr.monitorExit(lock, LockLevel.fromInt(type));
  }

  /**
   * @return true if obj is an instance of a {@link com.tc.object.LiteralValues literal type}, e.g., Class, Integer,
   *         etc.
   */
  public static boolean isLiteralInstance(final Object obj) {
    return getManager().isLiteralInstance(obj);
  }

  /**
   * Check whether an object is locked at this lockLevel
   * 
   * @param obj Lock
   * @param lockLevel Lock level
   * @return True if locked at this level
   * @throws NullPointerException If obj is null
   */
  public static boolean isLocked(final Object obj, final int lockLevel) {
    Manager mgr = getManager();
    LockID lock = mgr.generateLockIdentifier(obj);
    return mgr.isLocked(lock, LockLevel.fromInt(lockLevel));
  }

  public static boolean tryMonitorEnter(final Object obj, final int type) {
    Manager mgr = getManager();
    LockID lock = mgr.generateLockIdentifier(obj);
    return mgr.tryLock(lock, LockLevel.fromInt(type));
  }

  /**
   * Try to enter monitor for specified object
   * 
   * @param obj The object monitor
   * @param timeoutInNanos Timeout in nanoseconds
   * @param type The lock level
   * @return True if entered
   * @throws NullPointerException If obj is null
   */
  public static boolean tryMonitorEnter(final Object obj, final int type, final long timeoutInNanos)
      throws InterruptedException {
    Manager mgr = getManager();
    LockID lock = mgr.generateLockIdentifier(obj);
    return mgr.tryLock(lock, LockLevel.fromInt(type), timeoutInNanos / 1000000);
  }

  /**
   * Enter synchronized monitor (interruptibly).
   * 
   * @param obj The object monitor
   * @param type The lock level
   * @throws NullPointerException If obj is null
   * @throws InterruptedException If interrupted while entering or waiting
   */
  public static void monitorEnterInterruptibly(final Object obj, final int type) throws InterruptedException {
    Manager mgr = getManager();
    LockID lock = mgr.generateLockIdentifier(obj);
    mgr.lockInterruptibly(lock, LockLevel.fromInt(type));
  }

  /**
   * Get number of locks held locally on this object
   * 
   * @param obj The lock object
   * @param lockLevel The lock level
   * @return Lock count
   * @throws NullPointerException If obj is null
   */
  public static int localHeldCount(final Object obj, final int lockLevel) {
    Manager mgr = getManager();
    LockID lock = mgr.generateLockIdentifier(obj);
    return mgr.localHoldCount(lock, LockLevel.fromInt(lockLevel));
  }

  /**
   * Check whether this lock is held by the current thread
   * 
   * @param obj The lock
   * @param lockLevel The lock level
   * @return True if held by current thread
   * @throws NullPointerException If obj is null
   */
  public static boolean isHeldByCurrentThread(final Object obj, final int lockLevel) {
    Manager mgr = getManager();
    LockID lock = mgr.generateLockIdentifier(obj);
    return mgr.isLockedByCurrentThread(lock, LockLevel.fromInt(lockLevel));
  }

  /**
   * Check whether this lock is held by the current thread
   * 
   * @param lockId The lock ID
   * @param lockLevel The lock level
   * @return True if held by current thread
   */
  public static boolean isLockHeldByCurrentThread(final String lockId, final int lockLevel) {
    Manager mgr = getManager();
    LockID lock = mgr.generateLockIdentifier(lockId);
    return mgr.isLockedByCurrentThread(lock, LockLevel.fromInt(lockLevel));
  }

  /**
   * Number in queue waiting on this lock
   * 
   * @param obj The object
   * @return Number of waiters
   * @throws NullPointerException If obj is null
   */
  public static int queueLength(final Object obj) {
    Manager mgr = getManager();
    LockID lock = mgr.generateLockIdentifier(obj);
    return mgr.globalPendingCount(lock);
  }

  /**
   * Number in queue waiting on this wait()
   * 
   * @param obj The object
   * @return Number of waiters
   * @throws NullPointerException If obj is null
   */
  public static int waitLength(final Object obj) {
    Manager mgr = getManager();
    LockID lock = mgr.generateLockIdentifier(obj);
    return mgr.globalWaitingCount(lock);
  }

  private ManagerUtil() {
    // not for public instantiation
  }

  /**
   * For java.lang.reflect.Array.get()
   * 
   * @param array The array
   * @param index Index into the array
   * @return Item in array at index, boxed to Object if primitive array
   * @throws NullPointerException If array is null
   * @throws IllegalArgumentException If array is not an array type
   */
  public static Object get(final Object array, final int index) throws IllegalArgumentException,
      ArrayIndexOutOfBoundsException {
    return ArrayManager.get(array, index);
  }

  /**
   * This method is part of java.lang.reflect.Array and does the same as the set() method in the Sun JDK, the IBM
   * version of the set method just adds a series of argument checks and then delegates to the native setImpl version.
   * 
   * @param array Array
   * @param index Index in array
   * @param value New value
   * @throws NullPointerException If array is null
   * @throws IllegalArgumentException If array is an unexpected array type
   * @throws ArrayIndexOutOfBoundsException If index is not in valid range for array
   */
  public static void setImpl(final Object array, final int index, final Object value) throws IllegalArgumentException,
      ArrayIndexOutOfBoundsException {
    set(array, index, value);
  }

  /**
   * This method is part of java.lang.reflect.Array and does the same as the set() method in the Sun JDK, the IBM
   * version of the set method just adds a series of argument checks and then delegates to the native setImpl version.
   * 
   * @param array Array
   * @param index Index in array
   * @param value New value
   * @throws NullPointerException If array is null
   * @throws IllegalArgumentException If array is an unexpected array type
   * @throws ArrayIndexOutOfBoundsException If index is not in valid range for array
   */
  public static void set(final Object array, final int index, final Object value) throws IllegalArgumentException,
      ArrayIndexOutOfBoundsException {
    if (array == null) { throw new NullPointerException(); }

    if (array instanceof Object[]) {
      Class componentType = array.getClass().getComponentType();
      if (value != null && !componentType.isInstance(value)) {
        //
        throw new IllegalArgumentException("Cannot assign an instance of type " + value.getClass().getName()
                                           + " to array with component type " + componentType.getName());
      }
      ArrayManager.objectArrayChanged((Object[]) array, index, value);
    } else if (value instanceof Byte) {
      setByte(array, index, ((Byte) value).byteValue());
    } else if (value instanceof Short) {
      setShort(array, index, ((Short) value).shortValue());
    } else if (value instanceof Integer) {
      setInt(array, index, ((Integer) value).intValue());
    } else if (value instanceof Long) {
      setLong(array, index, ((Long) value).longValue());
    } else if (value instanceof Float) {
      setFloat(array, index, ((Float) value).floatValue());
    } else if (value instanceof Double) {
      setDouble(array, index, ((Double) value).doubleValue());
    } else if (value instanceof Character) {
      setChar(array, index, ((Character) value).charValue());
    } else if (value instanceof Boolean) {
      setBoolean(array, index, ((Boolean) value).booleanValue());
    } else {
      throw new IllegalArgumentException("Not an array type: " + array.getClass().getName());
    }
  }

  /**
   * Set boolean value in array
   * 
   * @param array Array
   * @param index Index in array
   * @param z New boolean value
   * @throws NullPointerException If array is null
   * @throws IllegalArgumentException If array is an unexpected array type
   * @throws ArrayIndexOutOfBoundsException If index is not in valid range for array
   */
  public static void setBoolean(final Object array, final int index, final boolean z) throws IllegalArgumentException,
      ArrayIndexOutOfBoundsException {
    if (array == null) { throw new NullPointerException(); }

    if (array instanceof boolean[]) {
      byte b = z ? (byte) 1 : (byte) 0;

      ArrayManager.byteOrBooleanArrayChanged(array, index, b);
    } else {
      throw new IllegalArgumentException();
    }
  }

  /**
   * Set byte value in array
   * 
   * @param array Array
   * @param index Index in array
   * @param b New byte value
   * @throws NullPointerException If array is null
   * @throws IllegalArgumentException If array is an unexpected array type
   * @throws ArrayIndexOutOfBoundsException If index is not in valid range for array
   */
  public static void setByte(final Object array, final int index, final byte b) throws IllegalArgumentException,
      ArrayIndexOutOfBoundsException {
    if (array == null) { throw new NullPointerException(); }

    if (array instanceof byte[]) {
      ArrayManager.byteOrBooleanArrayChanged(array, index, b);
    } else {
      setShort(array, index, b);
    }
  }

  /**
   * Set int value in array
   * 
   * @param array Array
   * @param index Index in array
   * @param c New int value
   * @throws NullPointerException If array is null
   * @throws IllegalArgumentException If array is an unexpected array type
   * @throws ArrayIndexOutOfBoundsException If index is not in valid range for array
   */
  public static void setChar(final Object array, final int index, final char c) throws IllegalArgumentException,
      ArrayIndexOutOfBoundsException {
    if (array == null) { throw new NullPointerException(); }

    if (array instanceof char[]) {
      ArrayManager.charArrayChanged((char[]) array, index, c);
    } else {
      setInt(array, index, c);
    }
  }

  /**
   * Set short value in array
   * 
   * @param array Array
   * @param index Index in array
   * @param s New short value
   * @throws NullPointerException If array is null
   * @throws IllegalArgumentException If array is an unexpected array type
   * @throws ArrayIndexOutOfBoundsException If index is not in valid range for array
   */
  public static void setShort(final Object array, final int index, final short s) throws IllegalArgumentException,
      ArrayIndexOutOfBoundsException {
    if (array == null) { throw new NullPointerException(); }

    if (array instanceof short[]) {
      ArrayManager.shortArrayChanged((short[]) array, index, s);
    } else {
      setInt(array, index, s);
    }
  }

  /**
   * Set int value in array
   * 
   * @param array Array
   * @param index Index in array
   * @param i New int value
   * @throws NullPointerException If array is null
   * @throws IllegalArgumentException If array is an unexpected array type
   * @throws ArrayIndexOutOfBoundsException If index is not in valid range for array
   */
  public static void setInt(final Object array, final int index, final int i) throws IllegalArgumentException,
      ArrayIndexOutOfBoundsException {
    if (array == null) { throw new NullPointerException(); }

    if (array instanceof int[]) {
      ArrayManager.intArrayChanged((int[]) array, index, i);
    } else {
      setLong(array, index, i);
    }
  }

  /**
   * Set long value in array
   * 
   * @param array Array
   * @param index Index in array
   * @param l New long value
   * @throws NullPointerException If array is null
   * @throws IllegalArgumentException If array is an unexpected array type
   * @throws ArrayIndexOutOfBoundsException If index is not in valid range for array
   */
  public static void setLong(final Object array, final int index, final long l) throws IllegalArgumentException,
      ArrayIndexOutOfBoundsException {
    if (array == null) { throw new NullPointerException(); }

    if (array instanceof long[]) {
      ArrayManager.longArrayChanged((long[]) array, index, l);
    } else {
      setFloat(array, index, l);
    }
  }

  /**
   * Set float value in array
   * 
   * @param array Array
   * @param index Index in array
   * @param f New float value
   * @throws NullPointerException If array is null
   * @throws IllegalArgumentException If array is an unexpected array type
   * @throws ArrayIndexOutOfBoundsException If index is not in valid range for array
   */
  public static void setFloat(final Object array, final int index, final float f) throws IllegalArgumentException,
      ArrayIndexOutOfBoundsException {
    if (array == null) { throw new NullPointerException(); }

    if (array instanceof float[]) {
      ArrayManager.floatArrayChanged((float[]) array, index, f);
    } else {
      setDouble(array, index, f);
    }
  }

  /**
   * Set double value in array
   * 
   * @param array Array
   * @param index Index in array
   * @param d New double value
   * @throws NullPointerException If array is null
   * @throws IllegalArgumentException If array is an unexpected array type
   * @throws ArrayIndexOutOfBoundsException If index is not in valid range for array
   */
  public static void setDouble(final Object array, final int index, final double d) throws IllegalArgumentException,
      ArrayIndexOutOfBoundsException {
    if (array == null) { throw new NullPointerException(); }

    if (array instanceof double[]) {
      ArrayManager.doubleArrayChanged((double[]) array, index, d);
    } else {
      throw new IllegalArgumentException();
    }
  }

  /**
   * Indicate that object in array changed
   * 
   * @param array The array
   * @param index The index into array
   * @param value The new value
   */
  public static void objectArrayChanged(final Object[] array, final int index, final Object value) {
    ArrayManager.objectArrayChanged(array, index, value);
  }

  /**
   * Indicate that short in array changed
   * 
   * @param array The array
   * @param index The index into array
   * @param value The new value
   */
  public static void shortArrayChanged(final short[] array, final int index, final short value) {
    ArrayManager.shortArrayChanged(array, index, value);
  }

  /**
   * Indicate that long in array changed
   * 
   * @param array The array
   * @param index The index into array
   * @param value The new value
   */
  public static void longArrayChanged(final long[] array, final int index, final long value) {
    ArrayManager.longArrayChanged(array, index, value);
  }

  /**
   * Indicate that int in array changed
   * 
   * @param array The array
   * @param index The index into array
   * @param value The new value
   */
  public static void intArrayChanged(final int[] array, final int index, final int value) {
    ArrayManager.intArrayChanged(array, index, value);
  }

  /**
   * Indicate that float in array changed
   * 
   * @param array The array
   * @param index The index into array
   * @param value The new value
   */
  public static void floatArrayChanged(final float[] array, final int index, final float value) {
    ArrayManager.floatArrayChanged(array, index, value);
  }

  /**
   * Indicate that double in array changed
   * 
   * @param array The array
   * @param index The index into array
   * @param value The new value
   */
  public static void doubleArrayChanged(final double[] array, final int index, final double value) {
    ArrayManager.doubleArrayChanged(array, index, value);
  }

  /**
   * Indicate that char in array changed
   * 
   * @param array The array
   * @param index The index into array
   * @param value The new value
   */
  public static void charArrayChanged(final char[] array, final int index, final char value) {
    ArrayManager.charArrayChanged(array, index, value);
  }

  /**
   * Indicate that byte or boolean in array changed
   * 
   * @param array The array
   * @param index The index into array
   * @param value The new value
   */
  public static void byteOrBooleanArrayChanged(final Object array, final int index, final byte value) {
    ArrayManager.byteOrBooleanArrayChanged(array, index, value);
  }

  /**
   * Handle System.arraycopy() semantics with managed arrays
   * 
   * @param src Source array
   * @param srcPos Start index in source
   * @param dest Destination array
   * @param destPos Destination start index
   * @param length Number of items to copy
   * @throws NullPointerException If src or dest is null
   */
  public static void arraycopy(final Object src, final int srcPos, final Object dest, final int destPos,
                               final int length) {
    ArrayManager.arraycopy(src, srcPos, dest, destPos, length);
  }

  /**
   * Get the TCO for an array
   * 
   * @param array The array instance
   * @return The TCObject
   */
  public static TCObject getObject(final Object array) {
    return ArrayManager.getObject(array);
  }

  /**
   * Copy char[]
   * 
   * @param src Source array
   * @param srcPos Start in src
   * @param dest Destination array
   * @param destPos Start in dest
   * @param length Number of items to copy
   * @param tco TCObject for dest array
   */
  public static void charArrayCopy(final char[] src, final int srcPos, final char[] dest, final int destPos,
                                   final int length, final TCObject tco) {
    ArrayManager.charArrayCopy(src, srcPos, dest, destPos, length, tco);
  }

  /**
   * Register an array with its TCO. It is an error to register an array that has already been registered.
   * 
   * @param array Array
   * @param obj TCObject
   * @throws NullPointerException if array or tco are null
   */
  public static void register(final Object array, final TCObject obj) {
    ArrayManager.register(array, obj);
  }

  /**
   * @return TCProperties
   */
  public static TCProperties getTCProperties() {
    return getManager().getTCProperties();
  }

  /**
   * Returns true if the field represented by the offset is a portable field, i.e., not static and not dso transient
   * 
   * @param pojo Object
   * @param fieldOffset The index
   * @return true if the field is portable and false otherwise
   */
  public static boolean isFieldPortableByOffset(final Object pojo, final long fieldOffset) {
    return getManager().isFieldPortableByOffset(pojo, fieldOffset);
  }

  //
  // public static void registerMBean(Object bean, ObjectName name) throws InstanceAlreadyExistsException,
  // MBeanRegistrationException, NotCompliantMBeanException {
  // getManager().registerMBean(bean, name);
  // }

  public static void waitForAllCurrentTransactionsToComplete() {
    getManager().waitForAllCurrentTransactionsToComplete();
  }

  public static MetaDataDescriptor createMetaDataDescriptor(String category) {
    return getManager().createMetaDataDescriptor(category);
  }

  public static SearchQueryResults executeQuery(String cachename, List queryStack, boolean includeKeys,
                                                boolean includeValues, Set<String> attributeSet,
                                                List<NVPair> sortAttributes, List<NVPair> aggregators, int maxResults,
                                                int batchSize) {
    return getManager().executeQuery(cachename, queryStack, includeKeys, includeValues, attributeSet, sortAttributes,
                                     aggregators, maxResults, batchSize);
  }

  public static NVPair createNVPair(String name, Object value) {
    return getManager().createNVPair(name, value);
  }

  /**
   * Begin lock
   * 
   * @param long lockID Lock identifier
   * @param type Lock type
   */
  public static void beginLock(final long lockID, final int type) {
    Manager mgr = getManager();
    LockID lock = mgr.generateLockIdentifier(lockID);
    mgr.lock(lock, LockLevel.fromInt(type));
  }

  /**
   * Try to begin lock
   * 
   * @param long lockID Lock identifier
   * @param type Lock type
   * @return True if lock was successful
   */
  public static boolean tryBeginLock(final long lockID, final int type) {
    Manager mgr = getManager();
    LockID lock = mgr.generateLockIdentifier(lockID);
    return mgr.tryLock(lock, LockLevel.fromInt(type));
  }

  /**
   * Try to begin lock within a specific timespan
   * 
   * @param lockID Lock identifier
   * @param type Lock type
   * @param timeoutInNanos Timeout in nanoseconds
   * @return True if lock was successful
   */
  public static boolean tryBeginLock(final long lockID, final int type, final long timeoutInNanos)
      throws InterruptedException {
    Manager mgr = getManager();
    LockID lock = mgr.generateLockIdentifier(lockID);
    return mgr.tryLock(lock, LockLevel.fromInt(type), timeoutInNanos / 1000000);
  }

  /**
   * Commit lock
   * 
   * @param long lockID Lock name
   */
  public static void commitLock(final long lockID, final int type) {
    Manager mgr = getManager();
    LockID lock = mgr.generateLockIdentifier(lockID);
    mgr.unlock(lock, LockLevel.fromInt(type));
  }

  public static void pinLock(final long lockID) {
    Manager mgr = getManager();
    LockID lock = mgr.generateLockIdentifier(lockID);
    mgr.pinLock(lock);
  }

  public static void unpinLock(final long lockID) {
    Manager mgr = getManager();
    LockID lock = mgr.generateLockIdentifier(lockID);
    mgr.unpinLock(lock);
  }

  /**
   * Check whether this lock is held by the current thread
   * 
   * @param lockId The lock ID
   * @param lockLevel The lock level
   * @return True if held by current thread
   */
  public static boolean isLockHeldByCurrentThread(final long lockId, final int lockLevel) {
    Manager mgr = getManager();
    LockID lock = mgr.generateLockIdentifier(lockId);
    return mgr.isLockedByCurrentThread(lock, LockLevel.fromInt(lockLevel));
  }

  public static void verifyCapability(String capability) {
    getManager().verifyCapability(capability);
  }

  public static void fireOperatorEvent(EventType coreOperatorEventLevel, EventSubsystem coreEventSubsytem,
                                       String eventMessage) {
    getManager().fireOperatorEvent(coreOperatorEventLevel, coreEventSubsytem, eventMessage);
  }

}
