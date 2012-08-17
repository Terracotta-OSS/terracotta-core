/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.roots.impl;

import org.terracotta.toolkit.object.ToolkitObject;

import com.tc.net.GroupID;
import com.tc.object.bytecode.ManagerUtil;
import com.terracotta.toolkit.object.TCToolkitObject;
import com.terracotta.toolkit.object.ToolkitObjectStripe;
import com.terracotta.toolkit.roots.AggregateToolkitTypeRoot;
import com.terracotta.toolkit.roots.ToolkitTypeRoot;
import com.terracotta.toolkit.roots.ToolkitTypeRootsFactory;
import com.terracotta.toolkit.roots.impl.RootsUtil.RootObjectCreator;
import com.terracotta.toolkit.type.DistributedToolkitType;
import com.terracotta.toolkit.type.DistributedToolkitTypeFactory;
import com.terracotta.toolkit.type.IsolatedToolkitTypeFactory;
import com.terracotta.toolkit.util.collections.WeakValueMapManager;

public class ToolkitTypeRootsStaticFactory implements ToolkitTypeRootsFactory {

  private final WeakValueMapManager manager;

  public ToolkitTypeRootsStaticFactory(WeakValueMapManager manager) {
    this.manager = manager;
  }

  public <T extends ToolkitObject, S extends TCToolkitObject> AggregateIsolatedToolkitTypeRoot<T, S> createAggregateIsolatedTypeRoot(String name,
                                                                                                                                     IsolatedToolkitTypeFactory<T, S> isolatedTypeFactory) {
    GroupID[] gids = ManagerUtil.getGroupIDs();
    ToolkitTypeRoot<S>[] roots = new ToolkitTypeRoot[gids.length];
    for (int i = 0; i < gids.length; i++) {
      roots[i] = lookupOrCreateRootInGroup(gids[i], name);
    }
    return new AggregateIsolatedToolkitTypeRoot<T, S>(roots, isolatedTypeFactory, manager.createWeakValueMap());
  }

  private static ToolkitTypeRoot lookupOrCreateRootInGroup(GroupID gid, String name) {
    return RootsUtil.lookupOrCreateRootInGroup(gid, name, new RootObjectCreator<ToolkitTypeRootImpl>() {
      @Override
      public ToolkitTypeRootImpl create() {
        return new ToolkitTypeRootImpl();
      }
    });
  }

  public <T extends DistributedToolkitType<S>, S extends TCToolkitObject> AggregateToolkitTypeRoot<T, S> createAggregateDistributedTypeRoot(String rootName,
                                                                                                                                            DistributedToolkitTypeFactory<T, S> aggregateToolkitTypeFactory) {
    GroupID[] gids = ManagerUtil.getGroupIDs();
    ToolkitTypeRoot<ToolkitObjectStripe<S>>[] roots = new ToolkitTypeRoot[gids.length];
    for (int i = 0; i < gids.length; i++) {
      roots[i] = lookupOrCreateRootInGroup(gids[i], rootName);
    }
    return new AggregateDistributedToolkitTypeRoot(roots, aggregateToolkitTypeFactory, manager.createWeakValueMap());
  }
}
