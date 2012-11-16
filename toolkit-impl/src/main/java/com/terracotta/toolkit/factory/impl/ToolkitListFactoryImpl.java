/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.factory.impl;

import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.internal.ToolkitInternal;

import com.terracotta.toolkit.collections.DestroyableToolkitList;
import com.terracotta.toolkit.collections.ToolkitListImpl;
import com.terracotta.toolkit.factory.ToolkitFactoryInitializationContext;
import com.terracotta.toolkit.factory.ToolkitObjectFactory;
import com.terracotta.toolkit.roots.impl.ToolkitTypeConstants;
import com.terracotta.toolkit.type.IsolatedClusteredObjectLookup;
import com.terracotta.toolkit.type.IsolatedToolkitTypeFactory;

/**
 * An implementation of {@link ClusteredListFactory}
 */
public class ToolkitListFactoryImpl extends
    AbstractPrimaryToolkitObjectFactory<DestroyableToolkitList, ToolkitListImpl> {

  private static final ListIsolatedTypeFactory FACTORY = new ListIsolatedTypeFactory();

  public ToolkitListFactoryImpl(ToolkitInternal toolkit, ToolkitFactoryInitializationContext context) {
    super(toolkit, context.getToolkitTypeRootsFactory()
        .createAggregateIsolatedTypeRoot(ToolkitTypeConstants.TOOLKIT_LIST_ROOT_NAME, FACTORY,
                                         context.getPlatformService()));
  }

  @Override
  public ToolkitObjectType getManufacturedToolkitObjectType() {
    return ToolkitObjectType.LIST;
  }

  private static class ListIsolatedTypeFactory implements
      IsolatedToolkitTypeFactory<DestroyableToolkitList, ToolkitListImpl> {
    @Override
    public DestroyableToolkitList createIsolatedToolkitType(ToolkitObjectFactory<DestroyableToolkitList> factory,
                                                            IsolatedClusteredObjectLookup<ToolkitListImpl> lookup,
                                                            String name, Configuration config,
                                                            ToolkitListImpl tcClusteredObject) {
      return new DestroyableToolkitList(factory, lookup, tcClusteredObject, name);
    }

    @Override
    public ToolkitListImpl createTCClusteredObject(Configuration config) {
      return new ToolkitListImpl();
    }
  }

}
