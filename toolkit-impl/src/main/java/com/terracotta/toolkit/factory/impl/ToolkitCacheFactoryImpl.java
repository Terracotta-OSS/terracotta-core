/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.factory.impl;

import org.terracotta.toolkit.cache.ToolkitCache;
import org.terracotta.toolkit.internal.ToolkitInternal;

import com.tc.platform.PlatformService;
import com.terracotta.toolkit.collections.map.ServerMap;
import com.terracotta.toolkit.collections.servermap.api.ServerMapLocalStoreFactory;
import com.terracotta.toolkit.factory.ToolkitObjectFactory;
import com.terracotta.toolkit.object.ToolkitObjectType;
import com.terracotta.toolkit.roots.ToolkitTypeRootsFactory;
import com.terracotta.toolkit.roots.impl.ToolkitTypeConstants;
import com.terracotta.toolkit.search.SearchBuilderFactory;

/**
 * Implementation of {@link ClusteredCacheFactory}
 */
public class ToolkitCacheFactoryImpl extends AbstractPrimaryToolkitObjectFactory<ToolkitCache, ServerMap> implements
    ToolkitObjectFactory<ToolkitCache> {

  private final ToolkitObjectType type;

  private ToolkitCacheFactoryImpl(ToolkitInternal toolkit, ToolkitObjectType type,
                                  ToolkitTypeRootsFactory rootsFactory, SearchBuilderFactory searchBuilderFactory,
                                  ServerMapLocalStoreFactory serverMapLocalStoreFactory, PlatformService platformService) {
    super(toolkit, rootsFactory
        .createAggregateDistributedTypeRoot(getRootNameFor(type),
                                            new ToolkitCacheDistributedTypeFactory(searchBuilderFactory,
                                                                                   serverMapLocalStoreFactory),
                                            platformService));
    this.type = type;
  }

  public static ToolkitCacheFactoryImpl newToolkitCacheFactory(ToolkitInternal toolkit,
                                                               ToolkitTypeRootsFactory rootsFactory,
                                                               SearchBuilderFactory searchBuilderFactory,
                                                               ServerMapLocalStoreFactory serverMapLocalStoreFactory,
                                                               PlatformService platformService) {
    return new ToolkitCacheFactoryImpl(toolkit, ToolkitObjectType.CACHE, rootsFactory, searchBuilderFactory,
                                       serverMapLocalStoreFactory, platformService);
  }

  public static ToolkitCacheFactoryImpl newToolkitStoreFactory(ToolkitInternal toolkit,
                                                               ToolkitTypeRootsFactory rootsFactory,
                                                               SearchBuilderFactory searchBuilderFactory,
                                                               ServerMapLocalStoreFactory serverMapLocalStoreFactory,
                                                               PlatformService platformService) {
    return new ToolkitCacheFactoryImpl(toolkit, ToolkitObjectType.STORE, rootsFactory, searchBuilderFactory,
                                       serverMapLocalStoreFactory, platformService);
  }

  @Override
  public ToolkitObjectType getManufacturedToolkitObjectType() {
    return type;
  }

  public static String getRootNameFor(ToolkitObjectType type) {
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
