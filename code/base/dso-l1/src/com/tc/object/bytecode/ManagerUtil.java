/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.cluster.ClusterEventListener;
import com.tc.exception.TCClassNotFoundException;
import com.tc.logging.TCLogger;
import com.tc.management.beans.sessions.SessionMonitorMBean;
import com.tc.object.ObjectID;
import com.tc.object.TCObject;
import com.tc.object.bytecode.hook.impl.ArrayManager;
import com.tc.object.bytecode.hook.impl.ClassProcessorHelper;
import com.tc.object.lockmanager.api.LockLevel;
import com.tc.properties.TCProperties;

/**
 * A bunch of static methods that make calling Manager method much easier from instrumented classes
 */
public class ManagerUtil {

  public static final String      CLASS        = "com/tc/object/bytecode/ManagerUtil";
  public static final String      TYPE         = "L" + CLASS + ";";

  private static final Manager    NULL_MANAGER = NullManager.getInstance();

  private static volatile boolean enabled      = false;

  public static void enable() {
    enabled = true;
  }

  private static Manager getManager() {
    if (!enabled) { return NULL_MANAGER; }

    if (ClassProcessorHelper.USE_GLOBAL_CONTEXT) {
      return GlobalManagerHolder.instance;
    } else {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      Manager rv = ClassProcessorHelper.getManager(loader);
      if (rv == null) { return NULL_MANAGER; }
      return rv;
    }
  }

  public static TCLogger getLogger(String loggerName) {
    return getManager().getLogger(loggerName);
  }

  public static boolean isPhysicallyInstrumented(Class clazz) {
    return getManager().isPhysicallyInstrumented(clazz);
  }

  public static void optimisticBegin() {
    getManager().optimisticBegin();
  }

  public static void optimisticCommit() {
    beginLock("test", LockLevel.WRITE);
    try {
      getManager().optimisticCommit();
    } catch (ClassNotFoundException e) {
      throw new TCClassNotFoundException(e);
    }
    commitLock("test");
  }

  public static void optimisticRollback() {
    getManager().optimisticRollback();
  }

  public static String getClientID() {
    return getManager().getClientID();
  }

  public static Object deepCopy(Object pojo) {
    return getManager().deepCopy(pojo);
  }

  public static Object lookupOrCreateRoot(String name, Object object) {
    return getManager().lookupOrCreateRoot(name, object);
  }

  public static Object lookupOrCreateRootNoDepth(String name, Object obj) {
    return getManager().lookupOrCreateRootNoDepth(name, obj);
  }

  public static Object createOrReplaceRoot(String rootName, Object object) {
    return getManager().createOrReplaceRoot(rootName, object);
  }

  public static void beginVolatileByOffset(Object pojo, long fieldOffset, int type) {
    TCObject tcObject = lookupExistingOrNull(pojo);
    if (tcObject == null) { throw new NullPointerException("beginVolatileByOffset called on a null TCObject"); }

    beginVolatile(tcObject, tcObject.getFieldNameByOffset(fieldOffset), type);
  }

  public static void commitVolatileByOffset(Object pojo, long fieldOffset) {
    TCObject tcObject = lookupExistingOrNull(pojo);
    if (tcObject == null) { throw new NullPointerException("commitVolatileByOffset called on a null TCObject"); }

    commitVolatile(tcObject, tcObject.getFieldNameByOffset(fieldOffset));
  }

  public static void beginVolatile(TCObject tcObject, String fieldName, int type) {
    getManager().beginVolatile(tcObject, fieldName, type);
  }

  public static void beginLock(String lockID, int type) {
    getManager().beginLock(lockID, type);
  }

  public static boolean tryBeginLock(String lockID, int type) {
    return getManager().tryBeginLock(lockID, type);
  }

  public static void commitVolatile(TCObject tcObject, String fieldName) {
    getManager().commitVolatile(tcObject, fieldName);
  }

  public static void commitLock(String lockID) {
    getManager().commitLock(lockID);
  }

  public static TCObject lookupExistingOrNull(Object pojo) {
    return getManager().lookupExistingOrNull(pojo);
  }

