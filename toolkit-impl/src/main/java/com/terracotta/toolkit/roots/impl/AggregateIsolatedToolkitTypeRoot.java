/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.roots.impl;

import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.internal.ToolkitInternal;
import org.terracotta.toolkit.internal.concurrent.locks.ToolkitLockTypeInternal;

import com.tc.net.GroupID;
import com.tc.platform.PlatformService;
import com.tc.platform.rejoin.RejoinLifecycleListener;
import com.tc.util.Assert;
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

import java.util.Collection;

public class AggregateIsolatedToolkitTypeRoot<T extends RejoinAwareToolkitObject, S extends TCToolkitObject> implements
    AggregateToolkitTypeRoot<T, S>, RejoinLifecycleListener, IsolatedClusteredObjectLookup<S> {

  private final ToolkitTypeRoot<S>[]             roots;
  private final IsolatedToolkitTypeFactory<T, S> isolatedTypeFactory;
  private final WeakValueMap<T>                  isolatedTypes;
  private final PlatformService                  platformService;
  private final String                           rootName;
  private Collection<T>                          currentTypes = null;

  protected AggregateIsolatedToolkitTypeRoot(String rootName, ToolkitTypeRoot<S>[] roots,
                                             IsolatedToolkitTypeFactory<T, S> isolatedTypeFactory,
                                             WeakValueMap weakValueMap, PlatformService platformService) {
    this.rootName = rootName;
    this.roots = roots;
    this.isolatedTypeFactory = isolatedTypeFactory;
    this.isolatedTypes = weakValueMap;
    this.platformService = platformService;
    this.platformService.addRejoinLifecycleListener(this);
  }

  @Override
  public T getOrCreateToolkitType(ToolkitInternal toolkit, ToolkitObjectFactory<T> factory, String name,
                                  Configuration configuration) {
    if (name == null) { throw new NullPointerException("'name' cannot be null"); }
    synchronized (isolatedTypes) {
      boolean created = false;
      T isolatedType = isolatedTypes.get(name);
      if (isolatedType == null) {
        final ToolkitObjectType type = factory.getManufacturedToolkitObjectType();
        S clusteredObject;
        lock(type, name);
        try {
          clusteredObject = lookupClusteredObject(name);
          if (clusteredObject == null) {
            clusteredObject = isolatedTypeFactory.createTCClusteredObject(configuration);
            getToolkitTypeRootForCreation(name).addClusteredObject(name, clusteredObject);
            created = true;
          }
        } finally {
          unlock(type, name);
        }
        // create new isolated object after ClusteredObject has been created/faulted-in
        isolatedType = isolatedTypeFactory.createIsolatedToolkitType(factory, this, name, configuration,
                                                                     clusteredObject);
        T oldvalue = isolatedTypes.put(name, isolatedType);
        Assert.assertNull("oldValue must be null here", oldvalue);

        if (created) {
          toolkit.waitUntilAllTransactionsComplete();
        }
      }
      return isolatedType;
    }
  }

  @Override
  public void removeToolkitType(ToolkitObjectType toolkitObjectType, String name) {
    lock(toolkitObjectType, name);
    try {
      isolatedTypes.remove(name);
      ToolkitTypeRoot<S> hashBasedRoot = getToolkitTypeRootForCreation(name);
      if (hashBasedRoot.getClusteredObject(name) != null) {
        hashBasedRoot.removeClusteredObject(name);
        return;
      }

      // We are looking up in all the stripes as a New stripe could have been added.
      for (ToolkitTypeRoot<S> root : roots) {
        S clusteredObject = root.getClusteredObject(name);
        if (clusteredObject != null) {
          root.removeClusteredObject(name);
          return;
        }
      }
    } finally {
      unlock(toolkitObjectType, name);
    }
  }

  private ToolkitTypeRoot<S> getToolkitTypeRootForCreation(String name) {
    return roots[Math.abs(name.hashCode() % roots.length)];
  }

  private void lock(ToolkitObjectType toolkitObjectType, String name) {
    ToolkitLockingApi.lock(toolkitObjectType, name, ToolkitLockTypeInternal.SYNCHRONOUS_WRITE, platformService);
  }

  private void unlock(ToolkitObjectType toolkitObjectType, String name) {
    ToolkitLockingApi.unlock(toolkitObjectType, name, ToolkitLockTypeInternal.SYNCHRONOUS_WRITE, platformService);
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
    synchronized (isolatedTypes) {
      currentTypes = isolatedTypes.values();
      for (T wrapper : currentTypes) {
        if (wrapper != null) {
          wrapper.rejoinStarted();
        }
      }
    }
  }

  @Override
  public void onRejoinComplete() {
    synchronized (isolatedTypes) {
      lookupOrCreateRoots();
      for (T wrapper : currentTypes) {
        if (wrapper != null) {
          wrapper.rejoinCompleted();
        }
      }
      currentTypes = null;
    }
  }

  @Override
  public S lookupClusteredObject(String name, ToolkitObjectType type, Configuration config) {
    readLock(type, name);
    try {
      return lookupClusteredObject(name);
    } finally {
      readUnlock(type, name);
    }
  }

  private S lookupClusteredObject(String name) {
    // perform a hash based lookup first.
    S clusteredObject = getToolkitTypeRootForCreation(name).getClusteredObject(name);
    if (clusteredObject != null) { return clusteredObject; }

    // We are looking up in all the stripes as a New stripe could have been added.
    for (ToolkitTypeRoot<S> root : roots) {
      clusteredObject = root.getClusteredObject(name);
      if (clusteredObject != null) { return clusteredObject; }
    }
    // return null If root not present in any stripe.
    return null;
  }

  @Override
  public void lookupOrCreateRoots() {
    GroupID[] gids = platformService.getGroupIDs();
    for (int i = 0; i < gids.length; i++) {
      roots[i] = ToolkitTypeRootsStaticFactory.lookupOrCreateRootInGroup(platformService, gids[i], rootName);
    }
  }

  @Override
  public void dispose(ToolkitObjectType toolkitObjectType, String name) {
    lock(toolkitObjectType, name);
    try {
      isolatedTypes.remove(name);
    } finally {
      unlock(toolkitObjectType, name);
    }
  }

}
