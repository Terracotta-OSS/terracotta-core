/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.terracotta.management.service.impl;

import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.terracotta.management.resource.ResponseEntityV2;

import com.tc.config.schema.L2Info;
import com.tc.config.schema.ServerGroupInfo;
import com.terracotta.management.resource.BackupEntityV2;
import com.terracotta.management.resource.ServerEntityV2;
import com.terracotta.management.resource.ServerGroupEntityV2;
import com.terracotta.management.resource.StatisticsEntityV2;
import com.terracotta.management.resource.ThreadDumpEntityV2;
import com.terracotta.management.resource.TopologyEntityV2;
import com.terracotta.management.security.impl.DfltSecurityContextService;
import com.terracotta.management.service.impl.util.LocalManagementSource;
import com.terracotta.management.service.impl.util.RemoteManagementSource;
import com.terracotta.management.web.proxy.ProxyException;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.ws.rs.WebApplicationException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Ludovic Orban
 */
public class ServerManagementServiceV2Test {

  private static final L2Info[] L2_INFOS = new L2Info[] {
    new L2Info("s1", "s1_host", 9520, 9510, "s1_bind", 9530, 9540, "s1_secure"),
    new L2Info("s2", "s2_host", 9521, 9511, "s2_bind", 9531, 9541, "s2_secure"),
    new L2Info("s3", "s3_host", 9522, 9512, "s3_bind", 9532, 9542, "s3_secure")
  };


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
  public void test_getBackupsStatus() throws Exception {
    LocalManagementSource localManagementSource = mock(LocalManagementSource.class);
    TimeoutServiceImpl timeoutService = new TimeoutServiceImpl(1000L);
    DfltSecurityContextService securityContextService = new DfltSecurityContextService();
    RemoteManagementSource remoteManagementSource = spy(new RemoteManagementSource(localManagementSource, timeoutService));

    when(localManagementSource.getL2Infos()).thenReturn(L2_INFOS);
    when(localManagementSource.getLocalServerName()).thenReturn("s1");
    when(localManagementSource.isActiveCoordinator()).thenReturn(true);
    when(localManagementSource.getBackupStatuses()).thenReturn(new HashMap<String, String>() {{
      put("backup1", "OK");
    }});

    ServerManagementServiceV2 serverManagementService = new ServerManagementServiceV2(executorService, timeoutService, localManagementSource, remoteManagementSource, securityContextService);

    ResponseEntityV2<BackupEntityV2> backupsStatus = serverManagementService.getBackupsStatus(new HashSet<String>(Arrays.asList("s1", "s2", "s3")));
    assertThat(backupsStatus.getExceptionEntities().size(), is(0));
    assertThat(backupsStatus.getEntities().size(), is(1));
    BackupEntityV2 entity = backupsStatus.getEntities().iterator().next();
    assertThat(entity.getName(), equalTo("backup1"));
    assertThat(entity.getStatus(), equalTo("OK"));
    assertThat(entity.getSourceId(), equalTo("s1"));
    assertThat(entity.getError(), is((Object)null));

    verify(remoteManagementSource).getFromRemoteL2(eq("s2"), eq(new URI("tc-management-api/v2/agents/backups;serverNames=s2")), eq(ResponseEntityV2.class), eq(BackupEntityV2.class));
    verify(remoteManagementSource).getFromRemoteL2(eq("s3"), eq(new URI("tc-management-api/v2/agents/backups;serverNames=s3")), eq(ResponseEntityV2.class), eq(BackupEntityV2.class));
  }

  @Test
  public void test_serversThreadDump_all() throws Exception {
    LocalManagementSource localManagementSource = mock(LocalManagementSource.class);
    TimeoutServiceImpl timeoutService = new TimeoutServiceImpl(1000L);
    DfltSecurityContextService securityContextService = new DfltSecurityContextService();
    RemoteManagementSource remoteManagementSource = spy(new RemoteManagementSource(localManagementSource, timeoutService));

    when(localManagementSource.getL2Infos()).thenReturn(L2_INFOS);
    when(localManagementSource.getLocalServerName()).thenReturn("s1");
    when(localManagementSource.isActiveCoordinator()).thenReturn(true);
    when(localManagementSource.serverThreadDump()).thenReturn("thread dump");

    ServerManagementServiceV2 serverManagementService = new ServerManagementServiceV2(executorService, timeoutService, localManagementSource, remoteManagementSource, securityContextService);

    ResponseEntityV2<ThreadDumpEntityV2> response = serverManagementService.serversThreadDump(new HashSet<String>(Arrays.asList("s1", "s2", "s3")));
    assertThat(response.getExceptionEntities().size(), is(0));
    assertThat(response.getEntities().size(), is(1));
    ThreadDumpEntityV2 entity = response.getEntities().iterator().next();
    assertThat(entity.getSourceId(), equalTo("s1"));
    assertThat(entity.getDump(), equalTo("thread dump"));
    assertThat(entity.getNodeType(), equalTo(ThreadDumpEntityV2.NodeType.SERVER));

    verify(remoteManagementSource).getFromRemoteL2(eq("s2"), eq(new URI("tc-management-api/v2/agents/diagnostics/threadDump/servers;names=s2")), eq(ResponseEntityV2.class), eq(ThreadDumpEntityV2.class));
    verify(remoteManagementSource).getFromRemoteL2(eq("s3"), eq(new URI("tc-management-api/v2/agents/diagnostics/threadDump/servers;names=s3")), eq(ResponseEntityV2.class), eq(ThreadDumpEntityV2.class));
  }

