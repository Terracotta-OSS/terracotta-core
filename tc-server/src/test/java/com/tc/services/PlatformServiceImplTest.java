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

import com.tc.exception.TCNotRunningException;
import org.junit.Before;
import org.junit.Test;
import org.terracotta.monitoring.PlatformService.RestartMode;

import com.tc.server.TCServer;
import java.util.Arrays;
import java.util.List;
import org.mockito.ArgumentMatcher;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mockito;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import org.terracotta.monitoring.PlatformStopException;
import org.terracotta.server.StopAction;
import static org.terracotta.server.StopAction.RESTART;
import static org.terracotta.server.StopAction.ZAP;

public class PlatformServiceImplTest {

  private TCServer tcServerMock;
  private PlatformServiceImpl platformService;

  @Before
  public void setUp() throws Exception {
    tcServerMock = mock(TCServer.class);
    platformService = new PlatformServiceImpl(tcServerMock);
  }

  @Test(expected=PlatformStopException.class)
  public void stopPlatformIfPassive() throws Exception {
    try {
      platformService.stopPlatformIfPassive(RestartMode.STOP_ONLY);
      verify(tcServerMock).stopIfPassive();
    } catch (TCNotRunningException expected) {
      
    }
    
    try {
      platformService.stopPlatformIfPassive(RestartMode.STOP_AND_RESTART);
      verify(tcServerMock).stopIfPassive(Mockito.eq(StopAction.RESTART));
    } catch (TCNotRunningException expected) {
      
    }
    
    try {
      platformService.stopPlatformIfPassive(RestartMode.ZAP_AND_RESTART);
//    verify(tcServerMock).stopIfPassive(Mockito.argThat(varargs(RestartMode.ZAP_AND_RESTART)));
      verify(tcServerMock).stopIfPassive(Mockito.eq(StopAction.ZAP), Mockito.eq(StopAction.RESTART));
    } catch (TCNotRunningException expected) {
      
    }
    
    try {
      platformService.stopPlatformIfPassive(RestartMode.ZAP_AND_STOP);
      verify(tcServerMock).stopIfPassive(Mockito.eq(StopAction.ZAP));
    } catch (TCNotRunningException expected) {
      
    }

    Mockito.doThrow(new PlatformStopException("not passive")).when(tcServerMock).stopIfPassive(any());
    platformService.stopPlatformIfPassive(RestartMode.STOP_ONLY);
  }

  @Test(expected=PlatformStopException.class)
  public void stopPlatformIfActive() throws Exception {
    try {
      platformService.stopPlatformIfActive(RestartMode.STOP_ONLY);
      verify(tcServerMock).stopIfActive();
    } catch (TCNotRunningException expected) {
      
    }

    try {
      platformService.stopPlatformIfActive(RestartMode.STOP_AND_RESTART);
      verify(tcServerMock).stopIfActive(Mockito.eq(StopAction.RESTART));
    } catch (TCNotRunningException expected) {
      
    }

    try {
      platformService.stopPlatformIfActive(RestartMode.ZAP_AND_RESTART);
      verify(tcServerMock).stopIfActive(Mockito.eq(StopAction.ZAP), Mockito.eq(StopAction.RESTART));
    } catch (TCNotRunningException expected) {
      
    }
    
    try {
      platformService.stopPlatformIfActive(RestartMode.ZAP_AND_STOP);
      verify(tcServerMock).stopIfActive(Mockito.eq(StopAction.ZAP));
    } catch (TCNotRunningException expected) {
      
    }

    Mockito.doThrow(new PlatformStopException("not active")).when(tcServerMock).stopIfActive(any());
    platformService.stopPlatformIfActive(RestartMode.STOP_ONLY);
  }

  @Test
  public void stopPlatform() {
    try {
      platformService.stopPlatform(RestartMode.STOP_ONLY);
      verify(tcServerMock).stop();
    } catch (TCNotRunningException expect) {
      
    }

    try {
      platformService.stopPlatform(RestartMode.STOP_AND_RESTART);
      verify(tcServerMock).stop(Mockito.eq(StopAction.RESTART));
    } catch (TCNotRunningException expect) {
      
    }
    try {
      platformService.stopPlatform(RestartMode.ZAP_AND_RESTART);
//    verify(tcServerMock).stop(Mockito.argThat(varargs(RestartMode.ZAP_AND_RESTART)));
      verify(tcServerMock).stop(Mockito.eq(StopAction.ZAP), Mockito.eq(StopAction.RESTART));
    } catch (TCNotRunningException expect) {
      
    }

    try {
      platformService.stopPlatform(RestartMode.ZAP_AND_STOP);
      verify(tcServerMock).stop(Mockito.eq(StopAction.ZAP));
    } catch (TCNotRunningException expect) {
      
    }
  }

  private ArgumentMatcher<StopAction[]> varargs(RestartMode mode) {
    switch (mode) {
      case STOP_AND_RESTART:
        return a->false;
      case STOP_ONLY:
        return a->false;
      case ZAP_AND_RESTART:
        return o->{
          List<StopAction> l = Arrays.asList(o);
          return l.remove(RESTART) && l.remove(ZAP) && l.isEmpty();
        };
      case ZAP_AND_STOP:
        return a->false;
      default:
        return null;
    }
  }
}