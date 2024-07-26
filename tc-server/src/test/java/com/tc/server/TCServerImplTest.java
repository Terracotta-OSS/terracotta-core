/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.server;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.tc.config.ServerConfigurationManager;
import com.tc.l2.api.L2Coordinator;
import com.tc.l2.state.ServerMode;
import com.tc.l2.state.StateManager;
import com.tc.lang.TCThreadGroup;

import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.transport.ConnectionPolicy;
import com.tc.object.net.DSOChannelManagerMBean;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.core.impl.ServerManagementContext;
import com.tc.objectserver.handshakemanager.ServerClientHandshakeManager;
import com.tc.objectserver.impl.DistributedObjectServer;
import com.tc.objectserver.impl.JMXSubsystem;
import com.tc.util.Assert;
import org.terracotta.server.Server;
import org.terracotta.server.ServerEnv;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.mockito.ArgumentMatchers;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.terracotta.server.StopAction.RESTART;

public class TCServerImplTest {

  @Rule
  public final ExpectedException expectedException = ExpectedException.none();

  private TCServerImpl tcServer;
  private DistributedObjectServer dso;
  private ServerConfigurationContext cxt;
  private ServerMode currentState = ServerMode.START;

  @Before
  public void setUp() throws Exception {
    dso = mock(DistributedObjectServer.class);
    tcServer = new TCServerImpl(dso, mock(ServerConfigurationManager.class), mock(TCThreadGroup.class), mock(JMXSubsystem.class), mock(ConnectionPolicy.class));
    when(dso.destroy(anyBoolean())).thenReturn(CompletableFuture.completedFuture(null));
    ServerManagementContext smc = mock(ServerManagementContext.class);
    DSOChannelManagerMBean cm = mock(DSOChannelManagerMBean.class);
    when(cm.getActiveChannels()).thenReturn(new MessageChannel[0]);
    when(smc.getChannelManager()).thenReturn(cm);    
    when(smc.getOperationGuardian()).thenReturn((o,p)->true);
    when(dso.getManagementContext()).thenReturn(smc);
    cxt = mock(ServerConfigurationContext.class);
    when(dso.getContext()).thenReturn(cxt);
    L2Coordinator l2 = mock(L2Coordinator.class);
    when(cxt.getL2Coordinator()).thenReturn(l2);
    StateManager state = mock(StateManager.class);
    when(state.getCurrentMode()).then(i->{
      return currentState;
    });
    when(state.moveToStopStateIf(any())).then(i->{
      return ((Set)i.getArguments()[0]).contains(currentState);
    });
    when(l2.getStateManager()).thenReturn(state);

    ServerEnv.setServer(mock(Server.class));
  }
  
  private void setState(ServerMode state) {
    currentState = state;
  }

  @Test
  public void testForceStop() throws Exception {
    tcServer.stop();
    verify(dso).destroy(ArgumentMatchers.anyBoolean());
  }

  @Test
  public void testForceRestart() throws Exception {
    tcServer.stop(RESTART);
    verify(dso).destroy(ArgumentMatchers.anyBoolean());
    Assert.assertTrue(tcServer.waitUntilShutdown());
  }

  @Test
  public void testStopIfPassive() throws Exception {
    setState(ServerMode.PASSIVE);
    tcServer.stopIfPassive();
    verify(dso).destroy(ArgumentMatchers.anyBoolean());
  }

  @Test
  public void testStopIfPassiveWhenStateStateIsUninitialized() throws Exception {
    setState(ServerMode.UNINITIALIZED);
    tcServer.stopIfPassive();
    verify(dso).destroy(ArgumentMatchers.anyBoolean());
  }

  @Test
  public void testStopIfPassiveWhenStateStateIsSyncing() throws Exception {
    setState(ServerMode.SYNCING);
    tcServer.stopIfPassive();
    verify(dso).destroy(ArgumentMatchers.anyBoolean());
  }

  @Test
  public void testStopIfPassiveWhenStateIsNotPassive() throws Exception {
    setState(ServerMode.ACTIVE);
    expectedException.expect(UnexpectedStateException.class);
    tcServer.stopIfPassive();
  }

  @Test
  public void testStopIfPassiveWithRestart() throws Exception {
    setState(ServerMode.PASSIVE);
    tcServer.stopIfPassive(RESTART);
    verify(dso).destroy(ArgumentMatchers.anyBoolean());
    Assert.assertTrue(tcServer.waitUntilShutdown());
  }

  @Test
  public void testStopIfActive() throws Exception {
    setState(ServerMode.ACTIVE);
    tcServer.stopIfActive();
    verify(dso).destroy(ArgumentMatchers.anyBoolean());
  }

  @Test
  public void testStopIfActiveWhenStateIsNotActive() throws Exception {
    setState(ServerMode.PASSIVE);
    expectedException.expect(UnexpectedStateException.class);
    tcServer.stopIfActive();
  }

  @Test
  public void testStopIfActiveWithRestart() throws Exception {
    setState(ServerMode.ACTIVE);
    tcServer.stopIfActive(RESTART);
    verify(dso).destroy(ArgumentMatchers.anyBoolean());
    Assert.assertTrue(tcServer.waitUntilShutdown());
  }

  @Test
  public void testAcceptingClientsNoReconnect() throws Exception {
    when(dso.isL1Listening()).thenReturn(Boolean.FALSE);
    ServerClientHandshakeManager handshake = mock(ServerClientHandshakeManager.class);
    when(handshake.isStarted()).thenReturn(Boolean.TRUE);
    when(cxt.getClientHandshakeManager()).thenReturn(handshake);
    Assert.assertFalse(tcServer.isAcceptingClients());
    verify(dso).isL1Listening();
    when(dso.isL1Listening()).thenReturn(Boolean.TRUE);
    Assert.assertTrue(tcServer.isAcceptingClients());
  }
  

  @Test
  public void testAcceptingClientsWithReconnect() throws Exception {
    ServerClientHandshakeManager handshake = mock(ServerClientHandshakeManager.class);
    when(handshake.isStarting()).thenReturn(Boolean.TRUE);
    when(handshake.isStarted()).thenReturn(Boolean.FALSE);
    when(dso.isL1Listening()).thenReturn(Boolean.TRUE);
    when(cxt.getClientHandshakeManager()).thenReturn(handshake);
    Assert.assertFalse(tcServer.isAcceptingClients());
    verify(dso).isL1Listening();
    verify(handshake).isStarted();
    when(handshake.isStarted()).thenReturn(Boolean.TRUE);
    Assert.assertTrue(tcServer.isAcceptingClients());
  }  
}
