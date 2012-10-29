/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.factory.impl;

import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.internal.ToolkitInternal;

import com.tc.object.bytecode.PlatformService;
import com.terracotta.toolkit.collections.DestroyableToolkitSortedMap;
import com.terracotta.toolkit.collections.ToolkitSortedSetImpl;
import com.terracotta.toolkit.collections.map.ToolkitSortedMapImpl;
import com.terracotta.toolkit.factory.ToolkitObjectFactory;
import com.terracotta.toolkit.object.ToolkitObjectType;
import com.terracotta.toolkit.roots.ToolkitTypeRootsFactory;
import com.terracotta.toolkit.roots.impl.ToolkitTypeConstants;
import com.terracotta.toolkit.type.IsolatedToolkitTypeFactory;

/**
 * An implementation of {@link ToolkitSortedSetFactory}
 */
public class ToolkitSortedSetFactoryImpl extends
    AbstractPrimaryToolkitObjectFactory<ToolkitSortedSetImpl, ToolkitSortedMapImpl> {

  private static final SortedSetIsolatedTypeFactory FACTORY = new SortedSetIsolatedTypeFactory();

  public ToolkitSortedSetFactoryImpl(ToolkitInternal toolkit, ToolkitTypeRootsFactory rootsFactory,
                                     PlatformService platformService) {
    super(toolkit, rootsFactory.createAggregateIsolatedTypeRoot(ToolkitTypeConstants.TOOLKIT_SORTED_SET_ROOT_NAME,
                                                                FACTORY, platformService));
  }

  @Override
  public ToolkitObjectType getManufacturedToolkitObjectType() {
    return ToolkitObjectType.SORTED_SET;
  }

  private static class SortedSetIsolatedTypeFactory implements
      IsolatedToolkitTypeFactory<ToolkitSortedSetImpl, ToolkitSortedMapImpl> {

    @Override
    public ToolkitSortedSetImpl createIsolatedToolkitType(ToolkitObjectFactory<ToolkitSortedSetImpl> factory,
                                                          String name, Configuration config,
                                                          ToolkitSortedMapImpl tcClusteredObject) {
      DestroyableToolkitSortedMap map = new DestroyableToolkitSortedMap(factory, tcClusteredObject, name);
      return new ToolkitSortedSetImpl(map);
    }

    @Override
    public ToolkitSortedMapImpl createTCClusteredObject(Configuration config) {
      return new ToolkitSortedMapImpl();
    }
  }

}