  @Test
  public void test_serversThreadDump_filter() throws Exception {
    LocalManagementSource localManagementSource = mock(LocalManagementSource.class);
    TimeoutServiceImpl timeoutService = new TimeoutServiceImpl(1000L);
    DfltSecurityContextService securityContextService = new DfltSecurityContextService();
    RemoteManagementSource remoteManagementSource = spy(new RemoteManagementSource(localManagementSource, timeoutService));

    when(localManagementSource.getL2Infos()).thenReturn(L2_INFOS);
    when(localManagementSource.getLocalServerName()).thenReturn("s1");
    when(localManagementSource.isActiveCoordinator()).thenReturn(true);
    when(localManagementSource.serverThreadDump()).thenReturn("thread dump");

    ServerManagementServiceV2 serverManagementService = new ServerManagementServiceV2(executorService, timeoutService, localManagementSource, remoteManagementSource, securityContextService);

    ResponseEntityV2<ThreadDumpEntityV2> response = serverManagementService.serversThreadDump(new HashSet<String>(Arrays.asList("s1", "s3")));
    assertThat(response.getExceptionEntities().size(), is(0));
    assertThat(response.getEntities().size(), is(1));
    ThreadDumpEntityV2 entity = response.getEntities().iterator().next();
    assertThat(entity.getSourceId(), equalTo("s1"));
    assertThat(entity.getDump(), equalTo("thread dump"));
    assertThat(entity.getNodeType(), equalTo(ThreadDumpEntityV2.NodeType.SERVER));

    verify(remoteManagementSource).getFromRemoteL2(eq("s3"), eq(new URI("tc-management-api/v2/agents/diagnostics/threadDump/servers;names=s3")), eq(ResponseEntityV2.class), eq(ThreadDumpEntityV2.class));
  }

  @Test
  public void test_getServersStatistics_all() throws Exception {
    LocalManagementSource localManagementSource = mock(LocalManagementSource.class);
    TimeoutServiceImpl timeoutService = new TimeoutServiceImpl(1000L);
    DfltSecurityContextService securityContextService = new DfltSecurityContextService();
    RemoteManagementSource remoteManagementSource = spy(new RemoteManagementSource(localManagementSource, timeoutService));

    when(localManagementSource.getL2Infos()).thenReturn(L2_INFOS);
    when(localManagementSource.getLocalServerName()).thenReturn("s1");
    when(localManagementSource.isActiveCoordinator()).thenReturn(true);
    when(localManagementSource.getDsoAttributes(any(String[].class))).thenReturn(new HashMap<String, Object>() {{
      put("stat1", "val1");
      put("stat2", "val2");
      put("stat3", "val3");
    }});

    ServerManagementServiceV2 serverManagementService = new ServerManagementServiceV2(executorService, timeoutService, localManagementSource, remoteManagementSource, securityContextService);

    ResponseEntityV2<StatisticsEntityV2> response = serverManagementService.getServersStatistics(
        new HashSet<String>(Arrays.asList("s1", "s2", "s3")),
        null);

    assertThat(response.getExceptionEntities().size(), is(0));
    assertThat(response.getEntities().size(), is(1));
    StatisticsEntityV2 entity = response.getEntities().iterator().next();
    assertThat(entity.getSourceId(), equalTo("s1"));
    assertThat(entity.getStatistics(), CoreMatchers.<Map<String, Object>>equalTo(new HashMap<String, Object>() {{
      put("stat1", "val1");
      put("stat2", "val2");
      put("stat3", "val3");
    }}));

    verify(remoteManagementSource).getFromRemoteL2(eq("s2"), eq(new URI("tc-management-api/v2/agents/statistics/servers;names=s2")), eq(ResponseEntityV2.class), eq(StatisticsEntityV2.class));
    verify(remoteManagementSource).getFromRemoteL2(eq("s3"), eq(new URI("tc-management-api/v2/agents/statistics/servers;names=s3")), eq(ResponseEntityV2.class), eq(StatisticsEntityV2.class));
  }

