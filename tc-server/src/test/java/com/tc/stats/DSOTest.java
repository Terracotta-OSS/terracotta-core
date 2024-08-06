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
package com.tc.stats;

import org.junit.Before;
import org.junit.Test;

import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.net.DSOChannelManagerMBean;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.core.impl.ServerManagementContext;

import java.net.ConnectException;
import java.net.Socket;

import javax.management.MBeanServer;

import static org.junit.Assert.*;
import org.junit.Ignore;
import org.terracotta.utilities.test.net.PortManager;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DSOTest {

  private DSO dso;

  @Before
  public void setUp() throws Exception {
    ServerManagementContext serverManagementContext = mock(ServerManagementContext.class);
    DSOChannelManagerMBean dsoChannelManagerMBean = mock(DSOChannelManagerMBean.class);
    when(dsoChannelManagerMBean.getActiveChannels()).thenReturn(new MessageChannel[0]);
    when(serverManagementContext.getChannelManager()).thenReturn(dsoChannelManagerMBean);
    dso = new DSO(serverManagementContext, mock(ServerConfigurationContext.class), mock(MBeanServer.class));
  }

  @Test @Ignore("some enviroments don't like the socket stuff going on here")
  public void testJMXRemote() throws Exception {
    try (PortManager.PortRef portRef = PortManager.getInstance().reservePort()) {
      final int jmxRemotePort = portRef.port();
      dso.setJmxRemotePort(String.valueOf(jmxRemotePort));
      dso.startJMXRemote();
      try {
        Socket socket = new Socket("localhost", jmxRemotePort);
        socket.close();
      } catch (ConnectException ce) {
        fail("couldn't connect to jmx remote at port " + jmxRemotePort);
      }

      try {
        dso.stopJMXRemote();
        new Socket("localhost", jmxRemotePort);
        fail("jmx remote stop failed");
      } catch (ConnectException expected) {}
    }
  }
}