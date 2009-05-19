/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.cluster.DsoCluster;
import com.tc.exception.TCClassNotFoundException;
import com.tc.logging.TCLogger;
import com.tc.management.beans.sessions.SessionMonitor;
import com.tc.object.ObjectID;
import com.tc.object.TCObject;
import com.tc.object.event.DmiManager;
import com.tc.object.loaders.ClassProvider;
import com.tc.object.loaders.NamedClassLoader;
import com.tc.object.lockmanager.api.LockLevel;
import com.tc.object.logging.InstrumentationLogger;
import com.tc.properties.TCProperties;

import java.lang.reflect.Field;

import javax.management.MBeanServer;

/**
 * The Manager interface
 */
public interface Manager {

  /** This class's class path: com/tc/object/bytecode/Manager */
  public static final String CLASS                       = "com/tc/object/bytecode/Manager";
  /** Bytecode type definition for this class */
  public static final String TYPE                        = "L" + CLASS + ";";

  public final static int    LOCK_TYPE_READ              = LockLevel.READ;
  public final static int    LOCK_TYPE_WRITE             = LockLevel.WRITE;
  public final static int    LOCK_TYPE_CONCURRENT        = LockLevel.CONCURRENT;
  public final static int    LOCK_TYPE_SYNCHRONOUS_WRITE = LockLevel.SYNCHRONOUS_WRITE;

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
   * Initialize the Manager for running tests
   */
  public void initForTests();

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
   */
  public Object lookupOrCreateRoot(String name, Object object);

  /**
   * Look up or create a new root object. Objects faulted in to arbitrary depth.
   *
   * @param name Root name
   * @param obj Root object to use if none exists yet
   * @return The root object actually used, may or may not == object
   */
  public Object lookupOrCreateRootNoDepth(String name, Object obj);

  /**
   * Create or replace root, typically used for replaceable roots.
   *
   * @param rootName Root name
   * @param object Root object
   * @return Root object used
   */
  public Object createOrReplaceRoot(String rootName, Object object);

  /**
   * Begin volatile lock
   *
   * @param tcObject TCObject to lock
   * @param fieldName Field name holding volatile object
   * @param type Lock type
   */
  public void beginVolatile(TCObject tcObject, String fieldName, int type);

  /**
   * Begin lock
   *
   * @param lockID Lock identifier
   * @param type Lock type
   * @param contextInfo
   */
  public void beginLock(String lockID, int type, String contextInfo);

  /**
   * Try to begin lock
   *
   * @param lockID Lock identifier
   * @param type Lock type
   * @return True if lock was successful
   */
  public boolean tryBeginLock(String lockID, int type);

  /**
   * Try to begin lock within a specific timespan
   *
   * @param lockID Lock identifier
   * @param type Lock type
   * @param timeoutInNanos Timeout in nanoseconds
   * @return True if lock was successful
   */
  public boolean tryBeginLock(String lockID, int type, long timeoutInNanos);

  /**
   * Commit volatile lock
   *
   * @param tcObject Volatile object TCObject
   * @param fieldName Field holding the volatile object
   */
  public void commitVolatile(TCObject tcObject, String fieldName);

  /**
   * Commit lock
   *
   * @param lockName Lock name
   */
  public void commitLock(String lockName);

  public void pinLock(String lockName);
  
  public void unpinLock(String lockName);
  
  public void evictLock(String lockName);
  
  /**
   * Look up object by ID, faulting into the JVM if necessary
   *
   * @param id Object identifier
   * @return The actual object
   */
  public Object lookupObject(ObjectID id) throws ClassNotFoundException;

  /**
   * Look up object by ID, faulting into the JVM if necessary, This method also passes the parent Object context so that
   * more intelligent prefetching is possible at the L2.
   *
   * @param id Object identifier of the object we are looking up
   * @param parentContext Object identifier of the parent object
   * @return The actual object
   * @throws TCClassNotFoundException If a class is not found during faulting
   */
  public Object lookupObject(ObjectID id, ObjectID parentContext) throws ClassNotFoundException;

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

  /**
   * @param pojo Object instance
   * @return TCObject for pojo
   */
  public TCObject shareObjectIfNecessary(Object pojo);

  /**
   * Perform notify on obj
   *
   * @param obj Instance
   */
  public void objectNotify(Object obj);

  /**
   * Perform notifyAll on obj
   *
   * @param obj Instance
   */
  public void objectNotifyAll(Object obj);

  /**
   * Perform untimed wait on obj
   *
   * @param obj Instance
   */
  public void objectWait(Object obj) throws InterruptedException;

  /**
   * Perform timed wait on obj
   *
   * @param obj Instance
   * @param millis Wait time
   */
  public void objectWait(Object obj, long millis) throws InterruptedException;

  /**
   * Perform timed wait on obj
   *
   * @param obj Instance
   * @param millis Wait time
   * @param nanos More wait time
   */
  public void objectWait(Object obj, long millis, int nanos) throws InterruptedException;

