/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.factory.impl;

import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.collections.ToolkitMap;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.internal.ToolkitInternal;

import com.tc.platform.PlatformService;
import com.terracotta.toolkit.collections.DestroyableToolkitMap;
import com.terracotta.toolkit.collections.map.ToolkitMapImpl;
import com.terracotta.toolkit.factory.ToolkitObjectFactory;
import com.terracotta.toolkit.roots.ToolkitTypeRootsFactory;
import com.terracotta.toolkit.roots.impl.ToolkitTypeConstants;
import com.terracotta.toolkit.type.IsolatedToolkitTypeFactory;

/**
 * An implementation of {@link ToolkitMapFactory}
 */
public class ToolkitMapFactoryImpl extends AbstractPrimaryToolkitObjectFactory<ToolkitMap, ToolkitMapImpl> {

  private static final MapIsolatedTypeFactory FACTORY = new MapIsolatedTypeFactory();

  public ToolkitMapFactoryImpl(ToolkitInternal toolkit, ToolkitTypeRootsFactory rootsFactory,
                               PlatformService platformService) {
    super(toolkit, rootsFactory.createAggregateIsolatedTypeRoot(ToolkitTypeConstants.TOOLKIT_MAP_ROOT_NAME, FACTORY,
                                                                platformService));
  }

  @Override
  public ToolkitObjectType getManufacturedToolkitObjectType() {
    return ToolkitObjectType.MAP;
  }

  private static class MapIsolatedTypeFactory implements IsolatedToolkitTypeFactory<ToolkitMap, ToolkitMapImpl> {

    @Override
    public ToolkitMap createIsolatedToolkitType(ToolkitObjectFactory<ToolkitMap> factory, String name,
                                                Configuration config, ToolkitMapImpl tcClusteredObject) {
      return new DestroyableToolkitMap(factory, tcClusteredObject, name);
    }

    @Override
    public ToolkitMapImpl createTCClusteredObject(Configuration config) {
      return new ToolkitMapImpl();
    }

  }

}
