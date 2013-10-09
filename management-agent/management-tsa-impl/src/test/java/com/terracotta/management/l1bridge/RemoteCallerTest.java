package com.terracotta.management.l1bridge;

import org.junit.Test;
import org.mockito.Matchers;
import org.terracotta.management.l1bridge.RemoteAgentEndpoint;
import org.terracotta.management.l1bridge.RemoteCallDescriptor;
import org.terracotta.management.resource.AgentEntity;

import com.terracotta.management.security.ContextService;
import com.terracotta.management.security.RequestTicketMonitor;
import com.terracotta.management.security.UserService;
import com.terracotta.management.service.RemoteAgentBridgeService;
import com.terracotta.management.user.UserRole;
import com.terracotta.management.user.impl.DfltUserInfo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isIn;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Ludovic Orban
 */
public class RemoteCallerTest {

  private final static byte[] SERIALIZED_AGENT_ENTITY;
  private final static byte[] SERIALIZED_AGENT_ENTITY_COLLECTION;

  static {
    try {
      SERIALIZED_AGENT_ENTITY = serialize(new AgentEntity());
      SERIALIZED_AGENT_ENTITY_COLLECTION = serialize(new ArrayList<AgentEntity>() {{
        add(new AgentEntity());
      }});
    } catch (IOException ioe) {
      throw new AssertionError();
    }
  }

  private static byte[] serialize(Object obj) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(baos);
    oos.writeObject(obj);
    oos.close();
    return baos.toByteArray();
  }


  @Test
  public void test_getRemoteAgentNodeNames_delegates_to_RemoteAgentBridgeService() throws Exception {
    RemoteAgentBridgeService remoteAgentBridgeService = mock(RemoteAgentBridgeService.class);
    ContextService contextService = null;
    ExecutorService executorService = null;
    RequestTicketMonitor requestTicketMonitor = null;
    UserService userService = null;
    RemoteCaller remoteCaller = new RemoteCaller(remoteAgentBridgeService, contextService, executorService, requestTicketMonitor, userService);

    Set<String> agentNodeNames = new HashSet<String>();
    agentNodeNames.add("localhost.home_59822");

    when(remoteAgentBridgeService.getRemoteAgentNodeNames()).thenReturn(agentNodeNames);

    Set<String> remoteAgentNodeNames = remoteCaller.getRemoteAgentNodeNames();

    assertThat(remoteAgentNodeNames, equalTo(agentNodeNames));
  }

  @Test
  public void test_getRemoteAgentNodeDetails_delegates_to_RemoteAgentBridgeService() throws Exception {
    RemoteAgentBridgeService remoteAgentBridgeService = mock(RemoteAgentBridgeService.class);
    ContextService contextService = null;
    ExecutorService executorService = null;
    RequestTicketMonitor requestTicketMonitor = null;
    UserService userService = null;
    RemoteCaller remoteCaller = new RemoteCaller(remoteAgentBridgeService, contextService, executorService, requestTicketMonitor, userService);

    Map<String, Map<String, String>> agentNodeDetails = new HashMap<String, Map<String, String>>();
    agentNodeDetails.put("localhost.home_59822", new HashMap<String, String>() {{
      put("Version", "123");
      put("Agency", "test");
    }});

    when(remoteAgentBridgeService.getRemoteAgentNodeDetails()).thenReturn(agentNodeDetails);

    Map<String, Map<String, String>> remoteAgentNodeDetails = remoteCaller.getRemoteAgentNodeDetails();

    assertThat(remoteAgentNodeDetails, equalTo(agentNodeDetails));
  }

  @Test
  public void test_call_returns_remote_object() throws Exception {
    RemoteAgentBridgeService remoteAgentBridgeService = mock(RemoteAgentBridgeService.class);
    ContextService contextService = mock(ContextService.class);
    ExecutorService executorService = Executors.newCachedThreadPool();
    RequestTicketMonitor requestTicketMonitor = mock(RequestTicketMonitor.class);
    UserService userService = mock(UserService.class);
    RemoteCaller remoteCaller = new RemoteCaller(remoteAgentBridgeService, contextService, executorService, requestTicketMonitor, userService);

    when(requestTicketMonitor.issueRequestTicket()).thenReturn("test-ticket");
    DfltUserInfo userInfo = new DfltUserInfo("testUser", "testPwHash", Collections.singleton(UserRole.TERRACOTTA));
    when(contextService.getUserInfo()).thenReturn(userInfo);
    when(userService.putUserInfo(userInfo)).thenReturn("test-token");

    String nodeName = "test-nodename";
    when(remoteAgentBridgeService.invokeRemoteMethod(eq(nodeName), eq(RemoteAgentEndpoint.class), Matchers.any(RemoteCallDescriptor.class))).thenReturn(SERIALIZED_AGENT_ENTITY);

    Object response = remoteCaller.call(nodeName, "myService", RemoteAgentEndpoint.class.getMethod("getVersion"), new Object[0]);
    assertThat(((AgentEntity) response).getAgentId(), equalTo(nodeName));
  }

  @Test
  public void test_fanOutCollectionCall_returns_remote_object() throws Exception {
    RemoteAgentBridgeService remoteAgentBridgeService = mock(RemoteAgentBridgeService.class);
    ContextService contextService = mock(ContextService.class);
    ExecutorService executorService = Executors.newCachedThreadPool();
    RequestTicketMonitor requestTicketMonitor = mock(RequestTicketMonitor.class);
    UserService userService = mock(UserService.class);
    RemoteCaller remoteCaller = new RemoteCaller(remoteAgentBridgeService, contextService, executorService, requestTicketMonitor, userService);

    when(requestTicketMonitor.issueRequestTicket()).thenReturn("test-ticket");
    DfltUserInfo userInfo = new DfltUserInfo("testUser", "testPwHash", Collections.singleton(UserRole.TERRACOTTA));
    when(contextService.getUserInfo()).thenReturn(userInfo);
    when(userService.putUserInfo(userInfo)).thenReturn("test-token");

    Set<String> nodeNames = new HashSet<String>() {{
      add("test-nodename-1");
      add("test-nodename-2");
      add("test-nodename-3");
    }};
    when(remoteAgentBridgeService.invokeRemoteMethod(anyString(), eq(RemoteAgentEndpoint.class), Matchers.any(RemoteCallDescriptor.class))).thenReturn(SERIALIZED_AGENT_ENTITY_COLLECTION);

    Object response = remoteCaller.fanOutCollectionCall(nodeNames, "myService", RemoteAgentEndpoint.class.getMethod("getVersion"), new Object[0]);
    Collection<AgentEntity> responseCollection = (Collection<AgentEntity>)response;
    assertThat(responseCollection.size(), equalTo(nodeNames.size()));

    for (AgentEntity agentEntity : responseCollection) {
      assertThat(agentEntity.getAgentId(), isIn(nodeNames));
    }
  }


}
