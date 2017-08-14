package com.tc.stats;

import org.junit.Before;
import org.junit.Test;

import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.net.DSOChannelManagerMBean;
import com.tc.objectserver.core.api.GlobalServerStats;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.core.impl.ServerManagementContext;
import com.tc.util.PortChooser;

import java.net.ConnectException;
import java.net.Socket;

import javax.management.MBeanServer;

import static org.junit.Assert.*;
import org.junit.Ignore;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DSOTest {

  private DSO dso;

  @Before
  public void setUp() throws Exception {
    ServerManagementContext serverManagementContext = mock(ServerManagementContext.class);
    when(serverManagementContext.getServerStats()).thenReturn(mock(GlobalServerStats.class));
    DSOChannelManagerMBean dsoChannelManagerMBean = mock(DSOChannelManagerMBean.class);
    when(dsoChannelManagerMBean.getActiveChannels()).thenReturn(new MessageChannel[0]);
    when(serverManagementContext.getChannelManager()).thenReturn(dsoChannelManagerMBean);
    dso = new DSO(serverManagementContext, mock(ServerConfigurationContext.class), mock(MBeanServer.class));
  }

  @Test @Ignore("some enviroments don't like the socket stuff going on here")
  public void testJMXRemote() throws Exception {
    PortChooser portChooser = new PortChooser();
    final int jmxRemotePort = portChooser.chooseRandomPort();
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