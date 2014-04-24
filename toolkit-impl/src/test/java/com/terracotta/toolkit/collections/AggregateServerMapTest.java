package com.terracotta.toolkit.collections;

import org.junit.Before;
import org.junit.Test;
import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.builder.ToolkitCacheConfigBuilder;
import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.internal.cache.VersionedValue;
import org.terracotta.toolkit.store.ToolkitConfigFields;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;
import com.tc.object.ObjectID;
import com.tc.object.TCObjectServerMap;
import com.tc.platform.PlatformService;
import com.tc.properties.TCPropertiesImpl;
import com.terracotta.toolkit.collections.map.AggregateServerMap;
import com.terracotta.toolkit.collections.map.InternalToolkitMap;
import com.terracotta.toolkit.collections.map.ServerMap;
import com.terracotta.toolkit.collections.map.VersionedValueImpl;
import com.terracotta.toolkit.collections.servermap.api.ServerMapLocalStore;
import com.terracotta.toolkit.collections.servermap.api.ServerMapLocalStoreConfig;
import com.terracotta.toolkit.collections.servermap.api.ServerMapLocalStoreFactory;
import com.terracotta.toolkit.object.TCToolkitObject;
import com.terracotta.toolkit.object.ToolkitObjectStripe;
import com.terracotta.toolkit.object.ToolkitObjectStripeImpl;
import com.terracotta.toolkit.search.SearchFactory;
import com.terracotta.toolkit.type.DistributedClusteredObjectLookup;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.Callable;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author tim
 */
public class AggregateServerMapTest {

  private PlatformService platformService;
  private ServerMapLocalStoreFactory serverMapLocalStoreFactory;
  private Configuration configuration;

  @Before
  public void setUp() throws Exception {
    platformService = mock(PlatformService.class);
    when(platformService.getTCProperties()).thenReturn(TCPropertiesImpl.getProperties());

    ServerMapLocalStore serverMapLocalStore = mock(ServerMapLocalStore.class);
    serverMapLocalStoreFactory = mock(ServerMapLocalStoreFactory.class);
    when(serverMapLocalStoreFactory.getOrCreateServerMapLocalStore(any(ServerMapLocalStoreConfig.class))).thenReturn(serverMapLocalStore);

    configuration = new ToolkitCacheConfigBuilder().consistency(ToolkitConfigFields.Consistency.EVENTUAL).build();
  }

  @Test
  public void testGetAllVersioned() throws Exception {

    ToolkitObjectStripe[] stripeObjects = new ToolkitObjectStripe[2];
    final ServerMap serverMap1 = mockServerMap(1);
    final ServerMap serverMap2 = mockServerMap(2);
    stripeObjects[0] = new ToolkitObjectStripeImpl(configuration, new TCToolkitObject[] { serverMap1 });
    stripeObjects[1] = new ToolkitObjectStripeImpl(configuration, new TCToolkitObject[] { serverMap2 });

    AggregateServerMap<String, String> asm = new AggregateServerMap<String, String>(ToolkitObjectType.CACHE, mock(SearchFactory.class),
        mock(DistributedClusteredObjectLookup.class), "foo", stripeObjects, configuration,
        mock(Callable.class), serverMapLocalStoreFactory, platformService, mock(ToolkitLock.class)) {
      @Override
      protected TCObjectServerMap getAnyTCObjectServerMap() {
        return (TCObjectServerMap) serverMap1.__tc_managed();
      }

      @Override
      protected InternalToolkitMap<String, String> getAnyServerMap() {
        return serverMap1;
      }
    };

    SetMultimap<ObjectID, String> getAllRequest = HashMultimap.create();
    getAllRequest.put(new ObjectID(1), "foo");
    getAllRequest.put(new ObjectID(2), "bar");

    Map<String, VersionedValue<String>> expectedResult = Maps.newHashMap();
    expectedResult.put("foo", new VersionedValueImpl<String>("foo", 10));
    expectedResult.put("bar", new VersionedValueImpl<String>("bar", 12));
    when(serverMap1.getAllVersioned(getAllRequest)).thenReturn(expectedResult);

    // "foo".hashCode() is even, "bar".hashCode() is odd, neat.
    Map<String, VersionedValue<String>> result = asm.getAllVersioned(Arrays.asList("foo", "bar"));

    verify(serverMap1).getAllVersioned(getAllRequest);
    assertThat(result, is(expectedResult));
  }

  private ServerMap mockServerMap(long oid) {
    ServerMap serverMap = mock(ServerMap.class);
    TCObjectServerMap tcObject = mock(TCObjectServerMap.class);
    when(tcObject.getObjectID()).thenReturn(new ObjectID(oid));
    when(serverMap.__tc_managed()).thenReturn(tcObject);
    return serverMap;
  }
}
