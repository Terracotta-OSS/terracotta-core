package com.terracotta.management.l1bridge;

import com.terracotta.management.security.ContextService;
import com.terracotta.management.security.RequestTicketMonitor;
import com.terracotta.management.security.UserService;
import com.terracotta.management.security.impl.NullContextService;
import com.terracotta.management.security.impl.NullRequestTicketMonitor;
import com.terracotta.management.security.impl.NullUserService;
import com.terracotta.management.service.RemoteAgentBridgeService;
import com.terracotta.management.service.TimeoutService;
import com.terracotta.management.service.impl.TimeoutServiceImpl;
import com.terracotta.management.user.UserRole;
import com.terracotta.management.user.impl.DfltUserInfo;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.terracotta.management.l1bridge.RemoteAgentEndpoint;
import org.terracotta.management.l1bridge.RemoteCallDescriptor;
import org.terracotta.management.resource.AbstractEntityV2;
import org.terracotta.management.resource.AgentEntityV2;
import org.terracotta.management.resource.ExceptionEntityV2;
import org.terracotta.management.resource.ResponseEntityV2;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Ludovic Orban
 */
public class RemoteCallerV2Test {

  private final static byte[] SERIALIZED_AGENT_ENTITY;
  private final static byte[] SERIALIZED_AGENT_ENTITY_RESPONSE;

  static {
    try {
      SERIALIZED_AGENT_ENTITY = serialize(new AgentEntityV2());
      ResponseEntityV2<AgentEntityV2> response = new ResponseEntityV2<AgentEntityV2>();
      response.getEntities().add(new AgentEntityV2());
      SERIALIZED_AGENT_ENTITY_RESPONSE = serialize(response);
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
  public void testFanOutResponseCallBehaviorWhenThreadPoolIsSaturated() throws Exception {
    final int maxThreads = 1;
    final int queueCapacity = 5;
    final long rejectionTimeoutInMs = 25L;
    final long invokeRemoteMethodDelayInMs = 1000L;
    final int nodeCount = 100;
    final long fanoutCallTimeout = 2500L;

    long before = System.nanoTime();
    final ThreadPoolExecutor l1BridgeExecutorService = new ThreadPoolExecutor(maxThreads, maxThreads, 60L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<Runnable>(queueCapacity, true));
    l1BridgeExecutorService.allowCoreThreadTimeOut(true);
    l1BridgeExecutorService.setRejectedExecutionHandler(new RejectedExecutionHandler() {
      public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        try {
          boolean accepted = l1BridgeExecutorService.getQueue().offer(r, rejectionTimeoutInMs, TimeUnit.MILLISECONDS);
          if (!accepted) {
            throw new RejectedExecutionException("L1 Management thread pool saturated, job rejected");
          }
        } catch (InterruptedException ie) {
          throw new RejectedExecutionException("L1 Management thread pool interrupted, job rejected", ie);
        }
      }
    });

    RemoteAgentBridgeService remoteAgentBridgeService = mock(RemoteAgentBridgeService.class);
    when(remoteAgentBridgeService.invokeRemoteMethod(anyString(), any(RemoteCallDescriptor.class))).thenAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        Thread.sleep(invokeRemoteMethodDelayInMs);
        ResponseEntityV2<AbstractEntityV2> response = new ResponseEntityV2<AbstractEntityV2>();
        response.getEntities().add(new AgentEntityV2());
        return serialize(response);
      }
    });

    RemoteCallerV2 remoteCaller = new RemoteCallerV2(remoteAgentBridgeService, new NullContextService(), l1BridgeExecutorService,
        new NullRequestTicketMonitor(), new NullUserService(), new TimeoutServiceImpl(fanoutCallTimeout));

    Set<String> nodeNames = new HashSet<String>();
    for (int i=0;i<nodeCount;i++) {
      nodeNames.add("node" + i);
    }

    ResponseEntityV2<AbstractEntityV2> response = remoteCaller.fanOutResponseCall(null, nodeNames, "myService", RemoteAgentEndpoint.class.getMethod("getVersion"), new Object[0]);
    long totalExecutionTimeInMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - before);
    Collection<AbstractEntityV2> entities = response.getEntities();
    Collection<ExceptionEntityV2> exceptionEntities = response.getExceptionEntities();

//    System.out.println(entities.size() + " jobs executed");
//    System.out.println(exceptionEntities.size() + " jobs rejected");
//    System.out.println(entities.size() + exceptionEntities.size() + " total jobs");
//    System.out.println("total execution time: " + totalExecutionTimeInMs + "ms");
//    System.out.println("threads still active in the pool : " + l1BridgeExecutorService.getActiveCount());

    assertThat(entities.size(), greaterThan(0));
    assertThat(exceptionEntities.size(), greaterThan(0));
    assertThat(entities.size() + exceptionEntities.size(), is(nodeCount));
