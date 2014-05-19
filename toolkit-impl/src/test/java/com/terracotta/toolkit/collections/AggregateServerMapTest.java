package com.terracotta.toolkit.collections;

import org.junit.Before;
import org.junit.Test;
import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.builder.ToolkitCacheConfigBuilder;
import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.internal.cache.VersionedValue;
import org.terracotta.toolkit.rejoin.RejoinException;
import org.terracotta.toolkit.store.ToolkitConfigFields;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;
import com.tc.object.ObjectID;
import com.tc.object.TCObjectServerMap;
import com.tc.platform.PlatformService;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.concurrent.TaskRunner;
import com.terracotta.toolkit.bulkload.BufferedOperation;
import com.terracotta.toolkit.collections.map.AggregateServerMap;
import com.terracotta.toolkit.collections.map.InternalToolkitMap;
import com.terracotta.toolkit.collections.map.ServerMap;
import com.terracotta.toolkit.collections.map.VersionedValueImpl;
import com.terracotta.toolkit.collections.servermap.api.ServerMapLocalStore;
import com.terracotta.toolkit.collections.servermap.api.ServerMapLocalStoreConfig;
import com.terracotta.toolkit.collections.servermap.api.ServerMapLocalStoreFactory;
import com.terracotta.toolkit.object.ToolkitObjectStripe;
import com.terracotta.toolkit.object.ToolkitObjectStripeImpl;
import com.terracotta.toolkit.search.SearchFactory;
import com.terracotta.toolkit.type.DistributedClusteredObjectLookup;
import com.terracotta.toolkit.util.ImmediateTimer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
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
    TaskRunner taskRunner = mock(TaskRunner.class);
    when(taskRunner.newTimer()).thenReturn(new ImmediateTimer());
    when(taskRunner.newTimer(anyString())).thenReturn(new ImmediateTimer());

    platformService = mock(PlatformService.class);
    when(platformService.getTaskRunner()).thenReturn(taskRunner);
    when(platformService.getTCProperties()).thenReturn(TCPropertiesImpl.getProperties());

    ServerMapLocalStore serverMapLocalStore = mock(ServerMapLocalStore.class);
    serverMapLocalStoreFactory = mock(ServerMapLocalStoreFactory.class);
    when(serverMapLocalStoreFactory.getOrCreateServerMapLocalStore(any(ServerMapLocalStoreConfig.class))).thenReturn(serverMapLocalStore);

    configuration = new ToolkitCacheConfigBuilder().consistency(ToolkitConfigFields.Consistency.EVENTUAL).build();
  }

  @Test
  public void testDrain() throws Exception {
    final List<ServerMap> serverMapList = mockServerMaps(256);
    ToolkitObjectStripe[] stripeObjects = createObjectStripes(configuration, serverMapList, 64);

    AggregateServerMap<String, String> asm = new AggregateServerMap<String, String>(ToolkitObjectType.CACHE, mock(SearchFactory.class),
        mock(DistributedClusteredObjectLookup.class), "foo", stripeObjects, configuration,
        mock(Callable.class), serverMapLocalStoreFactory, platformService, mock(ToolkitLock.class));

    Map<String, BufferedOperation<String>> allOps = new HashMap<String, BufferedOperation<String>>();
    Map<Integer, Map<String, BufferedOperation<String>>> batchedOps = new HashMap<Integer, Map<String, BufferedOperation<String>>>();
    for (int i = 0; i < 1000; i++) {
      String key = UUID.randomUUID().toString();
      BufferedOperation<String> bufferedOperation = mock(BufferedOperation.class);
      allOps.put(key, bufferedOperation);
      Map<String, BufferedOperation<String>> batch = batchedOps.get(Math.abs(key.hashCode() % 256));
      if (batch == null) {
        batch = new HashMap<String, BufferedOperation<String>>();
        batchedOps.put(Math.abs(key.hashCode() % 256), batch);
      }
      batch.put(key, bufferedOperation);
    }

    asm.drain(allOps);
    for (Map.Entry<Integer, Map<String, BufferedOperation<String>>> entry : batchedOps.entrySet()) {
      verify(serverMapList.get(entry.getKey())).drain(entry.getValue());
    }
  }

  @Test
  public void testRejoinDuringDrain() throws Exception {
    final ServerMap serverMap = mockServerMap(1);
    ToolkitObjectStripe[] stripeObjects = createObjectStripes(configuration, Collections.singletonList(serverMap), 1);

    AggregateServerMap<String, String> asm = new AggregateServerMap<String, String>(ToolkitObjectType.CACHE, mock(SearchFactory.class),
        mock(DistributedClusteredObjectLookup.class), "foo", stripeObjects, configuration,
        mock(Callable.class), serverMapLocalStoreFactory, platformService, mock(ToolkitLock.class));

    doThrow(new RejoinException()).when(serverMap).drain(anyMap());

    Map<String, BufferedOperation<String>> op = Collections.singletonMap("foo",
        (BufferedOperation<String>) mock(BufferedOperation.class));

    // Check that we're ignoring the rejoin exception.
    asm.drain(op);
  }

  @Test
  public void testCreateBufferedOperation() throws Exception {
    final List<ServerMap> serverMapList = mockServerMaps(256);
    ToolkitObjectStripe[] stripeObjects = createObjectStripes(configuration, serverMapList, 64);

    AggregateServerMap<String, String> asm = new AggregateServerMap<String, String>(ToolkitObjectType.CACHE, mock(SearchFactory.class),
        mock(DistributedClusteredObjectLookup.class), "foo", stripeObjects, configuration,
        mock(Callable.class), serverMapLocalStoreFactory, platformService, mock(ToolkitLock.class));

    asm.createBufferedOperation(BufferedOperation.Type.REMOVE, "a", "b", 1, 2, 3, 4);
    verify(serverMapList.get(Math.abs("a".hashCode() % 256))).createBufferedOperation(BufferedOperation.Type.REMOVE, "a", "b", 1, 2, 3, 4);
  }

  @Test
  public void testGetAllVersioned() throws Exception {
    final List<ServerMap> serverMapList = mockServerMaps(2);
    ToolkitObjectStripe[] stripeObjects = createObjectStripes(configuration, serverMapList, 1);

    AggregateServerMap<String, String> asm = new AggregateServerMap<String, String>(ToolkitObjectType.CACHE, mock(SearchFactory.class),
        mock(DistributedClusteredObjectLookup.class), "foo", stripeObjects, configuration,
        mock(Callable.class), serverMapLocalStoreFactory, platformService, mock(ToolkitLock.class)) {
      @Override
      protected TCObjectServerMap getAnyTCObjectServerMap() {
        return (TCObjectServerMap) serverMapList.get(0).__tc_managed();
      }

      @Override
      protected InternalToolkitMap<String, String> getAnyServerMap() {
        return serverMapList.get(0);
      }
    };

    SetMultimap<ObjectID, String> getAllRequest = HashMultimap.create();
    getAllRequest.put(new ObjectID(0), "foo");
    getAllRequest.put(new ObjectID(1), "bar");

    Map<String, VersionedValue<String>> expectedResult = Maps.newHashMap();
    expectedResult.put("foo", new VersionedValueImpl<String>("foo", 10));
    expectedResult.put("bar", new VersionedValueImpl<String>("bar", 12));
    when(serverMapList.get(0).getAllVersioned(getAllRequest)).thenReturn(expectedResult);

    // "foo".hashCode() is even, "bar".hashCode() is odd, neat.
    Map<String, VersionedValue<String>> result = asm.getAllVersioned(Arrays.asList("foo", "bar"));

    verify(serverMapList.get(0)).getAllVersioned(getAllRequest);
    assertThat(result, is(expectedResult));
  }

  private List<ServerMap> mockServerMaps(int number) {
    List<ServerMap> serverMaps = new ArrayList<ServerMap>();
    for (int i = 0; i < number; i++) {
      serverMaps.add(mockServerMap(i));
    }
    return serverMaps;
  }

  private ToolkitObjectStripe[] createObjectStripes(Configuration configuration, List<ServerMap> serverMaps, int mapsPerStripe) {
    List<ToolkitObjectStripe<ServerMap>> toolkitObjectStripes = new ArrayList<ToolkitObjectStripe<ServerMap>>();
    for (List<ServerMap> maps : Lists.partition(serverMaps, mapsPerStripe)) {
      toolkitObjectStripes.add(new ToolkitObjectStripeImpl<ServerMap>(configuration, maps.toArray(new ServerMap[maps.size()])));
    }
    return toolkitObjectStripes.toArray(new ToolkitObjectStripe[toolkitObjectStripes.size()]);
  }

  private ServerMap mockServerMap(long oid) {
    ServerMap serverMap = mock(ServerMap.class);
    TCObjectServerMap tcObject = mock(TCObjectServerMap.class);
    when(tcObject.getObjectID()).thenReturn(new ObjectID(oid));
    when(serverMap.__tc_managed()).thenReturn(tcObject);
    return serverMap;
  }
}
