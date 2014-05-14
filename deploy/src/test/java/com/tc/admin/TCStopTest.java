package com.tc.admin;

import org.junit.Test;

import com.tc.config.schema.CommonL2Config;
import com.tc.object.config.schema.L2DSOConfigObject;
import com.terracottatech.config.BindPort;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TCStopTest {

  @Test
  public void computeJMXPortDefined() {
    int jmxPort = 9525;

    CommonL2Config l2Config = mock(CommonL2Config.class);
    BindPort bindPort = mock(BindPort.class);

    when(l2Config.jmxPort()).thenReturn(bindPort);
    when(bindPort.getIntValue()).thenReturn(jmxPort);

    int jmxPortResult = TCStop.computeJMXPort(l2Config);
    assertThat(jmxPortResult, is(jmxPort));
  }

  @Test
  public void computeJMXPortDefinedAsZero() {
    CommonL2Config l2Config = mock(CommonL2Config.class);
    BindPort bindPort = mock(BindPort.class);

    when(l2Config.jmxPort()).thenReturn(bindPort);
    when(bindPort.getIntValue()).thenReturn(0);

    int jmxPortResult = TCStop.computeJMXPort(l2Config);
    assertThat(jmxPortResult, is(TCStop.DEFAULT_PORT));
  }

  @Test
  public void computeJMXPortUndefined() {
    int tsaPort = 9515;

    CommonL2Config l2Config = mock(CommonL2Config.class);
    BindPort bindPort = mock(BindPort.class);

    when(l2Config.tsaPort()).thenReturn(bindPort);
    when(bindPort.getIntValue()).thenReturn(tsaPort);

    int jmxPortResult = TCStop.computeJMXPort(l2Config);
    assertThat(jmxPortResult, is(tsaPort + L2DSOConfigObject.DEFAULT_JMXPORT_OFFSET_FROM_TSAPORT));
  }

}