//    assertThat(totalExecutionTimeInMs, lessThanOrEqualTo(fanoutCallTimeout * 125 / 100));
    assertThat(l1BridgeExecutorService.getActiveCount(), is(0));
  }

  @Test
  public void when_getRemoteAgentNodeNames_then_call_is_delegated_to_RemoteAgentBridgeService() throws Exception {
    RemoteAgentBridgeService remoteAgentBridgeService = mock(RemoteAgentBridgeService.class);
    RemoteCallerV2 remoteCaller = new RemoteCallerV2(remoteAgentBridgeService, null, null, null, null, null);

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
    RemoteCallerV2 remoteCaller = new RemoteCallerV2(remoteAgentBridgeService, null, Executors.newCachedThreadPool(),
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
    RemoteCallerV2 remoteCaller = new RemoteCallerV2(remoteAgentBridgeService, contextService, executorService, requestTicketMonitor, userService, timeoutService);

    when(requestTicketMonitor.issueRequestTicket()).thenReturn("test-ticket");
    DfltUserInfo userInfo = new DfltUserInfo("testUser", "testPwHash", Collections.singleton(UserRole.TERRACOTTA));
    when(contextService.getUserInfo()).thenReturn(userInfo);
    when(userService.putUserInfo(userInfo)).thenReturn("test-token");
    String nodeName = "test-nodename";
    when(remoteAgentBridgeService.invokeRemoteMethod(eq(nodeName), any(RemoteCallDescriptor.class))).thenReturn(SERIALIZED_AGENT_ENTITY);

    Object response = remoteCaller.call(nodeName, "myService", RemoteAgentEndpoint.class.getMethod("getVersion"), new Object[0]);
    assertThat(((AgentEntityV2) response).getAgentId(), equalTo(nodeName));
  }

  @Test
  public void when_call_then_remote_collection_has_corresponding_agentId() throws Exception {
    TimeoutService timeoutService = mock(TimeoutService.class);
    RemoteAgentBridgeService remoteAgentBridgeService = mock(RemoteAgentBridgeService.class);
    ContextService contextService = mock(ContextService.class);
    ExecutorService executorService = Executors.newCachedThreadPool();
    RequestTicketMonitor requestTicketMonitor = mock(RequestTicketMonitor.class);
    UserService userService = mock(UserService.class);
    RemoteCallerV2 remoteCaller = new RemoteCallerV2(remoteAgentBridgeService, contextService, executorService, requestTicketMonitor, userService, timeoutService);

    when(requestTicketMonitor.issueRequestTicket()).thenReturn("test-ticket");
    DfltUserInfo userInfo = new DfltUserInfo("testUser", "testPwHash", Collections.singleton(UserRole.TERRACOTTA));
    when(contextService.getUserInfo()).thenReturn(userInfo);
    when(userService.putUserInfo(userInfo)).thenReturn("test-token");
    String nodeName = "test-nodename";
    when(remoteAgentBridgeService.invokeRemoteMethod(eq(nodeName), any(RemoteCallDescriptor.class))).thenReturn(SERIALIZED_AGENT_ENTITY_RESPONSE);

    ResponseEntityV2<AgentEntityV2> response = (ResponseEntityV2<AgentEntityV2>)remoteCaller.call(nodeName, "myService", RemoteAgentEndpoint.class.getMethod("getVersion"), new Object[0]);
    for (AgentEntityV2 agentEntityV2 : response.getEntities()) {
      assertThat(agentEntityV2.getAgentId(), equalTo(nodeName));
    }
  }

  @Test
  public void when_fanOutResponseCall_any_agency_with_3_nodes_then_remote_collection_has_3_corresponding_agentIds() throws Exception {
    TimeoutService timeoutService = mock(TimeoutService.class);
    RemoteAgentBridgeService remoteAgentBridgeService = mock(RemoteAgentBridgeService.class);
    ContextService contextService = mock(ContextService.class);
    ExecutorService executorService = Executors.newCachedThreadPool();
    RequestTicketMonitor requestTicketMonitor = mock(RequestTicketMonitor.class);
    UserService userService = mock(UserService.class);
    RemoteCallerV2 remoteCaller = new RemoteCallerV2(remoteAgentBridgeService, contextService, executorService, requestTicketMonitor, userService, timeoutService);

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
    when(remoteAgentBridgeService.invokeRemoteMethod(anyString(), any(RemoteCallDescriptor.class))).thenReturn(SERIALIZED_AGENT_ENTITY_RESPONSE);
    when(timeoutService.getCallTimeout()).thenReturn(1000L);


    ResponseEntityV2<AgentEntityV2> response = remoteCaller.fanOutResponseCall(null, nodeNames, "myService", RemoteAgentEndpoint.class.getMethod("getVersion"), new Object[0]);

    Set<String> remoteAgentIds = new HashSet<String>();
    for (AgentEntityV2 agentEntityV2 : response.getEntities()) {
      String agentId = agentEntityV2.getAgentId();
      remoteAgentIds.add(agentId);
    }

    assertThat(remoteAgentIds, containsInAnyOrder("test-nodename-1", "test-nodename-2", "test-nodename-3"));
  }

  @Test
  public void when_fanOutResponseCall_filtering_agencies_with_3_nodes_then_remote_collection_has_3_corresponding_agentIds() throws Exception {
    TimeoutService timeoutService = mock(TimeoutService.class);
    RemoteAgentBridgeService remoteAgentBridgeService = mock(RemoteAgentBridgeService.class);
    ContextService contextService = mock(ContextService.class);
    ExecutorService executorService = Executors.newCachedThreadPool();
    RequestTicketMonitor requestTicketMonitor = mock(RequestTicketMonitor.class);
    UserService userService = mock(UserService.class);
    RemoteCallerV2 remoteCaller = new RemoteCallerV2(remoteAgentBridgeService, contextService, executorService, requestTicketMonitor, userService, timeoutService);

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
    when(remoteAgentBridgeService.invokeRemoteMethod(anyString(), any(RemoteCallDescriptor.class))).thenReturn(SERIALIZED_AGENT_ENTITY_RESPONSE);

    ResponseEntityV2<AgentEntityV2> response = remoteCaller.fanOutResponseCall("test", nodeNames, "myService", RemoteAgentEndpoint.class.getMethod("getVersion"), new Object[0]);

    Set<String> remoteAgentIds = new HashSet<String>();
    for (AgentEntityV2 agentEntityV2 : response.getEntities()) {
      String agentId = agentEntityV2.getAgentId();
      remoteAgentIds.add(agentId);
    }

    assertThat(remoteAgentIds, containsInAnyOrder("test-nodename-1", "test-nodename-2", "test-nodename-3"));
  }

  @Test
  public void when_fanOutResponseCall_filtering_agencies_with_4_nodes_filtering_out_1_then_remote_collection_has_3_corresponding_agentIds() throws Exception {
    TimeoutService timeoutService = mock(TimeoutService.class);
    RemoteAgentBridgeService remoteAgentBridgeService = mock(RemoteAgentBridgeService.class);
    ContextService contextService = mock(ContextService.class);
    ExecutorService executorService = Executors.newCachedThreadPool();
    RequestTicketMonitor requestTicketMonitor = mock(RequestTicketMonitor.class);
    UserService userService = mock(UserService.class);
    RemoteCallerV2 remoteCaller = new RemoteCallerV2(remoteAgentBridgeService, contextService, executorService, requestTicketMonitor, userService, timeoutService);

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
    when(remoteAgentBridgeService.invokeRemoteMethod(anyString(), any(RemoteCallDescriptor.class))).thenReturn(SERIALIZED_AGENT_ENTITY_RESPONSE);
    when(timeoutService.getCallTimeout()).thenReturn(1000L);


    ResponseEntityV2<AgentEntityV2> response = remoteCaller.fanOutResponseCall("testAgency", nodeNames, "myService", RemoteAgentEndpoint.class.getMethod("getVersion"), new Object[0]);

    Set<String> remoteAgentIds = new HashSet<String>();
    for (AgentEntityV2 agentEntityV2 : response.getEntities()) {
      String agentId = agentEntityV2.getAgentId();
      remoteAgentIds.add(agentId);
    }

    assertThat(remoteAgentIds, containsInAnyOrder("test-nodename-2", "test-nodename-3", "test-nodename-4"));
  }

  @Test
  public void when_fanOutResponseCall_filtering_agencies_with_4_nodes_timing_out_1_then_remote_collection_has_3_corresponding_agentIds() throws Exception {
    TimeoutService timeoutService = mock(TimeoutService.class);
    RemoteAgentBridgeService remoteAgentBridgeService = mock(RemoteAgentBridgeService.class);
    ContextService contextService = mock(ContextService.class);
    ExecutorService executorService = Executors.newCachedThreadPool();
    RequestTicketMonitor requestTicketMonitor = mock(RequestTicketMonitor.class);
    UserService userService = mock(UserService.class);
    RemoteCallerV2 remoteCaller = new RemoteCallerV2(remoteAgentBridgeService, contextService, executorService, requestTicketMonitor, userService, timeoutService);

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
    when(remoteAgentBridgeService.invokeRemoteMethod(anyString(), any(RemoteCallDescriptor.class))).thenReturn(SERIALIZED_AGENT_ENTITY_RESPONSE);
    when(timeoutService.getCallTimeout()).thenReturn(100L);


    ResponseEntityV2<AgentEntityV2> response = remoteCaller.fanOutResponseCall("testAgency", nodeNames, "myService", RemoteAgentEndpoint.class.getMethod("getVersion"), new Object[0]);

    Set<String> remoteAgentIds = new HashSet<String>();
    for (AgentEntityV2 agentEntityV2 : response.getEntities()) {
      String agentId = agentEntityV2.getAgentId();
      remoteAgentIds.add(agentId);
    }

    assertThat(remoteAgentIds, containsInAnyOrder("test-nodename-1", "test-nodename-2", "test-nodename-4"));
  }

}
