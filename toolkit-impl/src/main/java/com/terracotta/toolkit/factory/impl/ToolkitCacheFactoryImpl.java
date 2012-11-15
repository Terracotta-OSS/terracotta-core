/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.factory.impl;

import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.cache.ToolkitCache;
import org.terracotta.toolkit.internal.ToolkitInternal;

import com.terracotta.toolkit.collections.map.ServerMap;
import com.terracotta.toolkit.factory.ToolkitFactoryInitializationContext;
import com.terracotta.toolkit.factory.ToolkitObjectFactory;
import com.terracotta.toolkit.roots.impl.ToolkitTypeConstants;

/**
 * Implementation of {@link ClusteredCacheFactory}
 */
public class ToolkitCacheFactoryImpl extends AbstractPrimaryToolkitObjectFactory<ToolkitCache, ServerMap> implements
    ToolkitObjectFactory<ToolkitCache> {

  private final ToolkitObjectType type;

  private ToolkitCacheFactoryImpl(ToolkitInternal toolkit, ToolkitObjectType type,
                                  ToolkitFactoryInitializationContext context) {
    super(toolkit, context.getToolkitTypeRootsFactory()
        .createAggregateDistributedTypeRoot(getRootNameFor(type),
                                            new ToolkitCacheDistributedTypeFactory(context.getSearchFactory(), context
                                                .getServerMapLocalStoreFactory()), context.getPlatformService()));
    this.type = type;
  }

  public static ToolkitCacheFactoryImpl newToolkitCacheFactory(ToolkitInternal toolkit,
                                                               ToolkitFactoryInitializationContext context) {
    return new ToolkitCacheFactoryImpl(toolkit, ToolkitObjectType.CACHE, context);
  }

  public static ToolkitCacheFactoryImpl newToolkitStoreFactory(ToolkitInternal toolkit,
                                                               ToolkitFactoryInitializationContext context) {
    return new ToolkitCacheFactoryImpl(toolkit, ToolkitObjectType.STORE, context);
  }

  @Override
  public ToolkitObjectType getManufacturedToolkitObjectType() {
    return type;
  }

  private static String getRootNameFor(ToolkitObjectType type) {
    switch (type) {
      case CACHE:
        return ToolkitTypeConstants.TOOLKIT_CACHE_ROOT_NAME;
      case STORE:
        return ToolkitTypeConstants.TOOLKIT_STORE_ROOT_NAME;
      default:
        throw new IllegalArgumentException("Can't create factory for type - " + type + " using "
                                           + ToolkitCacheFactoryImpl.class.getName());
    }
  }

}
