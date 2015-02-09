package com.terracotta.management.l1bridge;

import com.terracotta.management.security.impl.NullContextService;
import com.terracotta.management.security.impl.NullRequestTicketMonitor;
import com.terracotta.management.security.impl.NullUserService;
import com.terracotta.management.service.L1MBeansSource;
import com.terracotta.management.service.RemoteAgentBridgeService;
import com.terracotta.management.service.impl.TimeoutServiceImpl;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.terracotta.management.l1bridge.RemoteCallDescriptor;
import org.terracotta.management.resource.AbstractEntityV2;
import org.terracotta.management.resource.ResponseEntityV2;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Ludovic Orban
 */
public class RemoteServiceStubGeneratorV2Test {


  private ExecutorService executorService;

  @Before
  public void setUp() throws Exception {
    executorService = Executors.newSingleThreadExecutor();
  }

  @After
  public void tearDown() throws Exception {
    executorService.shutdown();
  }


  @Test
  public void testPassiveProxyingToActive() throws Exception {
    RemoteRequestValidator remoteRequestValidator = mock(RemoteRequestValidator.class);
    RemoteAgentBridgeService remoteAgentBridgeService = mock(RemoteAgentBridgeService.class);
    L1MBeansSource l1MBeansSource = mock(L1MBeansSource.class);
    RemoteServiceStubGenerator remoteServiceStubGenerator = new RemoteServiceStubGenerator(new NullRequestTicketMonitor(), new NullUserService(), new NullContextService(), remoteRequestValidator, remoteAgentBridgeService, executorService, new TimeoutServiceImpl(1000), l1MBeansSource);

    when(l1MBeansSource.containsJmxMBeans()).thenReturn(false);
    when(l1MBeansSource.getActiveL2ContainingMBeansName()).thenReturn("host-1");

    DummyCacheService cacheService = remoteServiceStubGenerator.newRemoteService(DummyCacheService.class, "cache");
    cacheService.getCaches();
    verify(l1MBeansSource).proxyClientRequest();
  }

  @Test
  public void testAgencyFilteringAndMergingWithMultipleNodes() throws Exception {
    ResponseEntityV2<DummyCacheEntity> caches = new ResponseEntityV2<DummyCacheEntity>();
    caches.getEntities().addAll(Arrays.asList(new DummyCacheEntity("cache-1"), new DummyCacheEntity("cache-2"), new DummyCacheEntity("cache-3")));
    final byte[] serializedCaches = serialize(caches);
    ResponseEntityV2<DummySessionEntity> sessions1 = new ResponseEntityV2<DummySessionEntity>();
    sessions1.getEntities().addAll(Arrays.asList(new DummySessionEntity("sess-1"), new DummySessionEntity("sess-2")));
    final byte[] serializedSessions1 = serialize(sessions1);
    ResponseEntityV2<DummySessionEntity> sessions2 = new ResponseEntityV2<DummySessionEntity>();
    sessions2.getEntities().addAll(Arrays.asList(new DummySessionEntity("sess-3"), new DummySessionEntity("sess-4")));
    final byte[] serializedSessions2 = serialize(sessions2);

    RemoteRequestValidator remoteRequestValidator = mock(RemoteRequestValidator.class);
    RemoteAgentBridgeService remoteAgentBridgeService = mock(RemoteAgentBridgeService.class);
    L1MBeansSource l1MBeansSource = mock(L1MBeansSource.class);
    RemoteServiceStubGeneratorV2 remoteServiceStubGenerator = new RemoteServiceStubGeneratorV2(new NullRequestTicketMonitor(), new NullUserService(), new NullContextService(), remoteRequestValidator, remoteAgentBridgeService, executorService, new TimeoutServiceImpl(1000), l1MBeansSource);

    when(l1MBeansSource.containsJmxMBeans()).thenReturn(true);
    when(remoteRequestValidator.getValidatedNodes()).thenReturn(new HashSet<String>(Arrays.asList("node_cache", "node1_session", "node2_session")));
    when(remoteRequestValidator.getSingleValidatedNode()).thenThrow(new RuntimeException("Multiple nodes were specified"));
    when(remoteAgentBridgeService.getRemoteAgentNodeDetails(eq("node_cache"))).thenReturn(new HashMap<String, String>() {{
      put("Agency", "cache");
    }});
    when(remoteAgentBridgeService.getRemoteAgentNodeDetails(eq("node1_session"))).thenReturn(new HashMap<String, String>() {{
      put("Agency", "session");
    }});
    when(remoteAgentBridgeService.getRemoteAgentNodeDetails(eq("node2_session"))).thenReturn(new HashMap<String, String>() {{
      put("Agency", "session");
    }});
    when(remoteAgentBridgeService.invokeRemoteMethod(eq("node_cache"), any(RemoteCallDescriptor.class))).thenReturn(serializedCaches);
    when(remoteAgentBridgeService.invokeRemoteMethod(eq("node1_session"), any(RemoteCallDescriptor.class))).thenReturn(serializedSessions1);
    when(remoteAgentBridgeService.invokeRemoteMethod(eq("node2_session"), any(RemoteCallDescriptor.class))).thenReturn(serializedSessions2);

    DummyCacheService cacheService = remoteServiceStubGenerator.newRemoteService(DummyCacheService.class, "cache");
    DummySessionService sessionService = remoteServiceStubGenerator.newRemoteService(DummySessionService.class, "session");

    assertThat(new HashSet<DummyCacheEntity>(cacheService.getCaches().getEntities()),
        equalTo(new HashSet<DummyCacheEntity>(caches.getEntities())));
    assertThatAgentIdIsIn(cacheService.getCaches(), "node_cache");

    assertThat(new HashSet<DummySessionEntity>(sessionService.getSessions().getEntities()),
        equalTo(new HashSet<DummySessionEntity>(merge(sessions1, sessions2).getEntities())));
    assertThatAgentIdIsIn(sessionService.getSessions(), "node1_session", "node2_session");
  }

