/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.roots.impl;

import com.tc.abortable.AbortedOperationException;
import com.tc.net.GroupID;
import com.tc.object.ObjectID;
import com.tc.object.SerializationUtil;
import com.tc.object.TCObject;
import com.tc.object.bytecode.Manageable;
import com.tc.object.bytecode.NotClearable;
import com.tc.platform.PlatformService;
import com.terracotta.toolkit.abortable.ToolkitAbortableOperationException;
import com.terracotta.toolkit.object.TCToolkitObject;
import com.terracotta.toolkit.rejoin.PlatformServiceProvider;
import com.terracotta.toolkit.roots.ToolkitTypeRoot;

import java.util.HashMap;
import java.util.Map;

public class ToolkitTypeRootImpl<T extends TCToolkitObject> implements ToolkitTypeRoot<T>, Manageable, NotClearable {
  private transient volatile TCObject           tcManaged;
  private transient final Map<String, ObjectID> localCache = new HashMap<String, ObjectID>();
  private transient volatile GroupID            gid;
  private transient volatile Object             localResolveLock;

  private final PlatformService                 platformService;

  public ToolkitTypeRootImpl() {
    platformService = PlatformServiceProvider.getPlatformService();
  }

  @Override
  public void addClusteredObject(String name, T manageable) {
    synchronized (localResolveLock) {
      platformService.lookupOrCreate(manageable, gid);
      // TODO: write a test
      localCache.put(name, manageable.__tc_managed().getObjectID());
      logicalInvokePut(name, manageable);
    }
  }

  public void applyAdd(String key, ObjectID o) {
    synchronized (localResolveLock) {
      localCache.put(key, o);
    }
  }

  public void applyRemove(String o) {
    synchronized (localResolveLock) {
      localCache.remove(o);
    }
  }

  private void logicalInvokePut(String name, T manageable) {
    tcManaged.logicalInvoke(SerializationUtil.PUT, SerializationUtil.PUT_SIGNATURE, new Object[] { name, manageable });
  }

  @Override
  public T getClusteredObject(String name) {
    synchronized (localResolveLock) {
      ObjectID value = localCache.get(name);
      if (value != null) { return faultValue(value); }
      return null;
    }
  }

  private T faultValue(ObjectID value) {
    try {
      return (T) platformService.lookupObject(value);
    } catch (AbortedOperationException e) {
      throw new ToolkitAbortableOperationException(e);
    }
  }

  @Override
  public void removeClusteredObject(String name) {
    if (name == null) { throw new NullPointerException("Name is null"); }
    synchronized (localResolveLock) {
      Object value = localCache.remove(name);
      if (value != null) {
        logicalInvokeRemove(name);
      }
    }
  }

  private void logicalInvokeRemove(String name) {
    tcManaged.logicalInvoke(SerializationUtil.REMOVE, SerializationUtil.REMOVE_SIGNATURE, new Object[] { name });
  }

  @Override
  public void __tc_managed(TCObject t) {
    tcManaged = t;
    gid = new GroupID(tcManaged.getObjectID().getGroupID());
    localResolveLock = tcManaged.getResolveLock();
  }

  @Override
  public TCObject __tc_managed() {
    return tcManaged;
  }

  @Override
  public boolean __tc_isManaged() {
    return tcManaged != null;
  }
}
