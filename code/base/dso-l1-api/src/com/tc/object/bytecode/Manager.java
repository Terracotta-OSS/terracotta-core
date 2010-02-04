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
import com.tc.object.locks.LockID;
import com.tc.object.locks.LockLevel;
import com.tc.object.locks.TerracottaLocking;
import com.tc.object.logging.InstrumentationLogger;
import com.tc.properties.TCProperties;
import com.tc.statistics.StatisticRetrievalAction;

import java.lang.reflect.Field;

import javax.management.MBeanServer;

/**
 * The Manager interface
 */
public interface Manager extends TerracottaLocking {

  /** This class's class path: com/tc/object/bytecode/Manager */
  public static final String CLASS                       = "com/tc/object/bytecode/Manager";
  /** Bytecode type definition for this class */
  public static final String TYPE                        = "L" + CLASS + ";";

  public final static int    LOCK_TYPE_READ              = LockLevel.READ_LEVEL;
  public final static int    LOCK_TYPE_WRITE             = LockLevel.WRITE_LEVEL;
  public final static int    LOCK_TYPE_CONCURRENT        = LockLevel.CONCURRENT_LEVEL;
  public final static int    LOCK_TYPE_SYNCHRONOUS_WRITE = LockLevel.SYNCHRONOUS_WRITE_LEVEL;

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
   * Look up object by ID, faulting into the JVM if necessary
   * 
   * @param id Object identifier
   * @return The actual object
   */
  public Object lookupObject(ObjectID id) throws ClassNotFoundException;

  /**
   * Prefetch object by ID, faulting into the JVM if necessary, Async lookup and will not cause ObjectNotFoundException
   * like lookupObject. Non-existent objects are ignored by the server.
   * 
   * @param id Object identifier
   */
  public void preFetchObject(ObjectID id);

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
  public TCProperties getTCProperties();

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
   * Register a named classloader with Terracotta.
   * 
   * @param webAppName corresponds to the name of a web-application in the TC config, or null if the classloader being
   *        registered is not associated with a web application.
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

  public StatisticRetrievalAction getStatisticRetrievalActionInstance(String name);

  /**
   * Used by instrumented code to perform a clustered <code>monitorenter</code>.
   */
  public void monitorEnter(LockID lock, LockLevel level);

  /**
   * Used by instrumented code to perform a clustered <code>monitorexit</code>.
   * <p>
   * Implementations of this method should <em>prevent propagation of all
   * <code>Throwable</code> instances</em>. Instead <code>Throwable</code> instances are logged and the client VM is
   * then terminated. If you don't want this behavior then don't call this method.
   * <p>
   * This behavior is there to ensure that exceptions thrown during transaction commit or clustered unlocking do not
   * cause the thread to enter an infinite loop.
   * 
   * @see <a href="http://jira.terracotta.org/jira/browse/DEV-113">DEV-113</a>
   */
  public void monitorExit(LockID lock, LockLevel level);

  /**
   * Get the configuration for the given application name (ie. context path)
   * 
   * @return null if the given app is not configured for clustering
   */
  public SessionConfiguration getSessionConfiguration(String appName);
  
  /**
   * Used by BulkLoad to wait for all current transactions completed 
   * 
   */
  public void waitForAllCurrentTransactionsToComplete();
  
  /**
   * Registers a hook that will be called before shutting down this client
   */
  public void registerBeforeShutdownHook(Runnable beforeShutdownHook);
  
}