  @Test
  public void test_getServersStatistics_filter() throws Exception {
    LocalManagementSource localManagementSource = mock(LocalManagementSource.class);
    TimeoutServiceImpl timeoutService = new TimeoutServiceImpl(1000L);
    DfltSecurityContextService securityContextService = new DfltSecurityContextService();
    RemoteManagementSource remoteManagementSource = spy(new RemoteManagementSource(localManagementSource, timeoutService));

    when(localManagementSource.getL2Infos()).thenReturn(L2_INFOS);
    when(localManagementSource.getLocalServerName()).thenReturn("s1");
    when(localManagementSource.isActiveCoordinator()).thenReturn(true);
    when(localManagementSource.getDsoAttributes(argThat(arrayContainingInAnyOrder("stat1", "stat3")))).thenReturn(new HashMap<String, Object>() {{
      put("stat1", "val1");
      put("stat3", "val3");
    }});

    ServerManagementServiceV2 serverManagementService = new ServerManagementServiceV2(executorService, timeoutService, localManagementSource, remoteManagementSource, securityContextService);

    ResponseEntityV2<StatisticsEntityV2> response = serverManagementService.getServersStatistics(
        new HashSet<String>(Arrays.asList("s1", "s2", "s3")),
        new HashSet<String>(Arrays.asList("stat1", "stat3")));

    assertThat(response.getExceptionEntities().size(), is(0));
    assertThat(response.getEntities().size(), is(1));
    StatisticsEntityV2 entity = response.getEntities().iterator().next();
    assertThat(entity.getSourceId(), equalTo("s1"));
    assertThat(entity.getStatistics(), CoreMatchers.<Map<String, Object>>equalTo(new HashMap<String, Object>() {{
      put("stat1", "val1");
      put("stat3", "val3");
    }}));

    verify(remoteManagementSource).getFromRemoteL2(eq("s2"),
        argThat(IsEqualURI.equalToUri(new URI("tc-management-api/v2/agents/statistics/servers;names=s2?show=stat1,stat3"))), eq(ResponseEntityV2.class), eq(StatisticsEntityV2.class));
    verify(remoteManagementSource).getFromRemoteL2(eq("s3"),
        argThat(IsEqualURI.equalToUri(new URI("tc-management-api/v2/agents/statistics/servers;names=s3?show=stat1,stat3"))), eq(ResponseEntityV2.class), eq(StatisticsEntityV2.class));
  }

  @Test
  public void test_getServerGroups() throws Exception {
    LocalManagementSource localManagementSource = mock(LocalManagementSource.class);
    TimeoutServiceImpl timeoutService = new TimeoutServiceImpl(1000L);
    DfltSecurityContextService securityContextService = new DfltSecurityContextService();
    RemoteManagementSource remoteManagementSource = spy(new RemoteManagementSource(localManagementSource, timeoutService));

    when(localManagementSource.getL2Infos()).thenReturn(L2_INFOS);
    when(localManagementSource.getLocalServerName()).thenReturn("s1");
    when(localManagementSource.isActiveCoordinator()).thenReturn(true);
    when(localManagementSource.getServerGroupInfos()).thenReturn(new ServerGroupInfo[] {
        new ServerGroupInfo(new L2Info[] {L2_INFOS[0], L2_INFOS[1]}, "group0", 0, true),
        new ServerGroupInfo(new L2Info[] {L2_INFOS[2]}, "group1", 1, false),
    });

    ServerManagementServiceV2 serverManagementService = new ServerManagementServiceV2(executorService, timeoutService, localManagementSource, remoteManagementSource, securityContextService);

    Collection<ServerGroupEntityV2> response = serverManagementService.getServerGroups(new HashSet<String>(Arrays.asList("s1", "s2", "s3")));

    assertThat(response.size(), is(1));
    ServerGroupEntityV2 entity = response.iterator().next();
    assertThat(entity.getName(), equalTo("group0"));
    assertThat(entity.getId(), equalTo(0));
    assertThat(entity.isCoordinator(), equalTo(true));

    verify(remoteManagementSource).getFromRemoteL2(eq("s2"), eq(new URI("tc-management-api/v2/agents/topologies/servers;names=s2")), eq(ResponseEntityV2.class), eq(TopologyEntityV2.class));
    verify(remoteManagementSource).getFromRemoteL2(eq("s3"), eq(new URI("tc-management-api/v2/agents/topologies/servers;names=s3")), eq(ResponseEntityV2.class), eq(TopologyEntityV2.class));
  }