  @Test
  public void testAgencyFilteringWithSingleNode() throws Exception {
    ResponseEntityV2<DummyCacheEntity> caches = new ResponseEntityV2<DummyCacheEntity>();
    caches.getEntities().addAll(Arrays.asList(new DummyCacheEntity("cache-1"), new DummyCacheEntity("cache-2"), new DummyCacheEntity("cache-3")));
    final byte[] serializedCaches = serialize(caches);

    RemoteRequestValidator remoteRequestValidator = mock(RemoteRequestValidator.class);
    RemoteAgentBridgeService remoteAgentBridgeService = mock(RemoteAgentBridgeService.class);
    L1MBeansSource l1MBeansSource = mock(L1MBeansSource.class);
    RemoteServiceStubGeneratorV2 remoteServiceStubGenerator = new RemoteServiceStubGeneratorV2(new NullRequestTicketMonitor(), new NullUserService(), new NullContextService(), remoteRequestValidator, remoteAgentBridgeService, executorService, new TimeoutServiceImpl(1000), l1MBeansSource);

    when(l1MBeansSource.containsJmxMBeans()).thenReturn(true);
    when(remoteRequestValidator.getValidatedNodes()).thenReturn(new HashSet<String>(Arrays.asList("node_cache")));
    when(remoteRequestValidator.getSingleValidatedNode()).thenReturn("node_cache");
    when(remoteAgentBridgeService.getRemoteAgentNodeDetails(eq("node_cache"))).thenReturn(new HashMap<String, String>() {{
      put("Agency", "cache");
    }});
    when(remoteAgentBridgeService.invokeRemoteMethod(eq("node_cache"), any(RemoteCallDescriptor.class))).thenReturn(serializedCaches);

    DummyCacheService cacheService = remoteServiceStubGenerator.newRemoteService(DummyCacheService.class, "cache");
    assertThat(cacheService.getCaches().getEntities(), equalTo(caches.getEntities()));
    assertThatAgentIdIsIn(cacheService.getCaches(), "node_cache");

    DummySessionService sessionService = remoteServiceStubGenerator.newRemoteService(DummySessionService.class, "session");
    assertThat(sessionService.getSessions().getEntities(), equalTo(new ResponseEntityV2<DummySessionEntity>().getEntities()));
  }

  private static void assertThatAgentIdIsIn(ResponseEntityV2<? extends AbstractEntityV2> response, String... ids) {
    Collection<? extends AbstractEntityV2> entities = response.getEntities();
    for (AbstractEntityV2 entity : entities) {
      assertThat(entity.getAgentId(), Matchers.isOneOf(ids));
    }
  }

  private static <T extends AbstractEntityV2> ResponseEntityV2<T> merge(ResponseEntityV2<T> r1, ResponseEntityV2<T> r2) {
    ResponseEntityV2<T> result = new ResponseEntityV2<T>();
    result.getEntities().addAll(r1.getEntities());
    result.getEntities().addAll(r2.getEntities());
    return result;
  }

  private static byte[] serialize(Object obj) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(baos);
    oos.writeObject(obj);
    oos.close();
    return baos.toByteArray();
  }

  static class DummyCacheEntity extends AbstractEntityV2 {
    private String cacheName;

    DummyCacheEntity(String cacheName) {
      this.cacheName = cacheName;
    }

    public void setCacheName(String cacheName) {
      this.cacheName = cacheName;
    }

    public String getCacheName() {
      return cacheName;
    }

    @Override
    public int hashCode() {
      return cacheName.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof DummyCacheEntity) {
        DummyCacheEntity other = (DummyCacheEntity)obj;
        return other.cacheName.equals(cacheName);
      }
      return false;
    }
  }

  static class DummySessionEntity extends AbstractEntityV2 {
    private String sessionName;

    DummySessionEntity(String sessionName) {
      this.sessionName = sessionName;
    }

    public String getSessionName() {
      return sessionName;
    }

    public void setSessionName(String sessionName) {
      this.sessionName = sessionName;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof DummySessionEntity) {
        DummySessionEntity other = (DummySessionEntity)obj;
        return other.sessionName.equals(sessionName);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return sessionName.hashCode();
    }
  }

  static interface DummyCacheService {
    ResponseEntityV2<DummyCacheEntity> getCaches();
  }

  static interface DummySessionService {
    ResponseEntityV2<DummySessionEntity> getSessions();
  }
}
