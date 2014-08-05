/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.factory.impl;

import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.internal.ToolkitInternal;

import com.tc.platform.PlatformService;
import com.terracotta.toolkit.collections.DestroyableToolkitSortedMap;
import com.terracotta.toolkit.collections.map.ToolkitSortedMapImpl;
import com.terracotta.toolkit.factory.ToolkitFactoryInitializationContext;
import com.terracotta.toolkit.factory.ToolkitObjectFactory;
import com.terracotta.toolkit.roots.impl.ToolkitTypeConstants;
import com.terracotta.toolkit.type.IsolatedClusteredObjectLookup;
import com.terracotta.toolkit.type.IsolatedToolkitTypeFactory;

public class ToolkitSortedMapFactoryImpl extends
    AbstractPrimaryToolkitObjectFactory<DestroyableToolkitSortedMap, ToolkitSortedMapImpl> {

  public ToolkitSortedMapFactoryImpl(ToolkitInternal toolkit, ToolkitFactoryInitializationContext context) {
    super(toolkit, context.getToolkitTypeRootsFactory()
        .createAggregateIsolatedTypeRoot(ToolkitTypeConstants.TOOLKIT_SORTED_MAP_ROOT_NAME,
                                         new SortedMapIsolatedTypeFactory(context.getPlatformService()),
                                         context.getPlatformService()));
  }

  @Override
  public ToolkitObjectType getManufacturedToolkitObjectType() {
    return ToolkitObjectType.SORTED_MAP;
  }

  private static class SortedMapIsolatedTypeFactory implements
      IsolatedToolkitTypeFactory<DestroyableToolkitSortedMap, ToolkitSortedMapImpl> {

    private final PlatformService platformService;

    SortedMapIsolatedTypeFactory(PlatformService platformService) {
      this.platformService = platformService;
    }

    @Override
    public DestroyableToolkitSortedMap createIsolatedToolkitType(ToolkitObjectFactory<DestroyableToolkitSortedMap> factory,
                                                                 IsolatedClusteredObjectLookup<ToolkitSortedMapImpl> lookup,
                                                                 String name, Configuration config,
                                                                 ToolkitSortedMapImpl tcClusteredObject) {
      return new DestroyableToolkitSortedMap(factory, lookup, tcClusteredObject, name, platformService);
    }

    @Override
    public ToolkitSortedMapImpl createTCClusteredObject(Configuration config) {
      return new ToolkitSortedMapImpl(platformService);
    }

  }

}
