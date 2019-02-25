package com.tc.server;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.rules.ExpectedException;
import org.terracotta.monitoring.PlatformService.RestartMode;

import com.tc.config.ServerConfigurationManager;
import com.tc.l2.state.ServerMode;

import static com.tc.lang.ServerExitStatus.EXITCODE_RESTART_REQUEST;
import static org.mockito.Mockito.mock;

public class TCServerImplTest {

  @Rule
  public final ExpectedSystemExit expectedSystemExit = ExpectedSystemExit.none();

  @Rule
  public final ExpectedException expectedException = ExpectedException.none();

  private TCServerImpl tcServer;

  @Before
  public void setUp() throws Exception {
    tcServer = new TCServerImpl(mock(ServerConfigurationManager.class));
  }

  @Test
  public void testForceStop() {
    expectedSystemExit.expectSystemExitWithStatus(0);
    tcServer.stop(RestartMode.STOP_ONLY);
  }

  @Test
  public void testForceRestart() {
    expectedSystemExit.expectSystemExitWithStatus(EXITCODE_RESTART_REQUEST);
    tcServer.stop(RestartMode.STOP_AND_RESTART);
  }

  @Test
  public void testStopIfPassive() throws Exception {
    tcServer.setState(ServerMode.PASSIVE);
    expectedSystemExit.expectSystemExitWithStatus(0);
    tcServer.stopIfPassive(RestartMode.STOP_ONLY);
  }

  @Test
  public void testStopIfPassiveWhenStateStateIsUninitialized() throws Exception {
    tcServer.setState(ServerMode.UNINITIALIZED);
    expectedSystemExit.expectSystemExitWithStatus(0);
    tcServer.stopIfPassive(RestartMode.STOP_ONLY);
  }

  @Test
  public void testStopIfPassiveWhenStateStateIsSyncing() throws Exception {
    tcServer.setState(ServerMode.SYNCING);
    expectedSystemExit.expectSystemExitWithStatus(0);
    tcServer.stopIfPassive(RestartMode.STOP_ONLY);
  }

  @Test
  public void testStopIfPassiveWhenStateIsNotPassive() throws Exception {
    tcServer.setState(ServerMode.ACTIVE);
    expectedException.expect(UnexpectedStateException.class);
    tcServer.stopIfPassive(RestartMode.STOP_ONLY);
  }

  @Test
  public void testStopIfPassiveWithRestart() throws Exception {
    tcServer.setState(ServerMode.PASSIVE);
    expectedSystemExit.expectSystemExitWithStatus(EXITCODE_RESTART_REQUEST);
    tcServer.stopIfPassive(RestartMode.STOP_AND_RESTART);
  }

  @Test
  public void testStopIfActive() throws Exception {
    tcServer.setState(ServerMode.ACTIVE);
    expectedSystemExit.expectSystemExitWithStatus(0);
    tcServer.stopIfActive(RestartMode.STOP_ONLY);
  }

  @Test
  public void testStopIfActiveWhenStateIsNotActive() throws Exception {
    tcServer.setState(ServerMode.PASSIVE);
    expectedException.expect(UnexpectedStateException.class);
    tcServer.stopIfActive(RestartMode.STOP_ONLY);
  }

  @Test
  public void testStopIfActiveWithRestart() throws Exception {
    tcServer.setState(ServerMode.ACTIVE);
    expectedSystemExit.expectSystemExitWithStatus(EXITCODE_RESTART_REQUEST);
    tcServer.stopIfActive(RestartMode.STOP_AND_RESTART);
  }
}