package com.terracotta.management.l1bridge;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.terracotta.management.l1bridge.RemoteCallDescriptor;
import org.terracotta.management.resource.AgentEntityV2;
import org.terracotta.management.resource.AgentMetadataEntityV2;
import org.terracotta.management.resource.ResponseEntityV2;

import com.terracotta.management.security.impl.NullContextService;
import com.terracotta.management.security.impl.NullRequestTicketMonitor;
import com.terracotta.management.security.impl.NullUserService;
import com.terracotta.management.service.L1MBeansSource;
import com.terracotta.management.service.RemoteAgentBridgeService;
import com.terracotta.management.service.impl.TimeoutServiceImpl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Ludovic Orban
 */
public class RemoteAgentServiceV2Test {

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
  public void testGetAgents() throws Exception {
    RemoteAgentBridgeService remoteAgentBridgeService = mock(RemoteAgentBridgeService.class);
    L1MBeansSource l1MBeansSource = mock(L1MBeansSource.class);

    when(l1MBeansSource.containsJmxMBeans()).thenReturn(true);
    when(remoteAgentBridgeService.getRemoteAgentNodeNames()).thenReturn(Collections.singleton("node1"));
    when(remoteAgentBridgeService.getRemoteAgentNodeDetails(anyString())).thenReturn(new HashMap<String, String>(){{
      put("Agency", "Tst");
      put("Version", "1.2.3");
    }});

    RemoteAgentServiceV2 remoteAgentService = new RemoteAgentServiceV2(remoteAgentBridgeService, new NullContextService(), executorService, new NullRequestTicketMonitor(), new NullUserService(), new TimeoutServiceImpl(1000), l1MBeansSource);

    ResponseEntityV2<AgentEntityV2> agents = remoteAgentService.getAgents(Collections.<String>emptySet());
    assertThat(agents.getEntities().size(), is(1));
    AgentEntityV2 entity = agents.getEntities().iterator().next();
    assertThat(entity.getAgencyOf(), equalTo("Tst"));
  }

  @Test
  public void testGetAgentsProxyToActive() throws Exception {
    RemoteAgentBridgeService remoteAgentBridgeService = mock(RemoteAgentBridgeService.class);
    L1MBeansSource l1MBeansSource = mock(L1MBeansSource.class);

    when(l1MBeansSource.containsJmxMBeans()).thenReturn(false);
    when(l1MBeansSource.getActiveL2ContainingMBeansName()).thenReturn("server-1");

    RemoteAgentServiceV2 remoteAgentService = new RemoteAgentServiceV2(remoteAgentBridgeService, new NullContextService(), executorService, new NullRequestTicketMonitor(), new NullUserService(), new TimeoutServiceImpl(1000), l1MBeansSource);

    remoteAgentService.getAgents(Collections.<String>emptySet());
    verify(l1MBeansSource).proxyClientRequest();
  }

  @Test
  public void testGetAgentsFailWhenNoActive() throws Exception {
    RemoteAgentBridgeService remoteAgentBridgeService = mock(RemoteAgentBridgeService.class);
    L1MBeansSource l1MBeansSource = mock(L1MBeansSource.class);

    when(l1MBeansSource.containsJmxMBeans()).thenReturn(false);
    when(l1MBeansSource.getActiveL2ContainingMBeansName()).thenReturn(null);

    RemoteAgentServiceV2 remoteAgentService = new RemoteAgentServiceV2(remoteAgentBridgeService, new NullContextService(), executorService, new NullRequestTicketMonitor(), new NullUserService(), new TimeoutServiceImpl(1000), l1MBeansSource);

    remoteAgentService.getAgents(Collections.<String>emptySet());
    verify(l1MBeansSource).proxyClientRequest();
  }

  @Test
  public void testGetAgentsMetadata() throws Exception {
    RemoteAgentBridgeService remoteAgentBridgeService = mock(RemoteAgentBridgeService.class);
    L1MBeansSource l1MBeansSource = mock(L1MBeansSource.class);

    when(l1MBeansSource.containsJmxMBeans()).thenReturn(true);
    when(remoteAgentBridgeService.getRemoteAgentNodeNames()).thenReturn(Collections.singleton("node1"));
    when(remoteAgentBridgeService.getRemoteAgentNodeDetails(anyString())).thenReturn(new HashMap<String, String>(){{
      put("Agency", "Tst");
      put("Version", "1.2.3");
    }});

    AgentMetadataEntityV2 ame = new AgentMetadataEntityV2();
    ame.setAgencyOf("Tst");
    ResponseEntityV2<AgentMetadataEntityV2> responseEntity = new ResponseEntityV2<AgentMetadataEntityV2>();
    responseEntity.getEntities().add(ame);
    when(remoteAgentBridgeService.invokeRemoteMethod(anyString(), any(RemoteCallDescriptor.class))).thenReturn(serialize(responseEntity));

    RemoteAgentServiceV2 remoteAgentService = new RemoteAgentServiceV2(remoteAgentBridgeService, new NullContextService(), executorService, new NullRequestTicketMonitor(), new NullUserService(), new TimeoutServiceImpl(1000), l1MBeansSource);

    ResponseEntityV2<AgentMetadataEntityV2> agents = remoteAgentService.getAgentsMetadata(Collections.<String>emptySet());
    assertThat(agents.getEntities().size(), is(1));
    AgentMetadataEntityV2 entity = agents.getEntities().iterator().next();
    assertThat(entity.getAgentId(), equalTo("node1"));
    assertThat(entity.getAgencyOf(), equalTo("Tst"));
  }


  @Test
  public void testGetAgentsMetadataProxyToActive() throws Exception {
    RemoteAgentBridgeService remoteAgentBridgeService = mock(RemoteAgentBridgeService.class);
    L1MBeansSource l1MBeansSource = mock(L1MBeansSource.class);

    when(l1MBeansSource.containsJmxMBeans()).thenReturn(false);
    when(l1MBeansSource.getActiveL2ContainingMBeansName()).thenReturn("http://localhost:1234");

    RemoteAgentServiceV2 remoteAgentService = new RemoteAgentServiceV2(remoteAgentBridgeService, new NullContextService(), executorService, new NullRequestTicketMonitor(), new NullUserService(), new TimeoutServiceImpl(1000), l1MBeansSource);

    remoteAgentService.getAgentsMetadata(Collections.<String>emptySet());
    verify(l1MBeansSource).proxyClientRequest();
  }

  @Test
  public void testGetAgentsMetadataFailWhenNoActive() throws Exception {
    RemoteAgentBridgeService remoteAgentBridgeService = mock(RemoteAgentBridgeService.class);
    L1MBeansSource l1MBeansSource = mock(L1MBeansSource.class);

    when(l1MBeansSource.containsJmxMBeans()).thenReturn(false);
    when(l1MBeansSource.getActiveL2ContainingMBeansName()).thenReturn(null);

    RemoteAgentServiceV2 remoteAgentService = new RemoteAgentServiceV2(remoteAgentBridgeService, new NullContextService(), executorService, new NullRequestTicketMonitor(), new NullUserService(), new TimeoutServiceImpl(1000), l1MBeansSource);

    remoteAgentService.getAgentsMetadata(Collections.<String>emptySet());
    verify(l1MBeansSource).proxyClientRequest();
  }

  private static byte[] serialize(Object obj) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(baos);
    oos.writeObject(obj);
    oos.close();
    return baos.toByteArray();
  }

}
