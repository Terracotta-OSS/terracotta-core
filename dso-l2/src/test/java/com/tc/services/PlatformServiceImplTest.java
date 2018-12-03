package com.tc.services;

import org.junit.Before;
import org.junit.Test;
import org.terracotta.monitoring.PlatformService.RestartMode;

import com.tc.server.TCServer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class PlatformServiceImplTest {

  private TCServer tcServerMock;
  private PlatformServiceImpl platformService;

  @Before
  public void setUp() throws Exception {
    tcServerMock = mock(TCServer.class);
    platformService = new PlatformServiceImpl(tcServerMock);
  }

  @Test
  public void stopPlatformIfPassive() throws Exception {
    platformService.stopPlatformIfPassive(RestartMode.STOP_ONLY);
    verify(tcServerMock).stopIfPassive(RestartMode.STOP_ONLY);

    platformService.stopPlatformIfPassive(RestartMode.STOP_AND_RESTART);
    verify(tcServerMock).stopIfPassive(RestartMode.STOP_AND_RESTART);
  }

  @Test
  public void stopPlatformIfActive() throws Exception {
    platformService.stopPlatformIfActive(RestartMode.STOP_ONLY);
    verify(tcServerMock).stopIfActive(RestartMode.STOP_ONLY);

    platformService.stopPlatformIfActive(RestartMode.STOP_AND_RESTART);
    verify(tcServerMock).stopIfActive(RestartMode.STOP_AND_RESTART);
  }

  @Test
  public void stopPlatform() {
    platformService.stopPlatform(RestartMode.STOP_ONLY);
    verify(tcServerMock).stop(RestartMode.STOP_ONLY);

    platformService.stopPlatform(RestartMode.STOP_AND_RESTART);
    verify(tcServerMock).stop(RestartMode.STOP_AND_RESTART);
  }
}