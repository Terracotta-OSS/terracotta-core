package com.terracotta.management.l1bridge;

import com.terracotta.management.l1bridge.util.RemoteCallerUtility;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.terracotta.management.l1bridge.RemoteAgentEndpoint;
import org.terracotta.management.l1bridge.RemoteCallDescriptor;
import org.terracotta.management.resource.AgentEntity;

import com.terracotta.management.security.ContextService;
import com.terracotta.management.security.RequestTicketMonitor;
import com.terracotta.management.security.UserService;
import com.terracotta.management.service.RemoteAgentBridgeService;
import com.terracotta.management.service.TimeoutService;
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
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasEntry;
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
  public void when_getRemoteAgentNodeNames_then_call_is_delegated_to_RemoteAgentBridgeService() throws Exception {
    RemoteAgentBridgeService remoteAgentBridgeService = mock(RemoteAgentBridgeService.class);
    RemoteCaller remoteCaller = new RemoteCaller(remoteAgentBridgeService, null, null, null, null, null);

    Set<String> agentNodeNames = new HashSet<String>();
    agentNodeNames.add("localhost.home_59822");

    when(remoteAgentBridgeService.getRemoteAgentNodeNames()).thenReturn(agentNodeNames);

    Set<String> remoteAgentNodeNames = remoteCaller.getRemoteAgentNodeNames();

    assertThat(remoteAgentNodeNames, equalTo(agentNodeNames));
  }

  @Test
  public void when_getRemoteAgentNodeDetails_then_call_is_delegated_to_RemoteAgentBridgeService() throws Exception {
    TimeoutService timeoutService = mock(TimeoutService.class);
    RemoteAgentBridgeService remoteAgentBridgeService = mock(RemoteAgentBridgeService.class);
    RemoteCaller remoteCaller = new RemoteCaller(remoteAgentBridgeService, null, Executors.newCachedThreadPool(),
            null, null, timeoutService);

    Map<String, String> agentNodeDetails = new HashMap<String, String>();
    agentNodeDetails.put("Version", "123");
    agentNodeDetails.put("Agency", "test");

    Set<String> remoteAgentNodeNames = new HashSet<String>(){{add("localhost.home_59822");}};
    when(remoteAgentBridgeService.getRemoteAgentNodeNames()).thenReturn(remoteAgentNodeNames);

    when(remoteAgentBridgeService.getRemoteAgentNodeDetails("localhost.home_59822")).thenReturn(agentNodeDetails);
    when(timeoutService.getCallTimeout()).thenReturn(1000L);

    Map<String, Map<String, String>> remoteAgentNodeDetails = remoteCaller.getRemoteAgentNodeDetails();
    Map<String, Map<String, String>> operand = new HashMap<String, Map<String, String>>();
    operand.put("localhost.home_59822", agentNodeDetails);
    assertThat(remoteAgentNodeDetails, equalTo(operand));
    assertThat(remoteAgentNodeDetails, hasEntry(equalTo("localhost.home_59822"), equalTo(agentNodeDetails)));
  }

  @Test
  public void when_call_then_remote_entity_has_corresponding_agentId() throws Exception {
    TimeoutService timeoutService = mock(TimeoutService.class);
    RemoteAgentBridgeService remoteAgentBridgeService = mock(RemoteAgentBridgeService.class);
    ContextService contextService = mock(ContextService.class);
    ExecutorService executorService = Executors.newCachedThreadPool();
    RequestTicketMonitor requestTicketMonitor = mock(RequestTicketMonitor.class);
    UserService userService = mock(UserService.class);
    RemoteCallerUtility remoteCallerUtility = mock(RemoteCallerUtility.class);
    when(remoteCallerUtility.fetchClientUUIDs()).thenReturn(new HashSet<String>());
    RemoteCaller remoteCaller = new RemoteCaller(remoteAgentBridgeService, contextService, executorService, requestTicketMonitor, userService, timeoutService,remoteCallerUtility);

    when(requestTicketMonitor.issueRequestTicket()).thenReturn("test-ticket");
    DfltUserInfo userInfo = new DfltUserInfo("testUser", "testPwHash", Collections.singleton(UserRole.TERRACOTTA));
    when(contextService.getUserInfo()).thenReturn(userInfo);
    when(userService.putUserInfo(userInfo)).thenReturn("test-token");
    String nodeName = "test-nodename";
    when(remoteAgentBridgeService.invokeRemoteMethod(eq(nodeName), Matchers.any(RemoteCallDescriptor.class))).thenReturn(SERIALIZED_AGENT_ENTITY);

    Object response = remoteCaller.call(nodeName, "myService", RemoteAgentEndpoint.class.getMethod("getVersion"), new Object[0]);
    assertThat(((AgentEntity) response).getAgentId(), equalTo(nodeName));
  }

  @Test
  public void when_call_then_remote_collection_has_corresponding_agentId() throws Exception {
    TimeoutService timeoutService = mock(TimeoutService.class);
    RemoteAgentBridgeService remoteAgentBridgeService = mock(RemoteAgentBridgeService.class);
    ContextService contextService = mock(ContextService.class);
    ExecutorService executorService = Executors.newCachedThreadPool();
    RequestTicketMonitor requestTicketMonitor = mock(RequestTicketMonitor.class);
    UserService userService = mock(UserService.class);
    RemoteCallerUtility remoteCallerUtility = mock(RemoteCallerUtility.class);
    when(remoteCallerUtility.fetchClientUUIDs()).thenReturn(new HashSet<String>());
    RemoteCaller remoteCaller = new RemoteCaller(remoteAgentBridgeService, contextService, executorService, requestTicketMonitor, userService, timeoutService,remoteCallerUtility);

    when(requestTicketMonitor.issueRequestTicket()).thenReturn("test-ticket");
    DfltUserInfo userInfo = new DfltUserInfo("testUser", "testPwHash", Collections.singleton(UserRole.TERRACOTTA));
    when(contextService.getUserInfo()).thenReturn(userInfo);
    when(userService.putUserInfo(userInfo)).thenReturn("test-token");
    String nodeName = "test-nodename";
    when(remoteAgentBridgeService.invokeRemoteMethod(eq(nodeName), Matchers.any(RemoteCallDescriptor.class))).thenReturn(SERIALIZED_AGENT_ENTITY_COLLECTION);

    Object response = remoteCaller.call(nodeName, "myService", RemoteAgentEndpoint.class.getMethod("getVersion"), new Object[0]);
    Collection<AgentEntity> collection = (Collection<AgentEntity>)response;
    for (AgentEntity agentEntity : collection) {
      assertThat(agentEntity.getAgentId(), equalTo(nodeName));
    }
  }

  @Test
  public void when_fanOutCollectionCall_any_agency_with_3_nodes_then_remote_collection_has_3_corresponding_agentIds() throws Exception {
    TimeoutService timeoutService = mock(TimeoutService.class);
    RemoteAgentBridgeService remoteAgentBridgeService = mock(RemoteAgentBridgeService.class);
    ContextService contextService = mock(ContextService.class);
    ExecutorService executorService = Executors.newCachedThreadPool();
    RequestTicketMonitor requestTicketMonitor = mock(RequestTicketMonitor.class);
    UserService userService = mock(UserService.class);
    RemoteCallerUtility remoteCallerUtility = mock(RemoteCallerUtility.class);
    when(remoteCallerUtility.fetchClientUUIDs()).thenReturn(new HashSet<String>());
    RemoteCaller remoteCaller = new RemoteCaller(remoteAgentBridgeService, contextService, executorService, requestTicketMonitor, userService, timeoutService,remoteCallerUtility);

    when(remoteAgentBridgeService.getRemoteAgentNodeDetails("test-nodename-1")).thenReturn(new HashMap<String, String>() {{
      put("Version", "123");
      put("Agency", "test");
    }});
    when(remoteAgentBridgeService.getRemoteAgentNodeDetails("test-nodename-2")).thenReturn(new HashMap<String, String>() {{
      put("Version", "123");
      put("Agency", "test");
    }});
    when(remoteAgentBridgeService.getRemoteAgentNodeDetails("test-nodename-3")).thenReturn(new HashMap<String, String>() {{
      put("Version", "123");
      put("Agency", "test");
    }});
    when(requestTicketMonitor.issueRequestTicket()).thenReturn("test-ticket");
    DfltUserInfo userInfo = new DfltUserInfo("testUser", "testPwHash", Collections.singleton(UserRole.TERRACOTTA));
    when(contextService.getUserInfo()).thenReturn(userInfo);
    when(userService.putUserInfo(userInfo)).thenReturn("test-token");
    Set<String> nodeNames = new HashSet<String>() {{
      add("test-nodename-1");
      add("test-nodename-2");
      add("test-nodename-3");
    }};
    when(remoteAgentBridgeService.invokeRemoteMethod(anyString(), Matchers.any(RemoteCallDescriptor.class))).thenReturn(SERIALIZED_AGENT_ENTITY_COLLECTION);
    when(timeoutService.getCallTimeout()).thenReturn(1000L);


    Object response = remoteCaller.fanOutCollectionCall(null, nodeNames, "myService", RemoteAgentEndpoint.class.getMethod("getVersion"), new Object[0]);
    Collection<AgentEntity> responseCollection = (Collection<AgentEntity>)response;

    Set<String> remoteAgentIds = new HashSet<String>();
    for (AgentEntity agentEntity : responseCollection) {
      String agentId = agentEntity.getAgentId();
      remoteAgentIds.add(agentId);
    }

    assertThat(remoteAgentIds, containsInAnyOrder("test-nodename-1", "test-nodename-2", "test-nodename-3"));
  }

  @Test
  public void when_fanOutCollectionCall_filtering_agencies_with_3_nodes_then_remote_collection_has_3_corresponding_agentIds() throws Exception {
    TimeoutService timeoutService = mock(TimeoutService.class);
    RemoteAgentBridgeService remoteAgentBridgeService = mock(RemoteAgentBridgeService.class);
    ContextService contextService = mock(ContextService.class);
    ExecutorService executorService = Executors.newCachedThreadPool();
    RequestTicketMonitor requestTicketMonitor = mock(RequestTicketMonitor.class);
    UserService userService = mock(UserService.class);
    RemoteCallerUtility remoteCallerUtility = mock(RemoteCallerUtility.class);
    when(remoteCallerUtility.fetchClientUUIDs()).thenReturn(new HashSet<String>());
    RemoteCaller remoteCaller = new RemoteCaller(remoteAgentBridgeService, contextService, executorService, requestTicketMonitor, userService, timeoutService,remoteCallerUtility);

    Set<String> remoteAgentNodeNames = new HashSet<String>(){{add("test-nodename-1");add("test-nodename-2");add("test-nodename-3");}};
    when(remoteAgentBridgeService.getRemoteAgentNodeNames()).thenReturn(remoteAgentNodeNames);

    when(remoteAgentBridgeService.getRemoteAgentNodeDetails("test-nodename-1")).thenReturn(new HashMap<String, String>() {{
      put("Version", "123");
      put("Agency", "test");
    }});
    when(remoteAgentBridgeService.getRemoteAgentNodeDetails("test-nodename-2")).thenReturn(new HashMap<String, String>() {{
      put("Version", "123");
      put("Agency", "test");
    }});
    when(remoteAgentBridgeService.getRemoteAgentNodeDetails("test-nodename-3")).thenReturn(new HashMap<String, String>() {{
      put("Version", "123");
      put("Agency", "test");
    }});
    when(requestTicketMonitor.issueRequestTicket()).thenReturn("test-ticket");
    DfltUserInfo userInfo = new DfltUserInfo("testUser", "testPwHash", Collections.singleton(UserRole.TERRACOTTA));
    when(contextService.getUserInfo()).thenReturn(userInfo);
    when(userService.putUserInfo(userInfo)).thenReturn("test-token");
    when(timeoutService.getCallTimeout()).thenReturn(1000L);

    Set<String> nodeNames = new HashSet<String>() {{
      add("test-nodename-1");
      add("test-nodename-2");
      add("test-nodename-3");
    }};
    when(remoteAgentBridgeService.invokeRemoteMethod(anyString(), Matchers.any(RemoteCallDescriptor.class))).thenReturn(SERIALIZED_AGENT_ENTITY_COLLECTION);

    Object response = remoteCaller.fanOutCollectionCall("test", nodeNames, "myService", RemoteAgentEndpoint.class.getMethod("getVersion"), new Object[0]);
    Collection<AgentEntity> responseCollection = (Collection<AgentEntity>)response;

    Set<String> remoteAgentIds = new HashSet<String>();
    for (AgentEntity agentEntity : responseCollection) {
      String agentId = agentEntity.getAgentId();
      remoteAgentIds.add(agentId);
    }

    assertThat(remoteAgentIds, containsInAnyOrder("test-nodename-1", "test-nodename-2", "test-nodename-3"));
  }

  @Test
  public void when_fanOutCollectionCall_filtering_agencies_with_4_nodes_filtering_out_1_then_remote_collection_has_3_corresponding_agentIds() throws Exception {
    TimeoutService timeoutService = mock(TimeoutService.class);
    RemoteAgentBridgeService remoteAgentBridgeService = mock(RemoteAgentBridgeService.class);
    ContextService contextService = mock(ContextService.class);
    ExecutorService executorService = Executors.newCachedThreadPool();
    RequestTicketMonitor requestTicketMonitor = mock(RequestTicketMonitor.class);
    UserService userService = mock(UserService.class);
    RemoteCallerUtility remoteCallerUtility = mock(RemoteCallerUtility.class);
    when(remoteCallerUtility.fetchClientUUIDs()).thenReturn(new HashSet<String>());
    RemoteCaller remoteCaller = new RemoteCaller(remoteAgentBridgeService, contextService, executorService, requestTicketMonitor, userService, timeoutService,remoteCallerUtility);

    Set<String> remoteAgentNodeNames = new HashSet<String>(){{add("test-nodename-1");add("test-nodename-2");add("test-nodename-3");add("test-nodename-4");}};
    when(remoteAgentBridgeService.getRemoteAgentNodeNames()).thenReturn(remoteAgentNodeNames);

    when(remoteAgentBridgeService.getRemoteAgentNodeDetails("test-nodename-1")).thenReturn(new HashMap<String, String>() {{
      put("Version", "123");
      put("Agency", "other");
    }});
    when(remoteAgentBridgeService.getRemoteAgentNodeDetails("test-nodename-2")).thenReturn(new HashMap<String, String>() {{
      put("Version", "123");
      put("Agency", "testAgency");
    }});
    when(remoteAgentBridgeService.getRemoteAgentNodeDetails("test-nodename-3")).thenReturn(new HashMap<String, String>() {{
      put("Version", "123");
      put("Agency", "testAgency");
    }});
    when(remoteAgentBridgeService.getRemoteAgentNodeDetails("test-nodename-4")).thenReturn(new HashMap<String, String>() {{
      put("Version", "123");
      put("Agency", "testAgency");
    }});

    when(requestTicketMonitor.issueRequestTicket()).thenReturn("test-ticket");
    DfltUserInfo userInfo = new DfltUserInfo("testUser", "testPwHash", Collections.singleton(UserRole.TERRACOTTA));
    when(contextService.getUserInfo()).thenReturn(userInfo);
    when(userService.putUserInfo(userInfo)).thenReturn("test-token");
    Set<String> nodeNames = new HashSet<String>() {{
      add("test-nodename-1");
      add("test-nodename-2");
      add("test-nodename-3");
      add("test-nodename-4");
    }};
    when(remoteAgentBridgeService.invokeRemoteMethod(anyString(), Matchers.any(RemoteCallDescriptor.class))).thenReturn(SERIALIZED_AGENT_ENTITY_COLLECTION);
    when(timeoutService.getCallTimeout()).thenReturn(1000L);


    Object response = remoteCaller.fanOutCollectionCall("testAgency", nodeNames, "myService", RemoteAgentEndpoint.class.getMethod("getVersion"), new Object[0]);
    Collection<AgentEntity> responseCollection = (Collection<AgentEntity>)response;

    Set<String> remoteAgentIds = new HashSet<String>();
    for (AgentEntity agentEntity : responseCollection) {
      String agentId = agentEntity.getAgentId();
      remoteAgentIds.add(agentId);
    }

    assertThat(remoteAgentIds, containsInAnyOrder("test-nodename-2", "test-nodename-3", "test-nodename-4"));
  }

  @Test
  public void when_fanOutCollectionCall_filtering_agencies_with_4_nodes_timing_out_1_then_remote_collection_has_3_corresponding_agentIds() throws Exception {
    TimeoutService timeoutService = mock(TimeoutService.class);
    RemoteAgentBridgeService remoteAgentBridgeService = mock(RemoteAgentBridgeService.class);
    ContextService contextService = mock(ContextService.class);
    ExecutorService executorService = Executors.newCachedThreadPool();
    RequestTicketMonitor requestTicketMonitor = mock(RequestTicketMonitor.class);
    UserService userService = mock(UserService.class);
    RemoteCallerUtility remoteCallerUtility = mock(RemoteCallerUtility.class);
    when(remoteCallerUtility.fetchClientUUIDs()).thenReturn(new HashSet<String>());
    RemoteCaller remoteCaller = new RemoteCaller(remoteAgentBridgeService, contextService, executorService, requestTicketMonitor, userService, timeoutService,remoteCallerUtility);

    Set<String> remoteAgentNodeNames = new HashSet<String>(){{add("test-nodename-1");add("test-nodename-2");add("test-nodename-3");add("test-nodename-4");}};
    when(remoteAgentBridgeService.getRemoteAgentNodeNames()).thenReturn(remoteAgentNodeNames);

    when(remoteAgentBridgeService.getRemoteAgentNodeDetails("test-nodename-1")).thenReturn(new HashMap<String, String>() {{
      put("Version", "123");
      put("Agency", "testAgency");
    }});
    when(remoteAgentBridgeService.getRemoteAgentNodeDetails("test-nodename-2")).thenReturn(new HashMap<String, String>() {{
      put("Version", "123");
      put("Agency", "testAgency");
    }});
    when(remoteAgentBridgeService.getRemoteAgentNodeDetails("test-nodename-3")).then(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        Thread.sleep(200L);
        return new HashMap<String, String>() {{
          put("Version", "123");
          put("Agency", "testAgency");
        }};
      }
    });
    when(remoteAgentBridgeService.getRemoteAgentNodeDetails("test-nodename-4")).thenReturn(new HashMap<String, String>() {{
      put("Version", "123");
      put("Agency", "testAgency");
    }});

    when(requestTicketMonitor.issueRequestTicket()).thenReturn("test-ticket");
    DfltUserInfo userInfo = new DfltUserInfo("testUser", "testPwHash", Collections.singleton(UserRole.TERRACOTTA));
    when(contextService.getUserInfo()).thenReturn(userInfo);
    when(userService.putUserInfo(userInfo)).thenReturn("test-token");
    Set<String> nodeNames = new HashSet<String>() {{
      add("test-nodename-1");
      add("test-nodename-2");
      add("test-nodename-3");
      add("test-nodename-4");
    }};
    when(remoteAgentBridgeService.invokeRemoteMethod(anyString(), Matchers.any(RemoteCallDescriptor.class))).thenReturn(SERIALIZED_AGENT_ENTITY_COLLECTION);
    when(timeoutService.getCallTimeout()).thenReturn(100L);


    Object response = remoteCaller.fanOutCollectionCall("testAgency", nodeNames, "myService", RemoteAgentEndpoint.class.getMethod("getVersion"), new Object[0]);
    Collection<AgentEntity> responseCollection = (Collection<AgentEntity>)response;

    Set<String> remoteAgentIds = new HashSet<String>();
    for (AgentEntity agentEntity : responseCollection) {
      String agentId = agentEntity.getAgentId();
      remoteAgentIds.add(agentId);
    }

    assertThat(remoteAgentIds, containsInAnyOrder("test-nodename-1", "test-nodename-2", "test-nodename-4"));
  }

}
