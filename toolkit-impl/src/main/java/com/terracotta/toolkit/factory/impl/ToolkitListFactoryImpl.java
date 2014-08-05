/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.factory.impl;

import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.internal.ToolkitInternal;

import com.tc.platform.PlatformService;
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

  public ToolkitListFactoryImpl(ToolkitInternal toolkit, ToolkitFactoryInitializationContext context) {
    super(toolkit, context.getToolkitTypeRootsFactory()
        .createAggregateIsolatedTypeRoot(ToolkitTypeConstants.TOOLKIT_LIST_ROOT_NAME, new ListIsolatedTypeFactory(context.getPlatformService()),
                                         context.getPlatformService()));
  }

  @Override
  public ToolkitObjectType getManufacturedToolkitObjectType() {
    return ToolkitObjectType.LIST;
  }

  private static class ListIsolatedTypeFactory implements
      IsolatedToolkitTypeFactory<DestroyableToolkitList, ToolkitListImpl> {
    private final PlatformService platformService;

    ListIsolatedTypeFactory(PlatformService platformService) {
      this.platformService = platformService;
    }

    @Override
    public DestroyableToolkitList createIsolatedToolkitType(ToolkitObjectFactory<DestroyableToolkitList> factory,
                                                            IsolatedClusteredObjectLookup<ToolkitListImpl> lookup,
                                                            String name, Configuration config,
                                                            ToolkitListImpl tcClusteredObject) {
      return new DestroyableToolkitList(factory, lookup, tcClusteredObject, name, platformService);
    }

    @Override
    public ToolkitListImpl createTCClusteredObject(Configuration config) {
      return new ToolkitListImpl(platformService);
    }
  }

}