  public static TCObject shareObjectIfNecessary(Object pojo) {
    return getManager().shareObjectIfNecessary(pojo);
  }

  public static void logicalInvoke(Object object, String methodName, Object[] params) {
    getManager().logicalInvoke(object, methodName, params);
  }

  public static void logicalInvokeWithTransaction(Object object, Object lockObject, String methodName, Object[] params) {
    getManager().logicalInvokeWithTransaction(object, lockObject, methodName, params);
  }

  public static void distributedMethodCallCommit() {
    getManager().distributedMethodCallCommit();
  }

  public static boolean prunedDistributedMethodCall(Object receiver, String method, Object[] params) {
    return getManager().distributedMethodCall(receiver, method, params, false);
  }

  public static boolean distributedMethodCall(Object receiver, String method, Object[] params) {
    return getManager().distributedMethodCall(receiver, method, params, true);
  }

  public static Object lookupRoot(String name) {
    return getManager().lookupRoot(name);
  }

  public static Object lookupObject(ObjectID id) {
    try {
      return getManager().lookupObject(id);
    } catch (ClassNotFoundException e) {
      throw new TCClassNotFoundException(e);
    }
  }

  public static TCObject lookupOrCreate(Object obj) {
    return getManager().lookupOrCreate(obj);
  }

  public static void checkWriteAccess(Object context) {
    getManager().checkWriteAccess(context);
  }

  public static boolean isManaged(Object obj) {
    return getManager().isManaged(obj);
  }

  public static boolean isLogical(Object obj) {
    return getManager().isLogical(obj);
  }

  public static boolean isRoot(String className, String fieldName) {
    return getManager().isRoot(className, fieldName);
  }

  public static void objectNotify(Object obj) {
    getManager().objectNotify(obj);
  }

  public static void objectNotifyAll(Object obj) {
    getManager().objectNotifyAll(obj);
  }

  public static void objectWait0(Object obj) throws InterruptedException {
    getManager().objectWait0(obj);
  }

  public static void objectWait1(Object obj, long millis) throws InterruptedException {
    getManager().objectWait1(obj, millis);
  }

  public static void objectWait2(Object obj, long millis, int nanos) throws InterruptedException {
    getManager().objectWait2(obj, millis, nanos);
  }

  public static void monitorEnter(Object obj, int type) {
    getManager().monitorEnter(obj, type);
  }

  public static void monitorExit(Object obj) {
    getManager().monitorExit(obj);
  }

  public static boolean isLocked(Object obj) {
    return getManager().isLocked(obj);
  }

  public static boolean tryMonitorEnter(Object obj, int type) {
    return getManager().tryMonitorEnter(obj, type);
  }

  public static boolean isHeldByCurrentThread(Object obj, int lockLevel) {
    return getManager().isHeldByCurrentThread(obj, lockLevel);
  }

  public static int queueLength(Object obj) {
    return getManager().queueLength(obj);
  }

  public static int waitLength(Object obj) {
    return getManager().waitLength(obj);
  }

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

