package com.tc.server.util;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;

import com.tc.object.config.schema.L2DSOConfigObject;
import com.terracottatech.config.BindPort;
import com.terracottatech.config.Server;

public class ServerStatTest {

  @Test
  public void computeJMXPortDefined() {
    int jmxPort = 9525;

    Server server = mock(Server.class);
    BindPort bindPort = mock(BindPort.class);

    when(server.isSetJmxPort()).thenReturn(true);
    when(server.getJmxPort()).thenReturn(bindPort);
    when(bindPort.getIntValue()).thenReturn(jmxPort);

    int jmxPortResult = ServerStat.computeJMXPort(server);
    assertThat(jmxPortResult, is(jmxPort));
  }

  @Test
  public void computeJMXPortDefinedAsZero() {
    Server server = mock(Server.class);
    BindPort bindPort = mock(BindPort.class);

    when(server.isSetJmxPort()).thenReturn(true);
    when(server.getJmxPort()).thenReturn(bindPort);
    when(bindPort.getIntValue()).thenReturn(0);

    int jmxPortResult = ServerStat.computeJMXPort(server);
    assertThat(jmxPortResult, is(ServerStat.DEFAULT_MANAGEMENT_PORT));
  }

  @Test
  public void computeJMXPortUndefined() {
    int tsaPort = 9525;

    Server server = mock(Server.class);
    BindPort bindPort = mock(BindPort.class);

    when(server.isSetJmxPort()).thenReturn(false);
    when(server.getTsaPort()).thenReturn(bindPort);
    when(bindPort.getIntValue()).thenReturn(tsaPort);

    int jmxPortResult = ServerStat.computeJMXPort(server);
    assertThat(jmxPortResult, is(tsaPort + L2DSOConfigObject.DEFAULT_JMXPORT_OFFSET_FROM_TSAPORT));
  }

  @Test
  public void decodeJsonAndSetFieldsTest() {
    ServerStat.decodeJsonAndSetFields("{ \"health\" : \"OK\", \"role\" : \"ACTIVE\", \"state\": \"ACTIVE-COORDINATOR\", \"managementPort\" : \"9540\", \"serverGroupName\" : \"defaultGroup\"}");
    assertThat(ServerStat.health, is("OK"));
    assertThat(ServerStat.role, is("ACTIVE"));
    assertThat(ServerStat.state, is("ACTIVE-COORDINATOR"));
    assertThat(ServerStat.groupName, is("defaultGroup"));
  }
  
}