  @Test
  public void testProxyClientRequest_throwsProxyException() throws Exception {
    LocalManagementSource localManagementSource = mock(LocalManagementSource.class);
    TimeoutServiceImpl timeoutService = new TimeoutServiceImpl(1000L);
    DfltSecurityContextService securityContextService = new DfltSecurityContextService();
    RemoteManagementSource remoteManagementSource = spy(new RemoteManagementSource(localManagementSource, timeoutService));

    when(localManagementSource.getLocalServerName()).thenReturn("s2");
    when(localManagementSource.getServerGroupInfos()).thenReturn(new ServerGroupInfo[] {
        new ServerGroupInfo(new L2Info[] {L2_INFOS[0], L2_INFOS[1]}, "group0", 0, true),
        new ServerGroupInfo(new L2Info[] {L2_INFOS[2]}, "group1", 1, false),
    });

    when(localManagementSource.getServerUrls()).thenReturn(new HashMap<String, String>() {{
      put("s1", "http://s1");
      put("s3", "http://s3");
    }});

    doReturn(createTopologySingleServerResponse(true, "s1", "ACTIVE-COORDINATOR")).when(remoteManagementSource).getFromRemoteL2(eq("s1"), any(URI.class), eq(ResponseEntityV2.class), eq(TopologyEntityV2.class));
    doReturn(createTopologySingleServerResponse(true, "s3", "PASSIVE_STANDBY")).when(remoteManagementSource).getFromRemoteL2(eq("s3"), any(URI.class), eq(ResponseEntityV2.class), eq(TopologyEntityV2.class));

    ServerManagementServiceV2 serverManagementService = new ServerManagementServiceV2(executorService, timeoutService, localManagementSource, remoteManagementSource, securityContextService);

    try {
      serverManagementService.proxyClientRequest();
      fail("expected ProxyException");
    } catch (ProxyException pe) {
      assertThat(pe.getActiveL2WithMBeansUrl(), equalTo("http://s1"));
    }
  }

  private ResponseEntityV2<TopologyEntityV2> createTopologySingleServerResponse(boolean isGroupCoordinator, String serverName, String serverState) {
    ResponseEntityV2<TopologyEntityV2> response = new ResponseEntityV2<TopologyEntityV2>();
    TopologyEntityV2 topologyEntityV2 = new TopologyEntityV2();
    ServerGroupEntityV2 serverGroupEntityV2 = new ServerGroupEntityV2();
    serverGroupEntityV2.setCoordinator(isGroupCoordinator);
    ServerEntityV2 serverEntityV2 = new ServerEntityV2();
    serverEntityV2.getAttributes().put("Name", serverName);
    serverEntityV2.getAttributes().put("State", serverState);
    serverGroupEntityV2.getServers().add(serverEntityV2);
    topologyEntityV2.getServerGroupEntities().add(serverGroupEntityV2);
    response.getEntities().add(topologyEntityV2);
    return response;
  }

  @Test
  public void testProxyClientRequest_noActiveInCoordinatorGroupThrowsHttp400() throws Exception {
    LocalManagementSource localManagementSource = mock(LocalManagementSource.class);
    TimeoutServiceImpl timeoutService = new TimeoutServiceImpl(1000L);
    DfltSecurityContextService securityContextService = new DfltSecurityContextService();
    RemoteManagementSource remoteManagementSource = spy(new RemoteManagementSource(localManagementSource, timeoutService));

    when(localManagementSource.getServerGroupInfos()).thenReturn(new ServerGroupInfo[] {
        new ServerGroupInfo(new L2Info[] {L2_INFOS[0], L2_INFOS[1]}, "group0", 0, true),
        new ServerGroupInfo(new L2Info[] {L2_INFOS[2]}, "group1", 1, false),
    });

    ServerManagementServiceV2 serverManagementService = new ServerManagementServiceV2(executorService, timeoutService, localManagementSource, remoteManagementSource, securityContextService);

    try {
      serverManagementService.proxyClientRequest();
      fail("expected WebApplicationException");
    } catch (WebApplicationException wae) {
      assertThat(wae.getResponse().getStatus(), is(400));
    }
  }
}
