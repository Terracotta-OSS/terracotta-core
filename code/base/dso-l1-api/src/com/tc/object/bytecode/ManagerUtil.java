/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

// import com.partitions.TCNoPartitionError;
import com.tc.cluster.ClusterEventListener;
import com.tc.exception.TCClassNotFoundException;
import com.tc.logging.TCLogger;
import com.tc.management.beans.sessions.SessionMonitorMBean;
import com.tc.object.ObjectID;
import com.tc.object.TCObject;
import com.tc.object.bytecode.hook.impl.ArrayManager;
import com.tc.object.bytecode.hook.impl.ClassProcessorHelper;
import com.tc.object.lockmanager.api.LockLevel;
import com.tc.object.partitions.PartitionManager;
import com.tc.properties.TCProperties;

import java.lang.reflect.Field;

/**
 * A bunch of static methods that make calling Manager method much easier from instrumented classes
 */
public class ManagerUtil {

  /** This class name */
  public static final String      CLASS        = "com/tc/object/bytecode/ManagerUtil";
  /** This class type */
  public static final String      TYPE         = "L" + CLASS + ";";

  private static final Manager    NULL_MANAGER = NullManager.getInstance();

  private static volatile boolean enabled      = false;

  /**
   * Called when initialization has proceeded enough that the Manager can be used.
   */
  public static void enable() {
    enabled = true;
  }

  public static Manager getManager() {
    if (!enabled) { return NULL_MANAGER; }
    Manager rv = null;
    if (ClassProcessorHelper.USE_GLOBAL_CONTEXT) {
      return GlobalManagerHolder.instance;
    } else if (ClassProcessorHelper.USE_PARTITIONED_CONTEXT) {
      rv = PartitionManager.getPartitionManager();
    } else {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      rv = ClassProcessorHelper.getManager(loader);
    }
    if (rv == null) { return NULL_MANAGER; }
    return rv;
  }

  /**
   * Get the named logger
   *
   * @param name Logger name
   * @return The logger
   */
  public static TCLogger getLogger(String loggerName) {
    return getManager().getLogger(loggerName);
  }

  /**
   * Determine whether this class is physically instrumented
   *
   * @param clazz Class
   * @return True if physically instrumented
   */
  public static boolean isPhysicallyInstrumented(Class clazz) {
    return getManager().isPhysicallyInstrumented(clazz);
  }

  /**
   * Begin an optimistic transaction
   */
  public static void optimisticBegin() {
    getManager().optimisticBegin();
  }

  /**
   * Commit an optimistic transaction
   *
   * @throws ClassNotFoundException If class not found while faulting in object
   */
  public static void optimisticCommit() {
    beginLock("test", LockLevel.WRITE);
    try {
      getManager().optimisticCommit();
    } catch (ClassNotFoundException e) {
      throw new TCClassNotFoundException(e);
    }
    commitLock("test");
  }