  /**
   * Enter synchronized monitor
   *
   * @param obj Object
   * @param type Lock type
   * @contextInfo contextInfo
   */
  public void monitorEnter(Object obj, int type, String contextInfo);

  /**
   * Exit synchronized monitor
   *
   * @param obj Object
   */
  public void monitorExit(Object obj);

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
   */
  public void logicalInvokeWithTransaction(Object object, Object lockObject, String methodName, Object[] params);

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
   */
  public Object lookupRoot(String name);

  /**
   * Check whether current context has write access
   *
   * @param context Context object
   * @throws com.tc.object.util.ReadOnlyException If in read-only transaction
   */
  public void checkWriteAccess(Object context);
  
  /**
   * Calculate a hash code for the object that will be the same on all nodes, i.e., that does not depend on
   * Object.hashCode(). For objects that override hashCode(), the object's hashCode() will be used; for literals that
   * use Object.hashCode(), like Class, a stable hash code will be computed. Note that for objects that override
   * hashCode() but that still base the result on Object.hashCode() the result of this method may still be unstable.
   */
  public int calculateDsoHashCode(Object obj);
  
  /**
   * @return true if obj is an instance of a {@link com.tc.object.LiteralValues literal type}, 
   * e.g., Class, Integer, etc.
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
   * Check whether an object is shared
   *
   * @param obj Instance
   * @return True if shared
   */
  public boolean isDsoMonitored(Object obj);

  /**
   * Check whether dso MonitorExist is required
   *
   * @return True if required
   */
  public boolean isDsoMonitorEntered(Object obj);
  
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
   * Check whether an object is locked at this lockLevel
   *
   * @param obj Lock
   * @param lockLevel Lock level
   * @return True if locked at this level
   * @throws NullPointerException If obj is null
   */
  public boolean isLocked(Object obj, int lockLevel);

  /**
   * Try to enter monitor for specified object
   *
   * @param obj The object monitor
   * @param timeoutInNanos Timeout in nanoseconds
   * @param type The lock level
   * @return True if entered
   * @throws NullPointerException If obj is null
   */
  public boolean tryMonitorEnter(Object obj, int type, long timeoutInNanos);

  /**
   * Enter synchronized monitor (interruptibly)
   *
   * @param obj The object monitor
   * @param type The lock level
   * @throws InterruptedException If interrupted while entering or waiting
   * @throws NullPointerException If obj is null
   */
  public void monitorEnterInterruptibly(Object obj, int type) throws InterruptedException;

  /**
   * Get number of locks held locally on this object
   *
   * @param obj The lock object
   * @param lockLevel The lock level
   * @return Lock count
   * @throws NullPointerException If obj is null
   */
  public int localHeldCount(Object obj, int lockLevel);

  /**
   * Check whether this lock is held by the current thread
   *
   * @param obj The lock
   * @param lockLevel The lock level
   * @return True if held by current thread
   * @throws NullPointerException If obj is null
   */
  public boolean isHeldByCurrentThread(Object obj, int lockLevel);

  /**
   * Number in queue waiting on this lock
   *
   * @param obj The object
   * @return Number of waiters
   * @throws NullPointerException If obj is null
   */
  public int queueLength(Object obj);

  /**
   * Number in queue waiting on this wait()
   *
   * @param obj The object
   * @return Number of waiters
   * @throws NullPointerException If obj is null
   */
  public int waitLength(Object obj);

  /**
   * Get JVM Client identifier
   *
   * @return Client identifier
   */
  public String getClientID();

  /**
   * Get the named logger
   *
   * @param loggerName Logger name
   * @return The logger
   */
  public TCLogger getLogger(String loggerName);

  /**
   * Get the instrumentation logger
   */
  InstrumentationLogger getInstrumentationLogger();

  /**
   * @return Session monitor MBean
   */
  public SessionMonitor getHttpSessionMonitor();

  /**
   * @return TCProperties
   */
  public TCProperties getTCProperites();

  /**
   * @return DMI manager
   */
  public DmiManager getDmiManager();

  /**
   * Returns true if the field represented by the offset is a portable field, i.e., not static and not dso transient
   *
   * @param pojo Object
   * @param fieldOffset The index
   * @return true if the field is portable and false otherwise
   */
  public boolean isFieldPortableByOffset(Object pojo, long fieldOffset);

  /**
   * Returns true if the given object overrides hashCode() from java.lang.Object. Enum types are NOT considered to
   * override hashCode()
   */
  public boolean overridesHashCode(Object obj);

  /**
   * Begins a lock without associating any transaction context.
   */
  public void beginLockWithoutTxn(String lockID, int type);

  /**
   * Register a named classloader with Terracotta.
   * @param webAppName corresponds to the name of a web-application in the TC config, or null
   * if the classloader being registered is not associated with a web application.
   */
  public void registerNamedLoader(NamedClassLoader loader, String webAppName);

  /**
   * Get the ClassProvider associated with this Manager
   */
  public ClassProvider getClassProvider();

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

}
