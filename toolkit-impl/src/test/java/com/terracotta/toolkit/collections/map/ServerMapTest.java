package com.terracotta.toolkit.collections.map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.terracotta.toolkit.builder.ToolkitCacheConfigBuilder;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.internal.cache.VersionedValue;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.tc.object.ObjectID;
import com.tc.object.TCObjectServerMap;
import com.tc.object.VersionedObject;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheStore;
import com.tc.platform.PlatformService;
import com.tc.properties.TCPropertiesImpl;
import com.terracotta.toolkit.TerracottaToolkit;
import com.terracotta.toolkit.collections.servermap.api.ServerMapLocalStoreFactory;
import com.terracotta.toolkit.factory.impl.ToolkitCacheDistributedTypeFactory;
import com.terracotta.toolkit.object.serialization.SerializationStrategy;
import com.terracotta.toolkit.object.serialization.SerializedMapValue;
import com.terracotta.toolkit.rejoin.PlatformServiceProvider;
import com.terracotta.toolkit.search.SearchFactory;

import java.io.IOException;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

/**
 * @author tim
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(PlatformServiceProvider.class)
public class ServerMapTest {
  private PlatformService platformService;
  private Configuration configuration;
  private SerializationStrategy serializationStrategy;
  private TCObjectServerMap tcObjectServerMap;

  @Before
  public void setUp() throws Exception {
    configuration = defaultConfiguration();
    tcObjectServerMap = mock(TCObjectServerMap.class);
    when(tcObjectServerMap.getObjectID()).thenReturn(new ObjectID(1));
    platformService = mock(PlatformService.class);
    when(platformService.getTCProperties()).thenReturn(TCPropertiesImpl.getProperties());
    serializationStrategy = mock(SerializationStrategy.class);
    when(platformService.lookupRegisteredObjectByName(TerracottaToolkit.TOOLKIT_SERIALIZER_REGISTRATION_NAME, SerializationStrategy.class))
        .thenReturn(serializationStrategy);

    mockStatic(PlatformServiceProvider.class);
    when(PlatformServiceProvider.getPlatformService()).thenReturn(platformService);
  }

  @Test
  public void testGetAllVersioned() throws Exception {
    ServerMap serverMap = new ServerMap(configuration, "foo");
    serverMap.__tc_managed(tcObjectServerMap);

    SetMultimap<ObjectID, Object> request = HashMultimap.create();
    request.putAll(new ObjectID(1), Sets.<Object>newHashSet("a", "b"));
    request.putAll(new ObjectID(2), Sets.<Object>newHashSet("c", "d"));

    Map<Object, Object> response = Maps.newHashMap();
    response.put("a", new VersionedObject(mockSerializedMapValue("1"), 1));
    response.put("b", new VersionedObject(mockSerializedMapValue("2"), 2));
    response.put("c", new VersionedObject(mockSerializedMapValue("3"), 3));
    response.put("d", new VersionedObject(mockSerializedMapValue("4"), 4));

    when(tcObjectServerMap.getAllVersioned(request)).thenReturn(response);

    Map<String, VersionedValue<String>> result = serverMap.getAllVersioned(request);
    assertEquals(4, result.size());
    assertThat(result, hasEntry("a", versionedValue("1", 1)));
    assertThat(result, hasEntry("b", versionedValue("2", 2)));
    assertThat(result, hasEntry("c", versionedValue("3", 3)));
    assertThat(result, hasEntry("d", versionedValue("4", 4)));
  }

  private static <T> VersionedValue<T> versionedValue(T value, long version) {
    return new VersionedValueImpl<T>(value, version);
  }

  private static Configuration defaultConfiguration() {
    ToolkitCacheDistributedTypeFactory factory = new ToolkitCacheDistributedTypeFactory(mock(SearchFactory.class),
        mock(ServerMapLocalStoreFactory.class));
    return factory.newConfigForCreationInCluster(new ToolkitCacheConfigBuilder().build());
  }

  private static SerializedMapValue<String> mockSerializedMapValue(String value) throws IOException, ClassNotFoundException {
    SerializedMapValue<String> smv = mock(SerializedMapValue.class);
    when(smv.getDeserializedValue(any(SerializationStrategy.class), anyBoolean(), any(L1ServerMapLocalCacheStore.class), any(), anyBoolean()))
        .thenReturn(value);
    return smv;
  }
}
