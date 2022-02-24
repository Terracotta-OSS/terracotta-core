/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
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

import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.net.DSOChannelManagerMBean;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.core.impl.ServerManagementContext;
import com.tc.objectserver.impl.DistributedObjectServer;
import com.tc.util.Assert;
import org.terracotta.server.Server;
import org.terracotta.server.ServerEnv;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import javax.management.MBeanServer;
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
  private ServerMode currentState = ServerMode.START;

  @Before
  public void setUp() throws Exception {
    dso = mock(DistributedObjectServer.class);
    tcServer = new TCServerImpl(dso, mock(ServerConfigurationManager.class));
    when(dso.stop()).thenReturn(CompletableFuture.completedFuture(null));
    when(dso.stop(anyBoolean())).thenReturn(CompletableFuture.completedFuture(null));
    ServerManagementContext smc = mock(ServerManagementContext.class);
    DSOChannelManagerMBean cm = mock(DSOChannelManagerMBean.class);
    when(cm.getActiveChannels()).thenReturn(new MessageChannel[0]);
    when(smc.getChannelManager()).thenReturn(cm);    
    when(smc.getOperationGuardian()).thenReturn((o,p)->true);
    when(dso.getManagementContext()).thenReturn(smc);
    ServerConfigurationContext cxt = mock(ServerConfigurationContext.class);
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
    try {
      tcServer.registerDSOServer(mock(MBeanServer.class));
    } catch (NullPointerException n) {
      n.printStackTrace();
      throw n;
    }
    ServerEnv.setServer(mock(Server.class));
  }
  
  private void setState(ServerMode state) {
    currentState = state;
  }

  @Test
  public void testForceStop() throws Exception {
    tcServer.stop();
    verify(dso).stop(ArgumentMatchers.anyBoolean());
  }

  @Test
  public void testForceRestart() throws Exception {
    tcServer.stop(RESTART);
    verify(dso).stop(ArgumentMatchers.anyBoolean());
    Assert.assertTrue(tcServer.waitUntilShutdown());
  }

  @Test
  public void testStopIfPassive() throws Exception {
    setState(ServerMode.PASSIVE);
    tcServer.stopIfPassive();
    verify(dso).stop(ArgumentMatchers.anyBoolean());
  }

  @Test
  public void testStopIfPassiveWhenStateStateIsUninitialized() throws Exception {
    setState(ServerMode.UNINITIALIZED);
    tcServer.stopIfPassive();
    verify(dso).stop(ArgumentMatchers.anyBoolean());
  }

  @Test
  public void testStopIfPassiveWhenStateStateIsSyncing() throws Exception {
    setState(ServerMode.SYNCING);
    tcServer.stopIfPassive();
    verify(dso).stop(ArgumentMatchers.anyBoolean());
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
    verify(dso).stop(ArgumentMatchers.anyBoolean());
    Assert.assertTrue(tcServer.waitUntilShutdown());
  }

  @Test
  public void testStopIfActive() throws Exception {
    setState(ServerMode.ACTIVE);
    tcServer.stopIfActive();
    verify(dso).stop(ArgumentMatchers.anyBoolean());
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
    verify(dso).stop(ArgumentMatchers.anyBoolean());
    Assert.assertTrue(tcServer.waitUntilShutdown());
  }
}
