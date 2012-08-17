/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.factory.impl;

import org.terracotta.toolkit.collections.ToolkitSortedSet;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.internal.ToolkitInternal;

import com.terracotta.toolkit.collections.DestroyableToolkitSortedSet;
import com.terracotta.toolkit.collections.ToolkitSortedSetImpl;
import com.terracotta.toolkit.factory.ToolkitObjectFactory;
import com.terracotta.toolkit.object.ToolkitObjectType;
import com.terracotta.toolkit.roots.ToolkitTypeRootsFactory;
import com.terracotta.toolkit.roots.impl.ToolkitTypeConstants;
import com.terracotta.toolkit.type.IsolatedToolkitTypeFactory;

/**
 * An implementation of {@link ClusteredSortedSetFactory}
 */
public class ToolkitSortedSetFactoryImpl extends
    AbstractPrimaryToolkitObjectFactory<ToolkitSortedSet, ToolkitSortedSetImpl> {

  private static final SortedSetIsolatedTypeFactory FACTORY = new SortedSetIsolatedTypeFactory();

  public ToolkitSortedSetFactoryImpl(ToolkitInternal toolkit, ToolkitTypeRootsFactory rootsFactory) {
    super(toolkit, rootsFactory.createAggregateIsolatedTypeRoot(ToolkitTypeConstants.TOOLKIT_SORTED_SET_ROOT_NAME,
                                                                FACTORY));
  }

  @Override
  public ToolkitObjectType getManufacturedToolkitObjectType() {
    return ToolkitObjectType.SORTED_SET;
  }

  private static class SortedSetIsolatedTypeFactory implements
      IsolatedToolkitTypeFactory<ToolkitSortedSet, ToolkitSortedSetImpl> {

    @Override
    public ToolkitSortedSet createIsolatedToolkitType(ToolkitObjectFactory<ToolkitSortedSet> factory, String name,
                                                      Configuration config, ToolkitSortedSetImpl tcClusteredObject) {
      return new DestroyableToolkitSortedSet(factory, tcClusteredObject, name);
    }

    @Override
    public ToolkitSortedSetImpl createTCClusteredObject(Configuration config) {
      return new ToolkitSortedSetImpl();
    }

  }

}
