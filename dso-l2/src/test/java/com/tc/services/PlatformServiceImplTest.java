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