/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.factory.impl;

import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.internal.ToolkitInternal;

import com.terracotta.toolkit.collections.DestroyableToolkitSortedMap;
import com.terracotta.toolkit.collections.ToolkitSortedSetImpl;
import com.terracotta.toolkit.collections.map.ToolkitSortedMapImpl;
import com.terracotta.toolkit.factory.ToolkitFactoryInitializationContext;
import com.terracotta.toolkit.factory.ToolkitObjectFactory;
import com.terracotta.toolkit.roots.impl.ToolkitTypeConstants;
import com.terracotta.toolkit.type.IsolatedClusteredObjectLookup;
import com.terracotta.toolkit.type.IsolatedToolkitTypeFactory;

/**
 * An implementation of {@link ToolkitSortedSetFactory}
 */
public class ToolkitSortedSetFactoryImpl extends
    AbstractPrimaryToolkitObjectFactory<ToolkitSortedSetImpl, ToolkitSortedMapImpl> {

  private static final SortedSetIsolatedTypeFactory FACTORY = new SortedSetIsolatedTypeFactory();

  public ToolkitSortedSetFactoryImpl(ToolkitInternal toolkit, ToolkitFactoryInitializationContext context) {
    super(toolkit, context.getToolkitTypeRootsFactory()
        .createAggregateIsolatedTypeRoot(ToolkitTypeConstants.TOOLKIT_SORTED_SET_ROOT_NAME, FACTORY,
                                         context.getPlatformService()));
  }

  @Override
  public ToolkitObjectType getManufacturedToolkitObjectType() {
    return ToolkitObjectType.SORTED_SET;
  }

  private static class SortedSetIsolatedTypeFactory implements
      IsolatedToolkitTypeFactory<ToolkitSortedSetImpl, ToolkitSortedMapImpl> {

    @Override
    public ToolkitSortedSetImpl createIsolatedToolkitType(ToolkitObjectFactory<ToolkitSortedSetImpl> factory,
                                                          IsolatedClusteredObjectLookup<ToolkitSortedMapImpl> lookup,
                                                          String name, Configuration config,
                                                          ToolkitSortedMapImpl tcClusteredObject) {
      DestroyableToolkitSortedMap map = new DestroyableToolkitSortedMap(factory, lookup, tcClusteredObject, name);
      return new ToolkitSortedSetImpl(map);
    }

    @Override
    public ToolkitSortedMapImpl createTCClusteredObject(Configuration config) {
      return new ToolkitSortedMapImpl();
    }
  }

}
