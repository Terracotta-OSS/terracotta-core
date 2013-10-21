/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.collections;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.config.Configuration;

import com.tc.object.TCObjectServerMap;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheStore;
import com.tc.platform.PlatformService;
import com.terracotta.toolkit.collections.map.AggregateServerMap;
import com.terracotta.toolkit.collections.map.InternalToolkitMap;
import com.terracotta.toolkit.collections.servermap.api.ServerMapLocalStoreFactory;
import com.terracotta.toolkit.object.ToolkitObjectStripe;
import com.terracotta.toolkit.search.SearchFactory;
import com.terracotta.toolkit.type.DistributedClusteredObjectLookup;

import java.util.concurrent.Callable;

public class MockAggregateServerMap extends AggregateServerMap {
  @Mock
  private InternalToolkitMap         anyServerMap;
  @Mock
  private TCObjectServerMap          anyTcObjectServerMap;

  @Mock
  private L1ServerMapLocalCacheStore localCacheStore;

  public MockAggregateServerMap(ToolkitObjectType type, SearchFactory searchBuilderFactory,
                                DistributedClusteredObjectLookup lookup, String name,
                                ToolkitObjectStripe[] stripeObjects, Configuration config, Callable schemaCreator,
                                ServerMapLocalStoreFactory serverMapLocalStoreFactory, PlatformService platformService) {
    super(type, searchBuilderFactory, lookup, name, stripeObjects, config, schemaCreator, serverMapLocalStoreFactory,
          platformService);
    MockitoAnnotations.initMocks(this);

  }

  @Override
  protected L1ServerMapLocalCacheStore createLocalCacheStore() {
    return localCacheStore;
  }

  @Override
  protected InternalToolkitMap getAnyServerMap() {
    return anyServerMap;
  }

  @Override
  protected TCObjectServerMap getAnyTCObjectServerMap() {
    return anyTcObjectServerMap;
  }

}