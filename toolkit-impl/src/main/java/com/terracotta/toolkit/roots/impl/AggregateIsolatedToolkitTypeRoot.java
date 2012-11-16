/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.roots.impl;

import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.internal.ToolkitInternal;
import org.terracotta.toolkit.internal.concurrent.locks.ToolkitLockTypeInternal;

import com.tc.abortable.AbortedOperationException;
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
import com.terracotta.toolkit.type.IsolatedToolkitTypeFactory;
import com.terracotta.toolkit.util.collections.WeakValueMap;

public class AggregateIsolatedToolkitTypeRoot<T extends RejoinAwareToolkitObject, S extends TCToolkitObject> implements
    AggregateToolkitTypeRoot<T, S>, RejoinLifecycleListener {

  private final ToolkitTypeRoot<S>[]             roots;
  private final IsolatedToolkitTypeFactory<T, S> isolatedTypeFactory;
  private final WeakValueMap<T>                  isolatedTypes;
  private final PlatformService                  platformService;

  protected AggregateIsolatedToolkitTypeRoot(ToolkitTypeRoot<S>[] roots,
                                             IsolatedToolkitTypeFactory<T, S> isolatedTypeFactory,
                                             WeakValueMap weakValueMap, PlatformService platformService) {
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
        isolatedType = isolatedTypeFactory.createIsolatedToolkitType(factory, name, configuration, clusteredObject);
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
    for (String name : isolatedTypes.keySet()) {
      T wrapper = isolatedTypes.get(name);
      wrapper.rejoinStarted();
    }
  }

  @Override
  public void onRejoinComplete() {
    for (String name : isolatedTypes.keySet()) {
      T wrapper = isolatedTypes.get(name);
      wrapper.rejoinCompleted();
    }
  }

}
