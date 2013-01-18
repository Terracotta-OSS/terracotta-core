/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.roots.impl;

import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.internal.ToolkitInternal;
import org.terracotta.toolkit.internal.concurrent.locks.ToolkitLockTypeInternal;

import com.tc.abortable.AbortedOperationException;
import com.tc.net.GroupID;
import com.tc.platform.PlatformService;
import com.tc.platform.rejoin.RejoinLifecycleListener;
import com.terracotta.toolkit.abortable.ToolkitAbortableOperationException;
import com.terracotta.toolkit.concurrent.locks.ToolkitLockingApi;
import com.terracotta.toolkit.factory.ToolkitObjectFactory;
import com.terracotta.toolkit.object.AbstractDestroyableToolkitObject;
import com.terracotta.toolkit.object.TCToolkitObject;
import com.terracotta.toolkit.rejoin.RejoinAwareToolkitObject;
import com.terracotta.toolkit.roots.AggregateToolkitTypeRoot;
import com.terracotta.toolkit.roots.ToolkitTypeRoot;
import com.terracotta.toolkit.type.IsolatedClusteredObjectLookup;
import com.terracotta.toolkit.type.IsolatedToolkitTypeFactory;
import com.terracotta.toolkit.util.collections.WeakValueMap;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class AggregateIsolatedToolkitTypeRoot<T extends RejoinAwareToolkitObject, S extends TCToolkitObject> implements
    AggregateToolkitTypeRoot<T, S>, RejoinLifecycleListener, IsolatedClusteredObjectLookup<S> {

  private final ToolkitTypeRoot<S>[]             roots;
  private final IsolatedToolkitTypeFactory<T, S> isolatedTypeFactory;
  private final WeakValueMap<T>                  isolatedTypes;
  private final PlatformService                  platformService;
  private final String                           rootName;
  private volatile Set<String>                   currentKeys = Collections.EMPTY_SET;

  protected AggregateIsolatedToolkitTypeRoot(String rootName, ToolkitTypeRoot<S>[] roots,
                                             IsolatedToolkitTypeFactory<T, S> isolatedTypeFactory,
                                             WeakValueMap weakValueMap, PlatformService platformService) {
    this.rootName = rootName;
    this.roots = roots;
    this.isolatedTypeFactory = isolatedTypeFactory;
    this.isolatedTypes = weakValueMap;
    this.platformService = platformService;
    platformService.addRejoinLifecycleListener(this);
  }

  @Override
  public T getOrCreateToolkitType(ToolkitInternal toolkit, ToolkitObjectFactory factory, String name,
                                  Configuration configuration) {
    if (name == null) { throw new NullPointerException("'name' cannot be null"); }

    ToolkitObjectType type = factory.getManufacturedToolkitObjectType();
    lock(type, name);
    try {
      T isolatedType = isolatedTypes.get(name);
      if (isolatedType != null) {
        return isolatedType;
      } else {
        S clusteredObject = getToolkitTypeRoot(name).getClusteredObject(name);
        if (clusteredObject == null) {
          clusteredObject = isolatedTypeFactory.createTCClusteredObject(configuration);
          getToolkitTypeRoot(name).addClusteredObject(name, clusteredObject);
        }
        isolatedType = isolatedTypeFactory.createIsolatedToolkitType(factory, this, name, configuration,
                                                                     clusteredObject);
        isolatedTypes.put(name, isolatedType);
        return isolatedType;
      }

    } finally {
      unlock(type, name);
      try {
        platformService.waitForAllCurrentTransactionsToComplete();
      } catch (AbortedOperationException e) {
        throw new ToolkitAbortableOperationException(e);
      }
    }
  }

  @Override
  public void removeToolkitType(ToolkitObjectType toolkitObjectType, String name) {
    lock(toolkitObjectType, name);
    try {
      isolatedTypes.remove(name);
      getToolkitTypeRoot(name).removeClusteredObject(name);
    } finally {
      unlock(toolkitObjectType, name);
    }
  }

  private ToolkitTypeRoot<S> getToolkitTypeRoot(String name) {
    return roots[Math.abs(name.hashCode() % roots.length)];
  }

  private void lock(ToolkitObjectType toolkitObjectType, String name) {
    ToolkitLockingApi.lock(toolkitObjectType, name, ToolkitLockTypeInternal.WRITE, platformService);
  }

  private void unlock(ToolkitObjectType toolkitObjectType, String name) {
    ToolkitLockingApi.unlock(toolkitObjectType, name, ToolkitLockTypeInternal.WRITE, platformService);
  }

  private void readLock(ToolkitObjectType toolkitObjectType, String name) {
    ToolkitLockingApi.lock(toolkitObjectType, name, ToolkitLockTypeInternal.READ, platformService);
  }

  private void readUnlock(ToolkitObjectType toolkitObjectType, String name) {
    ToolkitLockingApi.unlock(toolkitObjectType, name, ToolkitLockTypeInternal.READ, platformService);
  }

  @Override
  public void applyDestroy(String name) {
    this.isolatedTypes.remove(name);
  }

  @Override
  public final void destroy(AbstractDestroyableToolkitObject obj, ToolkitObjectType type) {
    lock(type, obj.getName());
    try {
      if (!obj.isDestroyed()) {
        removeToolkitType(type, obj.getName());
        obj.destroyFromCluster();
      }
    } finally {
      unlock(type, obj.getName());
    }
  }

  @Override
  public void onRejoinStart() {
    currentKeys = new HashSet<String>(isolatedTypes.keySet());
    for (String name : currentKeys) {
      T wrapper = isolatedTypes.get(name);
      if (wrapper != null) {
        wrapper.rejoinStarted();
      }
    }
  }

  @Override
  public void onRejoinComplete() {
    lookupOrCreateRoots();
    for (String name : currentKeys) {
      T wrapper = isolatedTypes.get(name);
      if (wrapper != null) {
        wrapper.rejoinCompleted();
      }
    }
    currentKeys = Collections.EMPTY_SET;
  }

  @Override
  public S lookupClusteredObject(String name, ToolkitObjectType type, Configuration config) {
    readLock(type, name);
    try {
      return getToolkitTypeRoot(name).getClusteredObject(name);
    } finally {
      readUnlock(type, name);
    }
  }

  @Override
  public void lookupOrCreateRoots() {
    GroupID[] gids = platformService.getGroupIDs();
    for (int i = 0; i < gids.length; i++) {
      roots[i] = ToolkitTypeRootsStaticFactory.lookupOrCreateRootInGroup(platformService, gids[i], rootName);
    }
  }

}
