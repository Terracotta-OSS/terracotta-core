/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.factory.impl;

import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.internal.ToolkitInternal;

import com.tc.platform.PlatformService;
import com.terracotta.toolkit.collections.DestroyableToolkitMap;
import com.terracotta.toolkit.collections.ToolkitSetImpl;
import com.terracotta.toolkit.collections.map.ToolkitMapImpl;
import com.terracotta.toolkit.factory.ToolkitFactoryInitializationContext;
import com.terracotta.toolkit.factory.ToolkitObjectFactory;
import com.terracotta.toolkit.roots.impl.ToolkitTypeConstants;
import com.terracotta.toolkit.type.IsolatedClusteredObjectLookup;
import com.terracotta.toolkit.type.IsolatedToolkitTypeFactory;

public class ToolkitSetFactoryImpl extends AbstractPrimaryToolkitObjectFactory<ToolkitSetImpl, ToolkitMapImpl> {

  public ToolkitSetFactoryImpl(ToolkitInternal toolkit, ToolkitFactoryInitializationContext context) {
    super(toolkit, context.getToolkitTypeRootsFactory()
        .createAggregateIsolatedTypeRoot(ToolkitTypeConstants.TOOLKIT_SET_ROOT_NAME, new SetIsolatedTypeFactory(context.getPlatformService()),
                                         context.getPlatformService()));
  }

  @Override
  public ToolkitObjectType getManufacturedToolkitObjectType() {
    return ToolkitObjectType.SET;
  }

  private static class SetIsolatedTypeFactory implements IsolatedToolkitTypeFactory<ToolkitSetImpl, ToolkitMapImpl> {

    private final PlatformService platformService;

    SetIsolatedTypeFactory(PlatformService platformService) {
      this.platformService = platformService;
    }

    @Override
    public ToolkitSetImpl createIsolatedToolkitType(ToolkitObjectFactory<ToolkitSetImpl> factory,
                                                    IsolatedClusteredObjectLookup<ToolkitMapImpl> lookup, String name,
                                                    Configuration config, ToolkitMapImpl tcClusteredObject) {
      DestroyableToolkitMap map = new DestroyableToolkitMap(factory, lookup, tcClusteredObject, name, platformService);
      return new ToolkitSetImpl(map, platformService);
    }

    @Override
    public ToolkitMapImpl createTCClusteredObject(Configuration config) {
      return new ToolkitMapImpl(platformService);
    }

  }

}
