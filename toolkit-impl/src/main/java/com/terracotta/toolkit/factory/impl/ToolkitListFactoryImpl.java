/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.factory.impl;

import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.collections.ToolkitList;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.internal.ToolkitInternal;

import com.tc.platform.PlatformService;
import com.terracotta.toolkit.collections.DestroyableToolkitList;
import com.terracotta.toolkit.collections.ToolkitListImpl;
import com.terracotta.toolkit.factory.ToolkitObjectFactory;
import com.terracotta.toolkit.roots.ToolkitTypeRootsFactory;
import com.terracotta.toolkit.roots.impl.ToolkitTypeConstants;
import com.terracotta.toolkit.type.IsolatedToolkitTypeFactory;

/**
 * An implementation of {@link ClusteredListFactory}
 */
public class ToolkitListFactoryImpl extends AbstractPrimaryToolkitObjectFactory<ToolkitList, ToolkitListImpl> {

  private static final ListIsolatedTypeFactory FACTORY = new ListIsolatedTypeFactory();

  public ToolkitListFactoryImpl(ToolkitInternal toolkit, ToolkitTypeRootsFactory rootsFactory,
                                PlatformService platformService) {
    super(toolkit, rootsFactory.createAggregateIsolatedTypeRoot(ToolkitTypeConstants.TOOLKIT_LIST_ROOT_NAME, FACTORY,
                                                                platformService));
  }

  @Override
  public ToolkitObjectType getManufacturedToolkitObjectType() {
    return ToolkitObjectType.LIST;
  }

  private static class ListIsolatedTypeFactory implements IsolatedToolkitTypeFactory<ToolkitList, ToolkitListImpl> {
    @Override
    public ToolkitList createIsolatedToolkitType(ToolkitObjectFactory<ToolkitList> factory, String name,
                                                 Configuration config, ToolkitListImpl tcClusteredObject) {
      return new DestroyableToolkitList(factory, tcClusteredObject, name);
    }

    @Override
    public ToolkitListImpl createTCClusteredObject(Configuration config) {
      return new ToolkitListImpl();
    }
  }

}
