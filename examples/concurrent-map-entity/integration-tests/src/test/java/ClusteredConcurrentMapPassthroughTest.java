/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.terracotta.connection.Connection;
import org.terracotta.connection.ConnectionFactory;
import org.terracotta.connection.entity.EntityRef;
import org.terracotta.entity.map.TerracottaClusteredMapClientService;
import org.terracotta.entity.map.ConcurrentClusteredMap;
import org.terracotta.entity.map.server.TerracottaClusteredMapService;
import org.terracotta.passthrough.PassthroughClusterControl;
import org.terracotta.passthrough.PassthroughServer;
import org.terracotta.passthrough.PassthroughServerRegistry;
import org.terracotta.passthrough.PassthroughTestHelpers;

import java.io.Serializable;
import java.net.URI;
import java.util.Properties;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import org.terracotta.entity.map.MapConfig;

/**
 * ClusteredConcurrentMapPassthroughTest
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class ClusteredConcurrentMapPassthroughTest {

  private static final String MAP_NAME = "my-map";
  private static final String SERVER_NAME = "testServer";
  private static final String CLUSTER_URI = "passthrough://" + SERVER_NAME;

  private ConcurrentClusteredMap<Long, String> clusteredMap;
  private PassthroughClusterControl clusterControl;

  @Before
  public void setUp() throws Exception {
    clusterControl = PassthroughTestHelpers.createActiveOnly("test", new PassthroughTestHelpers.ServerInitializer() {
      @Override
      public void registerServicesForServer(PassthroughServer passthroughServer) {
        passthroughServer.setServerName(SERVER_NAME);
        passthroughServer.registerClientEntityService(new TerracottaClusteredMapClientService());
        passthroughServer.registerServerEntityService(new TerracottaClusteredMapService());
        PassthroughServerRegistry.getSharedInstance().registerServer(SERVER_NAME, passthroughServer);
      }
    });
    Connection connection = ConnectionFactory.connect(URI.create(CLUSTER_URI), new Properties());
    EntityRef<ConcurrentClusteredMap, Object, Object> entityRef = connection.getEntityRef(ConcurrentClusteredMap.class, ConcurrentClusteredMap.VERSION, MAP_NAME);
    entityRef.create(new MapConfig(4, "test"));
    clusteredMap = entityRef.fetchEntity(null);
    clusteredMap.setTypes(Long.class, String.class);
  }

  @After
  public void tearDown() {
    clusterControl.terminateAllServers();
  }

  @Test
  public void testBasicMapInteraction() throws Exception {
    long key = 42L;
    String value = "The answer!";
    clusteredMap.put(key, value);
    assertThat(clusteredMap.get(key), is(value));
    assertThat(clusteredMap.remove(key), is(value));
  }

  @Test
  public void testMultiClientBasicInteraction() throws Exception {
    long key = 1L;
    String value = "see that?";
    clusteredMap.put(key, value);

    Connection connection = ConnectionFactory.connect(URI.create(CLUSTER_URI), new Properties());
    EntityRef<ConcurrentClusteredMap, Object, Object> entityRef = connection.getEntityRef(ConcurrentClusteredMap.class, ConcurrentClusteredMap.VERSION, MAP_NAME);
    ConcurrentClusteredMap<Long, String> mapFromOtherClient = entityRef.fetchEntity(null);
    mapFromOtherClient.setTypes(Long.class, String.class);

    assertThat(mapFromOtherClient.get(key), is(value));
  }

  @Test
  public void testCASOperations() throws Exception {
    long key = 244L;
    String value1 = "Tadam!";
    String value2 = "Youhou";
    String value3 = "Boom";

    assertThat(clusteredMap.putIfAbsent(key, value1), nullValue());

    assertThat(clusteredMap.putIfAbsent(key, value2), is(value1));

    assertThat(clusteredMap.replace(key, value2), is(value1));

    assertThat(clusteredMap.replace(key, value2, value3), is(true));

    assertThat(clusteredMap.remove(key, value3), is(true));
  }

  @Test
  public void testBulkOps() throws Exception {
    clusteredMap.put(1L, "One");
    clusteredMap.put(2L, "Two");
    clusteredMap.put(3L, "Three");

    assertThat(clusteredMap.keySet(), containsInAnyOrder(1L, 2L, 3L));
    assertThat(clusteredMap.values(), containsInAnyOrder("One", "Two", "Three"));
    assertThat(clusteredMap.entrySet().size(), is(3));
  }

  @Test
  public void testWithCustomType() throws Exception {
    Connection connection = ConnectionFactory.connect(URI.create(CLUSTER_URI), new Properties());
    EntityRef<ConcurrentClusteredMap, Object, Object> entityRef = connection.getEntityRef(ConcurrentClusteredMap.class, ConcurrentClusteredMap.VERSION, "person-map");
    entityRef.create(new MapConfig(4, "test"));
    ConcurrentClusteredMap<Long, Person> map = entityRef.fetchEntity(null);
    map.setTypes(Long.class, Person.class);

    map.put(33L, new Person("Iron Man", 33));
    map.close();

    connection = ConnectionFactory.connect(URI.create(CLUSTER_URI), new Properties());
    entityRef = connection.getEntityRef(ConcurrentClusteredMap.class, ConcurrentClusteredMap.VERSION, "person-map");
    map = entityRef.fetchEntity(null);
    map.setTypes(Long.class, Person.class);

    assertThat(map.get(33L).name, is("Iron Man"));
    map.close();
  }

  public static class Person implements Serializable  {
    private static final long serialVersionUID = 1L;
    final String name;
    final int age;

    public Person(String name, int age) {
      this.name = name;
      this.age = age;
    }
  }
}
