/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.cluster.ClusterEventListener;
import com.tc.logging.TCLogger;
import com.tc.management.beans.sessions.SessionMonitorMBean;
import com.tc.object.ObjectID;
import com.tc.object.TCObject;
import com.tc.object.event.DmiManager;
import com.tc.object.lockmanager.api.LockLevel;
import com.tc.properties.TCProperties;

/**
 * The Manager interface
 */
public interface Manager {

  public static final String CLASS                = "com/tc/object/bytecode/Manager";
  public static final String TYPE                 = "L" + CLASS + ";";

  public final static int    LOCK_TYPE_READ       = LockLevel.READ;
  public final static int    LOCK_TYPE_WRITE      = LockLevel.WRITE;
  public final static int    LOCK_TYPE_CONCURRENT = LockLevel.CONCURRENT;

  public boolean isPhysicallyInstrumented(Class clazz);

  public Object deepCopy(Object source);

  public void init();

  public void stop();

  public Object lookupOrCreateRoot(String name, Object object);

  public Object lookupOrCreateRootNoDepth(String name, Object obj);

  public Object createOrReplaceRoot(String rootName, Object object);

  public void beginVolatile(TCObject tcObject, String fieldName, int type);

  public void beginLock(String lockID, int type);

  public boolean tryBeginLock(String lockID, int type);

  public void commitVolatile(TCObject tcObject, String fieldName);

  public void commitLock(String lockName);

  public Object lookupObject(ObjectID id);

  public TCObject lookupExistingOrNull(Object obj);

  public TCObject lookupOrCreate(Object obj);

  public TCObject shareObjectIfNecessary(Object pojo);

  public void objectNotify(Object obj);

  public void objectNotifyAll(Object obj);

  public void objectWait0(Object obj) throws InterruptedException;

  public void objectWait1(Object obj, long millis) throws InterruptedException;

  public void objectWait2(Object obj, long millis, int nanos) throws InterruptedException;

  public void monitorEnter(Object obj, int type);

  public void monitorExit(Object obj);

  public void logicalInvoke(Object object, String methodName, Object[] params);

  public void logicalInvokeWithTransaction(Object object, Object lockObject, String methodName, Object[] params);

  public boolean distributedMethodCall(Object receiver, String method, Object[] params);

  public void distributedMethodCallCommit();

  public Object lookupRoot(String name);

  public void checkWriteAccess(Object context);

  public boolean isManaged(Object object);

  public boolean isLogical(Object object);

  public boolean isRoot(String className, String fieldName);

  public void optimisticBegin();

  public void optimisticCommit();

  public void optimisticRollback();

  public boolean isLocked(Object obj);

  public boolean tryMonitorEnter(Object obj, int type);

  public int heldCount(Object obj, int lockLevel);

  public boolean isHeldByCurrentThread(Object obj, int lockLevel);

  public int queueLength(Object obj);

  public int waitLength(Object obj);

  public boolean isCreationInProgress();

  public String getClientID();

  public TCLogger getLogger(String loggerName);

  public SessionMonitorMBean getSessionMonitorMBean();

  public TCProperties getTCProperites();

  public void addClusterEventListener(ClusterEventListener cel);

  public DmiManager getDmiManager();

}
