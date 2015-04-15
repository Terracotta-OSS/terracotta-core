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

import com.terracotta.management.resource.ClientEntityV2;
import com.terracotta.management.resource.TopologyEntityV2;
import com.terracotta.management.security.impl.DfltSecurityContextService;
import com.terracotta.management.service.L1MBeansSource;
import com.terracotta.management.service.impl.util.LocalManagementSource;
import com.terracotta.management.service.impl.util.RemoteManagementSource;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.management.ObjectName;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * @author Ludovic Orban
 */
public class ClientManagementServiceV2Test {

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
  public void test_getClients_local() throws Exception {
    LocalManagementSource localManagementSource = mock(LocalManagementSource.class);
    TimeoutServiceImpl timeoutService = new TimeoutServiceImpl(1000L);
    DfltSecurityContextService securityContextService = new DfltSecurityContextService();
    RemoteManagementSource remoteManagementSource = spy(new RemoteManagementSource(localManagementSource, timeoutService));
    L1MBeansSource l1MBeansSource = mock(L1MBeansSource.class);

    when(localManagementSource.getRemoteServerUrls()).thenReturn(new HashMap<String, String>() {{
      put("s1", "http://host-1.com:9540");
      put("s2", "http://host-2.com:9540");
      put("s3", "http://host-3.com:9540");
    }});
    when(l1MBeansSource.containsJmxMBeans()).thenReturn(true);
    when(localManagementSource.fetchClientObjectNames(any(Set.class))).thenReturn(new ArrayList<ObjectName>(Arrays.asList(new ObjectName("x:id=objName1"), new ObjectName("x:id=objName2"), new ObjectName("x:id=objName3"))));
    when(localManagementSource.getClientID(eq(new ObjectName("x:id=objName1")))).thenReturn("1");
    when(localManagementSource.getClientID(eq(new ObjectName("x:id=objName2")))).thenReturn("2");
    when(localManagementSource.getClientID(eq(new ObjectName("x:id=objName3")))).thenReturn("3");
    when(localManagementSource.getClientAttributes(eq(new ObjectName("x:id=objName1")))).thenReturn(
        new HashMap<String, Object>() {{
          put("Name", "Client1");
          put("MavenArtifactsVersion", "1.2.3_1");
        }}
    );
    when(localManagementSource.getClientAttributes(eq(new ObjectName("x:id=objName2")))).thenReturn(
        new HashMap<String, Object>() {{
          put("Name", "Client2");
          put("MavenArtifactsVersion", "1.2.3_2");
        }}
    );
    when(localManagementSource.getClientAttributes(eq(new ObjectName("x:id=objName3")))).thenReturn(
        new HashMap<String, Object>() {{
          put("Name", "Client3");
          put("MavenArtifactsVersion", "1.2.3_3");
        }}
    );

    ClientManagementServiceV2 clientManagementService = new ClientManagementServiceV2(l1MBeansSource, executorService, timeoutService, localManagementSource, remoteManagementSource, securityContextService);

    ResponseEntityV2<ClientEntityV2> response = clientManagementService.getClients(new HashSet<String>(Arrays.asList("2")), null);
    assertThat(response.getEntities().size(), is(1));

    assertThat(response.getEntities().iterator().next().getProductVersion(), equalTo("1.2.3_2"));
    assertThat(response.getEntities().iterator().next().getAttributes().get("Name"), CoreMatchers.<Object>equalTo("Client2"));
  }

  @Test
  public void test_getClients_remote() throws Exception {
    LocalManagementSource localManagementSource = mock(LocalManagementSource.class);
    TimeoutServiceImpl timeoutService = new TimeoutServiceImpl(1000L);
    DfltSecurityContextService securityContextService = new DfltSecurityContextService();
    RemoteManagementSource remoteManagementSource = mock(RemoteManagementSource.class);
    L1MBeansSource l1MBeansSource = mock(L1MBeansSource.class);

    when(localManagementSource.getRemoteServerUrls()).thenReturn(new HashMap<String, String>() {{
      put("s1", "http://host-1.com:9540");
      put("s2", "http://host-2.com:9540");
      put("s3", "http://host-3.com:9540");
    }});
    when(l1MBeansSource.containsJmxMBeans()).thenReturn(false);
    when(l1MBeansSource.getActiveL2ContainingMBeansName()).thenReturn("s1");
    ResponseEntityV2<TopologyEntityV2> value = new ResponseEntityV2<TopologyEntityV2>();
    TopologyEntityV2 topologyEntity = new TopologyEntityV2();
    ClientEntityV2 clientEntity = new ClientEntityV2();
    clientEntity.setProductVersion("1.2.3");
    clientEntity.getAttributes().put("Name", "ClientName");
    topologyEntity.getClientEntities().add(clientEntity);
    value.getEntities().add(topologyEntity);
    when(remoteManagementSource.getFromRemoteL2(eq("s1"), eq(new URI("tc-management-api/v2/agents/topologies/clients;ids=1")), eq(ResponseEntityV2.class), eq(TopologyEntityV2.class))).thenReturn(value);


    ClientManagementServiceV2 clientManagementService = new ClientManagementServiceV2(l1MBeansSource, executorService, timeoutService, localManagementSource, remoteManagementSource, securityContextService);

    ResponseEntityV2<ClientEntityV2> response = clientManagementService.getClients(new HashSet<String>(Arrays.asList("1")), null);
    assertThat(response.getEntities().size(), is(1));
    assertThat(response.getEntities().iterator().next(), equalTo(clientEntity));
  }

}
