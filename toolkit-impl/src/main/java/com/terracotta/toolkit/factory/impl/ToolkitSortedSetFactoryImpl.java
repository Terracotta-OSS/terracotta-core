/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.factory.impl;

import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.internal.ToolkitInternal;

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

  private static final SetIsolatedTypeFactory FACTORY = new SetIsolatedTypeFactory();

  public ToolkitSortedSetFactoryImpl(ToolkitInternal toolkit, ToolkitTypeRootsFactory rootsFactory) {
    super(toolkit, rootsFactory.createAggregateIsolatedTypeRoot(ToolkitTypeConstants.TOOLKIT_SORTED_SET_ROOT_NAME,
                                                                FACTORY));
  }

  @Override
  public ToolkitObjectType getManufacturedToolkitObjectType() {
    return ToolkitObjectType.SORTED_SET;
  }

  private static class SetIsolatedTypeFactory implements
      IsolatedToolkitTypeFactory<ToolkitSortedSetImpl, ToolkitSortedMapImpl> {

    @Override
    public ToolkitSortedSetImpl createIsolatedToolkitType(ToolkitObjectFactory<ToolkitSortedSetImpl> factory,
                                                          String name,
 Configuration config,
                                                          ToolkitSortedMapImpl tcClusteredObject) {
      return new ToolkitSortedSetImpl(factory, tcClusteredObject);
    }

    @Override
    public ToolkitSortedMapImpl createTCClusteredObject(Configuration config) {
      return new ToolkitSortedMapImpl();
    }
  }

}
