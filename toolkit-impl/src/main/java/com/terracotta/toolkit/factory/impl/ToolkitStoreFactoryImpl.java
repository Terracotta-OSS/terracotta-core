/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */

package com.terracotta.toolkit.factory.impl;

import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.internal.ToolkitInternal;

import com.terracotta.toolkit.collections.map.ServerMap;
import com.terracotta.toolkit.collections.map.ToolkitCacheImpl;
import com.terracotta.toolkit.factory.ToolkitFactoryInitializationContext;
import com.terracotta.toolkit.roots.impl.ToolkitTypeConstants;

public class ToolkitStoreFactoryImpl extends AbstractPrimaryToolkitObjectFactory<ToolkitCacheImpl, ServerMap> {

  private ToolkitStoreFactoryImpl(ToolkitInternal toolkit, ToolkitFactoryInitializationContext context) {
    super(toolkit, context.getToolkitTypeRootsFactory().createAggregateDistributedTypeRoot(
        ToolkitTypeConstants.TOOLKIT_STORE_ROOT_NAME, new ToolkitStoreDistributedTypeFactory(
        context.getSearchFactory(), context.getServerMapLocalStoreFactory()), context.getPlatformService()));
  }

  public static ToolkitStoreFactoryImpl newToolkitStoreFactory(ToolkitInternal toolkit,
                                                               ToolkitFactoryInitializationContext context) {
    return new ToolkitStoreFactoryImpl(toolkit, context);
  }

  @Override
  public ToolkitObjectType getManufacturedToolkitObjectType() {
    return ToolkitObjectType.STORE;
  }

}