  /**
   * Rollback an optimistic transaction
   */
  public static void optimisticRollback() {
    getManager().optimisticRollback();
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
   * Deep copy the source object graph
   *
   * @param source Source object
   * @return The copy
   */
  public static Object deepCopy(Object pojo) {
    return getManager().deepCopy(pojo);
  }

  /**
   * Look up or create a new root object
   *
   * @param name Root name
   * @param object Root object to use if none exists yet
   * @return The root object actually used, may or may not == object
   */
  public static Object lookupOrCreateRoot(String name, Object object) {
    return getManager().lookupOrCreateRoot(name, object);
  }

  /**
   * Look up or create a new root object. Objects faulted in to arbitrary depth.
   *
   * @param name Root name
   * @param obj Root object to use if none exists yet
   * @return The root object actually used, may or may not == object
   */
  public static Object lookupOrCreateRootNoDepth(String name, Object obj) {
    return getManager().lookupOrCreateRootNoDepth(name, obj);
  }

  /**
   * Create or replace root, typically used for replaceable roots.
   *
   * @param rootName Root name
   * @param object Root object
   * @return Root object used
   */
  public static Object createOrReplaceRoot(String rootName, Object object) {
    return getManager().createOrReplaceRoot(rootName, object);
  }

  /**
   * Begin volatile lock by field offset in the class
   *
   * @param pojo Instance containing field
   * @param fieldOffset Field offset in pojo
   * @param type Lock level
   */
  public static void beginVolatileByOffset(Object pojo, long fieldOffset, int type) {
    TCObject tcObject = lookupExistingOrNull(pojo);
    if (tcObject == null) { throw new NullPointerException("beginVolatileByOffset called on a null TCObject"); }

    beginVolatile(tcObject, tcObject.getFieldNameByOffset(fieldOffset), type);
  }

  /**
   * Commit volatile lock by field offset in the class
   *
   * @param pojo Instance containing field
   * @param fieldOffset Field offset in pojo
   */
  public static void commitVolatileByOffset(Object pojo, long fieldOffset) {
    TCObject tcObject = lookupExistingOrNull(pojo);
    if (tcObject == null) { throw new NullPointerException("commitVolatileByOffset called on a null TCObject"); }

    commitVolatile(tcObject, tcObject.getFieldNameByOffset(fieldOffset));
  }

  /**
   * Begin volatile lock
   *
   * @param tcObject TCObject to lock
   * @param fieldName Field name holding volatile object
   * @param type Lock type
   */
  public static void beginVolatile(TCObject tcObject, String fieldName, int type) {
    getManager().beginVolatile(tcObject, fieldName, type);
  }

  /**
   * Begin lock
   *
   * @param lockID Lock identifier
   * @param type Lock type
   */
  public static void beginLock(String lockID, int type) {
    getManager().beginLock(lockID, type);
  }

  /**
   * Begin lock
   *
   * @param lockID Lock identifier
   * @param type Lock type
   * @param contextInfo
   */
  public static void beginLockWithContextInfo(String lockID, int type, String contextInfo) {
    getManager().beginLock(lockID, type, contextInfo);
  }

  /**
   * Try to begin lock
   *
   * @param lockID Lock identifier
   * @param type Lock type
   * @return True if lock was successful
   */
  public static boolean tryBeginLock(String lockID, int type) {
    return getManager().tryBeginLock(lockID, type);
  }

  /**
   * Commit volatile lock
   *
   * @param tcObject Volatile object TCObject
   * @param fieldName Field holding the volatile object
   */
  public static void commitVolatile(TCObject tcObject, String fieldName) {
    getManager().commitVolatile(tcObject, fieldName);
  }

  /**
   * Commit lock
   *
   * @param lockID Lock name
   */
  public static void commitLock(String lockID) {
    getManager().commitLock(lockID);
  }

  /**
   * Find managed object, which may be null
   *
   * @param pojo The object instance
   * @return The TCObject
   */
  public static TCObject lookupExistingOrNull(Object pojo) {
    return getManager().lookupExistingOrNull(pojo);
  }

  /**
   * @param pojo Object instance
   * @return TCObject for pojo
   */
  public static TCObject shareObjectIfNecessary(Object pojo) {
    return getManager().shareObjectIfNecessary(pojo);
  }

  /**
   * Perform invoke on logical managed object
   *
   * @param object The object
   * @param methodName The method to call
   * @param params The parameters to the method
   */
  public static void logicalInvoke(Object object, String methodName, Object[] params) {
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
  public static void logicalInvokeWithTransaction(Object object, Object lockObject, String methodName, Object[] params) {
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
  public static boolean prunedDistributedMethodCall(Object receiver, String method, Object[] params) {
    return getManager().distributedMethodCall(receiver, method, params, false);
  }

  /**
   * Perform distributed method call on all nodes
   *
   * @param receiver The receiver object
   * @param method The method to call
   * @param params The parameter values
   */
  public static boolean distributedMethodCall(Object receiver, String method, Object[] params) {
    return getManager().distributedMethodCall(receiver, method, params, true);
  }

  /**
   * Lookup root by name
   *
   * @param name Name of root
   * @return Root object
   */
  public static Object lookupRoot(String name) {
    return getManager().lookupRoot(name);
  }

  /**
   * Look up object by ID, faulting into the JVM if necessary
   *
   * @param id Object identifier
   * @return The actual object
   * @throws TCClassNotFoundException If a class is not found during faulting
   */
  public static Object lookupObject(ObjectID id) {
    try {
      return getManager().lookupObject(id);
    } catch (ClassNotFoundException e) {
      throw new TCClassNotFoundException(e);
    }
  }

  /**
   * Look up object by ID, faulting into the JVM if necessary, This method also passes the parent Object context so that
   * more intelligent prefetching is possible at the L2. XXX::FIXME:: This method is not called lookupObject() coz
   * ManagerHelperFactory doesn't allow method overloading.
   *
   * @param id Object identifier of the object we are looking up
   * @param parentContext Object identifier of the parent object
   * @return The actual object
   * @throws TCClassNotFoundException If a class is not found during faulting
   */
  public static Object lookupObjectWithParentContext(ObjectID id, ObjectID parentContext) {
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
  public static TCObject lookupOrCreate(Object obj) {
    return getManager().lookupOrCreate(obj);
  }

  /**
   * Check whether current context has write access
   *
   * @param context Context object
   * @throws com.tc.object.util.ReadOnlyException If in read-only transaction
   */
  public static void checkWriteAccess(Object context) {
    getManager().checkWriteAccess(context);
  }

  /**
   * Check whether an object is managed
   *
   * @param obj Instance
   * @return True if managed
   */
  public static boolean isManaged(Object obj) {
    return getManager().isManaged(obj);
  }

  /**
   * Check whether an object is shared
   *
   * @param obj Instance
   * @return True if shared
   */
  public static boolean isDsoMonitored(Object obj) {
    return getManager().isDsoMonitored(obj);
  }

  /**
   * Check whether dso MonitorExist is required
   *
   * @return True if required
   */
  public static boolean isDsoMonitorEntered(Object obj) {
    return getManager().isDsoMonitorEntered(obj);
  }

  /**
   * Check whether object is logically instrumented
   *
   * @param obj Instance
   * @return True if logically instrumented
   */
  public static boolean isLogical(Object obj) {
    return getManager().isLogical(obj);
  }

  /**
   * Check whether field is a root
   *
   * @param field Field
   * @return True if root
   */
  public static boolean isRoot(Field field) {
    return getManager().isRoot(field);
  }

  /**
   * Perform notify on obj
   *
   * @param obj Instance
   */
  public static void objectNotify(Object obj) {
    getManager().objectNotify(obj);
  }

  /**
   * Perform notifyAll on obj
   *
   * @param obj Instance
   */
  public static void objectNotifyAll(Object obj) {
    getManager().objectNotifyAll(obj);
  }

  /**
   * Perform untimed wait on obj
   *
   * @param obj Instance
   */
  public static void objectWait0(Object obj) throws InterruptedException {
    getManager().objectWait0(obj);
  }

  /**
   * Perform timed wait on obj
   *
   * @param obj Instance
   * @param millis Wait time
   */
  public static void objectWait1(Object obj, long millis) throws InterruptedException {
    getManager().objectWait1(obj, millis);
  }

  /**
   * Perform timed wait on obj
   *
   * @param obj Instance
   * @param millis Wait time
   * @param nonas More wait time
   */
  public static void objectWait2(Object obj, long millis, int nanos) throws InterruptedException {
    getManager().objectWait2(obj, millis, nanos);
  }

  /**
   * Enter synchronized monitor
   *
   * @param obj Object
   * @param type Lock type
   */
  public static void monitorEnter(Object obj, int type) {
    getManager().monitorEnter(obj, type);
  }

  /**
   * Enter synchronized monitor
   *
   * @param obj Object
   * @param type Lock type
   * @param configText Configuration text of the lock
   */
  public static void monitorEnterWithContextInfo(Object obj, int type, String contextInfo) {
    getManager().monitorEnter(obj, type, contextInfo);
  }

  /**
   * Exit synchronized monitor
   *
   * @param obj Object
   */
  public static void monitorExit(Object obj) {
    getManager().monitorExit(obj);
  }

  /**
   * Check whether an object is locked at this lockLevel
   *
   * @param obj Lock
   * @param lockLevel Lock level
   * @return True if locked at this level
   * @throws NullPointerException If obj is null
   */
  public static boolean isLocked(Object obj, int lockLevel) {
    return getManager().isLocked(obj, lockLevel);
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
  public static boolean tryMonitorEnter(Object obj, long timeoutInNanos, int type) {
    return getManager().tryMonitorEnter(obj, timeoutInNanos, type);
  }

  /**
   * Get number of locks held locally on this object
   *
   * @param obj The lock object
   * @param lockLevel The lock level
   * @return Lock count
   * @throws NullPointerException If obj is null
   */
  public static int localHeldCount(Object obj, int lockLevel) {
    return getManager().localHeldCount(obj, lockLevel);
  }

  /**
   * Check whether this lock is held by the current thread
   *
   * @param obj The lock
   * @param lockLevel The lock level
   * @return True if held by current thread
   * @throws NullPointerException If obj is null
   */
  public static boolean isHeldByCurrentThread(Object obj, int lockLevel) {
    return getManager().isHeldByCurrentThread(obj, lockLevel);
  }

  /**
   * Number in queue waiting on this lock
   *
   * @param obj The object
   * @return Number of waiters
   * @throws NullPointerException If obj is null
   */
  public static int queueLength(Object obj) {
    return getManager().queueLength(obj);
  }

  /**
   * Number in queue waiting on this wait()
   *
   * @param obj The object
   * @return Number of waiters
   * @throws NullPointerException If obj is null
   */
  public static int waitLength(Object obj) {
    return getManager().waitLength(obj);
  }

  /**
   * Check whether a creation is in progress. This flag is set on a per-thread basis while hydrating an object from DNA.
   *
   * @return True if in progress
   */
  public static boolean isCreationInProgress() {
    return getManager().isCreationInProgress();
  }

  private ManagerUtil() {
    // not for public instantiation
  }

  private static class GlobalManagerHolder {
    static final Manager instance;
    static {
      instance = ClassProcessorHelper.getGlobalManager();
    }
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
  public static Object get(Object array, int index) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
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
  public static void setImpl(Object array, int index, Object value) throws IllegalArgumentException,
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
  public static void set(Object array, int index, Object value) throws IllegalArgumentException,
      ArrayIndexOutOfBoundsException {
    if (array == null) throw new NullPointerException();

    if (array instanceof Object[]) {
      Class componentType = array.getClass().getComponentType();
      if (value != null && !componentType.isInstance(value)) {
        //
        throw new IllegalArgumentException("Cannot assign an instance of type " + value.getClass().getName()
                                           + " to array with component type " + componentType.getName());
      }
      ArrayManager.objectArrayChanged((Object[]) array, index, value);
    } else if (value instanceof Byte) setByte(array, index, ((Byte) value).byteValue());
    else if (value instanceof Short) setShort(array, index, ((Short) value).shortValue());
    else if (value instanceof Integer) setInt(array, index, ((Integer) value).intValue());
    else if (value instanceof Long) setLong(array, index, ((Long) value).longValue());
    else if (value instanceof Float) setFloat(array, index, ((Float) value).floatValue());
    else if (value instanceof Double) setDouble(array, index, ((Double) value).doubleValue());
    else if (value instanceof Character) setChar(array, index, ((Character) value).charValue());
    else if (value instanceof Boolean) setBoolean(array, index, ((Boolean) value).booleanValue());
    else throw new IllegalArgumentException("Not an array type: " + array.getClass().getName());
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
  public static void setBoolean(Object array, int index, boolean z) throws IllegalArgumentException,
      ArrayIndexOutOfBoundsException {
    if (array == null) throw new NullPointerException();

    if (array instanceof boolean[]) {
      byte b = z ? (byte) 1 : (byte) 0;

      ArrayManager.byteOrBooleanArrayChanged(array, index, b);
    } else throw new IllegalArgumentException();
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
  public static void setByte(Object array, int index, byte b) throws IllegalArgumentException,
      ArrayIndexOutOfBoundsException {
    if (array == null) throw new NullPointerException();

    if (array instanceof byte[]) ArrayManager.byteOrBooleanArrayChanged(array, index, b);
    else setShort(array, index, b);
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
  public static void setChar(Object array, int index, char c) throws IllegalArgumentException,
      ArrayIndexOutOfBoundsException {
    if (array == null) throw new NullPointerException();

    if (array instanceof char[]) ArrayManager.charArrayChanged((char[]) array, index, c);
    else setInt(array, index, c);
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
  public static void setShort(Object array, int index, short s) throws IllegalArgumentException,
      ArrayIndexOutOfBoundsException {
    if (array == null) throw new NullPointerException();

    if (array instanceof short[]) ArrayManager.shortArrayChanged((short[]) array, index, s);
    else setInt(array, index, s);
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
  public static void setInt(Object array, int index, int i) throws IllegalArgumentException,
      ArrayIndexOutOfBoundsException {
    if (array == null) throw new NullPointerException();

    if (array instanceof int[]) ArrayManager.intArrayChanged((int[]) array, index, i);
    else setLong(array, index, i);
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
  public static void setLong(Object array, int index, long l) throws IllegalArgumentException,
      ArrayIndexOutOfBoundsException {
    if (array == null) throw new NullPointerException();

    if (array instanceof long[]) ArrayManager.longArrayChanged((long[]) array, index, l);
    else setFloat(array, index, l);
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
  public static void setFloat(Object array, int index, float f) throws IllegalArgumentException,
      ArrayIndexOutOfBoundsException {
    if (array == null) throw new NullPointerException();

    if (array instanceof float[]) ArrayManager.floatArrayChanged((float[]) array, index, f);
    else setDouble(array, index, f);
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
  public static void setDouble(Object array, int index, double d) throws IllegalArgumentException,
      ArrayIndexOutOfBoundsException {
    if (array == null) throw new NullPointerException();

    if (array instanceof double[]) ArrayManager.doubleArrayChanged((double[]) array, index, d);
    else throw new IllegalArgumentException();
  }

  /**
   * Indicate that object in array changed
   *
   * @param array The array
   * @param index The index into array
   * @param value The new value
   */
  public static void objectArrayChanged(Object[] array, int index, Object value) {
    ArrayManager.objectArrayChanged(array, index, value);
  }

  /**
   * Indicate that short in array changed
   *
   * @param array The array
   * @param index The index into array
   * @param value The new value
   */
  public static void shortArrayChanged(short[] array, int index, short value) {
    ArrayManager.shortArrayChanged(array, index, value);
  }

  /**
   * Indicate that long in array changed
   *
   * @param array The array
   * @param index The index into array
   * @param value The new value
   */
  public static void longArrayChanged(long[] array, int index, long value) {
    ArrayManager.longArrayChanged(array, index, value);
  }

  /**
   * Indicate that int in array changed
   *
   * @param array The array
   * @param index The index into array
   * @param value The new value
   */
  public static void intArrayChanged(int[] array, int index, int value) {
    ArrayManager.intArrayChanged(array, index, value);
  }

  /**
   * Indicate that float in array changed
   *
   * @param array The array
   * @param index The index into array
   * @param value The new value
   */
  public static void floatArrayChanged(float[] array, int index, float value) {
    ArrayManager.floatArrayChanged(array, index, value);
  }

  /**
   * Indicate that double in array changed
   *
   * @param array The array
   * @param index The index into array
   * @param value The new value
   */
  public static void doubleArrayChanged(double[] array, int index, double value) {
    ArrayManager.doubleArrayChanged(array, index, value);
  }

  /**
   * Indicate that char in array changed
   *
   * @param array The array
   * @param index The index into array
   * @param value The new value
   */
  public static void charArrayChanged(char[] array, int index, char value) {
    ArrayManager.charArrayChanged(array, index, value);
  }

  /**
   * Indicate that byte or boolean in array changed
   *
   * @param array The array
   * @param index The index into array
   * @param value The new value
   */
  public static void byteOrBooleanArrayChanged(Object array, int index, byte value) {
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
  public static void arraycopy(Object src, int srcPos, Object dest, int destPos, int length) {
    ArrayManager.arraycopy(src, srcPos, dest, destPos, length);
  }

  /**
   * Get the TCO for an array
   *
   * @param array The array instance
   * @return The TCObject
   */
  public static TCObject getObject(Object array) {
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
   * @param tcDest TCObject for dest array
   */
  public static void charArrayCopy(char[] src, int srcPos, char[] dest, int destPos, int length, TCObject tco) {
    ArrayManager.charArrayCopy(src, srcPos, dest, destPos, length, tco);
  }

  /**
   * Register an array with its TCO. It is an error to register an array that has already been registered.
   *
   * @param array Array
   * @param tco TCObject
   * @throws NullPointerException if array or tco are null
   */
  public static void register(Object pojo, TCObject obj) {
    ArrayManager.register(pojo, obj);
  }

  /**
   * @return Session monitor MBean
   */
  public static SessionMonitorMBean getSessionMonitorMBean() {
    return getManager().getSessionMonitorMBean();
  }

  /**
   * @return TCProperties
   */
  public static TCProperties getTCProperties() {
    return getManager().getTCProperites();
  }

  /**
   * Add listener for cluster events
   *
   * @param cel Listener
   */
  public static void addClusterEventListener(ClusterEventListener cel) {
    getManager().addClusterEventListener(cel);
  }

  /**
   * Get session lock type for the specified app (usually WRITE or SYNCHRONOUS_WRITE)
   *
   * @param appName Web app name
   * @return Lock type
   */
  public static int getSessionLockType(String appName) {
    return ClassProcessorHelper.getSessionLockType(appName);
  }

  /**
   * Returns true if the field represented by the offset is a portable field, i.e., not static and not dso transient
   *
   * @param pojo Object
   * @param fieldOffset The index
   * @return true if the field is portable and false otherwise
   */
  public static boolean isFieldPortableByOffset(Object pojo, long fieldOffset) {
    return getManager().isFieldPortableByOffset(pojo, fieldOffset);
  }
}
