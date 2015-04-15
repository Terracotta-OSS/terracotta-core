/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.terracotta.toolkit.roots.impl;

import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.internal.ToolkitInternal;
import org.terracotta.toolkit.internal.concurrent.locks.ToolkitLockTypeInternal;

import com.tc.net.GroupID;
import com.tc.platform.PlatformService;
import com.tc.platform.rejoin.RejoinLifecycleListener;
import com.tc.util.Assert;
import com.terracotta.toolkit.concurrent.locks.ToolkitLockingApi;
import com.terracotta.toolkit.concurrent.locks.UnnamedToolkitReadWriteLock;
import com.terracotta.toolkit.factory.ToolkitObjectFactory;
import com.terracotta.toolkit.object.AbstractDestroyableToolkitObject;
import com.terracotta.toolkit.object.TCToolkitObject;
import com.terracotta.toolkit.object.ToolkitObjectStripe;
import com.terracotta.toolkit.roots.AggregateToolkitTypeRoot;
import com.terracotta.toolkit.roots.ToolkitTypeRoot;
import com.terracotta.toolkit.type.DistributedClusteredObjectLookup;
import com.terracotta.toolkit.type.DistributedToolkitType;
import com.terracotta.toolkit.type.DistributedToolkitTypeFactory;
import com.terracotta.toolkit.util.collections.WeakValueMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AggregateDistributedToolkitTypeRoot<T extends DistributedToolkitType<S>, S extends TCToolkitObject>
implements AggregateToolkitTypeRoot<T, S>, RejoinLifecycleListener, DistributedClusteredObjectLookup<S> {

  private final ToolkitTypeRoot<ToolkitObjectStripe<S>>[] roots;
  private final DistributedToolkitTypeFactory<T, S>       distributedTypeFactory;
  private final WeakValueMap<T>                           distributedTypes;
  private final PlatformService                           platformService;
  private final String                                    rootName;
  private Collection<T>                                   currentTypes = null;

  protected AggregateDistributedToolkitTypeRoot(String rootName, ToolkitTypeRoot<ToolkitObjectStripe<S>>[] roots,
                                                DistributedToolkitTypeFactory<T, S> factory, WeakValueMap weakValueMap,
                                                PlatformService platformService) {
    this.rootName = rootName;
    this.roots = roots;
    this.distributedTypeFactory = factory;
    this.distributedTypes = weakValueMap;
    this.platformService = platformService;
    this.platformService.addRejoinLifecycleListener(this);
  }

  @Override
  public T getOrCreateToolkitType(ToolkitInternal toolkit, ToolkitObjectFactory<T> factory, String name,
                                  Configuration configuration) {
    if (name == null) { throw new NullPointerException("'name' cannot be null"); }
    distributedTypeFactory.validateConfig(configuration);
    synchronized (distributedTypes) {
      boolean created = false;
      T distributedType = distributedTypes.get(name);
      if (distributedType != null) {
        // validate and reuse existing distributed object
        distributedTypeFactory.validateExistingLocalInstanceConfig(distributedType, configuration);
      } else {
        final ToolkitObjectType type = factory.getManufacturedToolkitObjectType();
        ToolkitObjectStripe<S>[] stripeObjects;
        final Configuration effectiveConfig;
        ToolkitLock objectCreationLock = objectInstanceLock(type, name).writeLock();
        objectCreationLock.lock();
        try {
          stripeObjects = lookupStripeObjects(name);
          if (stripeObjects != null) {
            effectiveConfig = distributedTypeFactory.newConfigForCreationInLocalNode(name, stripeObjects, configuration);
          } else {
            // make sure config is complete
            effectiveConfig = distributedTypeFactory.newConfigForCreationInCluster(configuration);
            // need to create stripe objects
            stripeObjects = distributedTypeFactory.createStripeObjects(name, effectiveConfig, roots.length,
                                                                       platformService);
            injectStripeObjects(name, stripeObjects);
            created = true;
          }
        } finally {
          objectCreationLock.unlock();
        }
        // create new distributed object after ToolkitObjectStripe has been created/faulted-in
        distributedType = distributedTypeFactory.createDistributedType(toolkit, factory, this, name, stripeObjects,
                                                                       effectiveConfig, platformService, objectCreationLock);
        T oldvalue = distributedTypes.put(name, distributedType);
        Assert.assertNull("oldValue must be null here", oldvalue);

        if (created) {
          toolkit.waitUntilAllTransactionsComplete();
        }
      }
      return distributedType;
    }
  }

  private void injectStripeObjects(final String name, final ToolkitObjectStripe<S>[] stripeObjects)
      throws AssertionError {
    if (stripeObjects == null || stripeObjects.length != roots.length) { throw new AssertionError(
                                                                                                  "DistributedTypeFactory should create as many ClusteredObjectStripe's "
                                                                                                      + "as there are stripes - numStripes: "
                                                                                                      + roots.length
                                                                                                      + ", created: "
                                                                                                      + (stripeObjects == null ? "null"
                                                                                                          : stripeObjects.length)); }
    for (int i = 0; i < roots.length; i++) {
      ToolkitTypeRoot<ToolkitObjectStripe<S>> root = roots[i];
      root.addClusteredObject(name, stripeObjects[i]);
    }
  }

  @Override
  public ToolkitObjectStripe<S>[] lookupStripeObjects(final String name, final ToolkitObjectType type,
                                                      Configuration config) {
    ToolkitLock lock = objectInstanceLock(type, name).readLock();
    lock.lock();
    try {
      return lookupStripeObjects(name);
    } finally {
      lock.unlock();
    }
  }

  private ToolkitObjectStripe<S>[] lookupStripeObjects(String name) {
    final List<ToolkitObjectStripe<S>> stripeObjects = new ArrayList<ToolkitObjectStripe<S>>(roots.length);
    int missingClusteredObjectStripe = -1;
    int i = 0;
    for (ToolkitTypeRoot<ToolkitObjectStripe<S>> root : roots) {
      ToolkitObjectStripe<S> clusteredObject = root.getClusteredObject(name);
      // If the Stripe was added after the object is created. It might not be present in the last stripes.
      if (clusteredObject != null) {
        if (missingClusteredObjectStripe > -1) {
          // missing object can be present only in newly added stripes.
          throw new AssertionError("ClusteredObjectStrip not created in stripe : " + missingClusteredObjectStripe);
        }
        stripeObjects.add(clusteredObject);
      } else {
        missingClusteredObjectStripe = i;
      }
      i++;
    }
    return stripeObjects.size() == 0 ? null : stripeObjects.toArray(new ToolkitObjectStripe[0]);
  }

  @Override
  public void removeToolkitType(ToolkitObjectType toolkitObjectType, String name) {
    ToolkitLock lock = objectInstanceLock(toolkitObjectType, name).writeLock();
    lock.lock();
    try {
      distributedTypes.remove(name);
      for (ToolkitTypeRoot<ToolkitObjectStripe<S>> root : roots) {
        root.removeClusteredObject(name);
      }
    } finally {
      lock.unlock();
    }
  }

  private UnnamedToolkitReadWriteLock objectInstanceLock(ToolkitObjectType type, String name) {
    return ToolkitLockingApi.createUnnamedReadWriteLock(type, name, platformService, ToolkitLockTypeInternal.SYNCHRONOUS_WRITE);
  }

  @Override
  public void applyDestroy(String name) {
    this.distributedTypes.remove(name);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final void destroy(AbstractDestroyableToolkitObject obj, ToolkitObjectType type) {
    ToolkitLock lock = objectInstanceLock(type, obj.getName()).writeLock();
    lock.lock();
    try {
      if (!obj.isDestroyed()) {
        removeToolkitType(type, obj.getName());
        obj.destroyFromCluster();
      }
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void onRejoinStart() {
    synchronized (distributedTypes) {
      currentTypes = distributedTypes.values();
      for (T wrapper : currentTypes) {
        if (wrapper != null) {
          wrapper.rejoinStarted();
        }
      }
    }
  }

  @Override
  public void onRejoinComplete() {
    synchronized (distributedTypes) {
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
  public void lookupOrCreateRoots() {
    GroupID[] gids = platformService.getGroupIDs();
    for (int i = 0; i < gids.length; i++) {
      roots[i] = ToolkitTypeRootsStaticFactory.lookupOrCreateRootInGroup(platformService, gids[i], rootName);
    }
  }

  @Override
  public void dispose(ToolkitObjectType toolkitObjectType, String name) {
    ToolkitLock lock = objectInstanceLock(toolkitObjectType, name).writeLock();
    lock.lock();
    try {
      distributedTypes.remove(name);
    } finally {
      lock.unlock();
    }
  }
}