  public static Object get(Object array, int index) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
    return ArrayManager.get(array, index);
  }

  public static void set(Object array, int index, Object value) throws IllegalArgumentException,
      ArrayIndexOutOfBoundsException {
    if (array == null) throw new NullPointerException();

    if (array instanceof Object[]) {
      if (!array.getClass().getComponentType().isInstance(value)) throw new IllegalArgumentException();
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

  public static void setBoolean(Object array, int index, boolean z) throws IllegalArgumentException,
      ArrayIndexOutOfBoundsException {
    if (array == null) throw new NullPointerException();

    if (array instanceof boolean[]) {
      byte b = z ? (byte) 1 : (byte) 0;

      ArrayManager.byteOrBooleanArrayChanged(array, index, b);
    } else throw new IllegalArgumentException();
  }

  public static void setByte(Object array, int index, byte b) throws IllegalArgumentException,
      ArrayIndexOutOfBoundsException {
    if (array == null) throw new NullPointerException();

    if (array instanceof byte[]) ArrayManager.byteOrBooleanArrayChanged(array, index, b);
    else setShort(array, index, b);
  }

  public static void setChar(Object array, int index, char c) throws IllegalArgumentException,
      ArrayIndexOutOfBoundsException {
    if (array == null) throw new NullPointerException();

    if (array instanceof char[]) ArrayManager.charArrayChanged((char[]) array, index, c);
    else setInt(array, index, c);
  }

  public static void setShort(Object array, int index, short s) throws IllegalArgumentException,
      ArrayIndexOutOfBoundsException {
    if (array == null) throw new NullPointerException();

    if (array instanceof short[]) ArrayManager.shortArrayChanged((short[]) array, index, s);
    else setInt(array, index, s);
  }

  public static void setInt(Object array, int index, int i) throws IllegalArgumentException,
      ArrayIndexOutOfBoundsException {
    if (array == null) throw new NullPointerException();

    if (array instanceof int[]) ArrayManager.intArrayChanged((int[]) array, index, i);
    else setLong(array, index, i);
  }

  public static void setLong(Object array, int index, long l) throws IllegalArgumentException,
      ArrayIndexOutOfBoundsException {
    if (array == null) throw new NullPointerException();

    if (array instanceof long[]) ArrayManager.longArrayChanged((long[]) array, index, l);
    else setFloat(array, index, l);
  }

  public static void setFloat(Object array, int index, float f) throws IllegalArgumentException,
      ArrayIndexOutOfBoundsException {
    if (array == null) throw new NullPointerException();

    if (array instanceof float[]) ArrayManager.floatArrayChanged((float[]) array, index, f);
    else setDouble(array, index, f);
  }

  public static void setDouble(Object array, int index, double d) throws IllegalArgumentException,
      ArrayIndexOutOfBoundsException {
    if (array == null) throw new NullPointerException();

    if (array instanceof double[]) ArrayManager.doubleArrayChanged((double[]) array, index, d);
    else throw new IllegalArgumentException();
  }

  public static void objectArrayChanged(Object[] array, int index, Object value) {
    ArrayManager.objectArrayChanged(array, index, value);
  }

  public static void shortArrayChanged(short[] array, int index, short value) {
    ArrayManager.shortArrayChanged(array, index, value);
  }

  public static void longArrayChanged(long[] array, int index, long value) {
    ArrayManager.longArrayChanged(array, index, value);
  }

  public static void intArrayChanged(int[] array, int index, int value) {
    ArrayManager.intArrayChanged(array, index, value);
  }

  public static void floatArrayChanged(float[] array, int index, float value) {
    ArrayManager.floatArrayChanged(array, index, value);
  }

  public static void doubleArrayChanged(double[] array, int index, double value) {
    ArrayManager.doubleArrayChanged(array, index, value);
  }

  public static void charArrayChanged(char[] array, int index, char value) {
    ArrayManager.charArrayChanged(array, index, value);
  }

  public static void byteOrBooleanArrayChanged(Object array, int index, byte value) {
    ArrayManager.byteOrBooleanArrayChanged(array, index, value);
  }

  public static void arraycopy(Object src, int srcPos, Object dest, int destPos, int length) {
    ArrayManager.arraycopy(src, srcPos, dest, destPos, length);
  }

  public static TCObject getObject(Object array) {
    return ArrayManager.getObject(array);
  }

  public static void charArrayCopy(char[] src, int srcPos, char[] dest, int destPos, int length, TCObject tco) {
    ArrayManager.charArrayCopy(src, srcPos, dest, destPos, length, tco);
  }

  public static void register(Object pojo, TCObject obj) {
    ArrayManager.register(pojo, obj);
  }

  public static SessionMonitorMBean getSessionMonitorMBean() {
    return getManager().getSessionMonitorMBean();
  }

  public static TCProperties getTCProperties() {
    return getManager().getTCProperites();
  }

  public static void addClusterEventListener(ClusterEventListener cel) {
    getManager().addClusterEventListener(cel);
  }

  public static int getSessionLockType(String appName) {
    return ClassProcessorHelper.getSessionLockType(appName);
  }

}
