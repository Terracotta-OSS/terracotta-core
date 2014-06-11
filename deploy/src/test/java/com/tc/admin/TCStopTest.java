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
  public void computeManagementPortDefined() {
    int managementPort = 9525;

    CommonL2Config l2Config = mock(CommonL2Config.class);
    BindPort bindPort = mock(BindPort.class);

    when(l2Config.managementPort()).thenReturn(bindPort);
    when(bindPort.getIntValue()).thenReturn(managementPort);

    int managementPortResult = TCStop.computeManagementPort(l2Config);
    assertThat(managementPortResult, is(managementPort));
  }

  @Test
  public void computeManagementPortDefinedAsZero() {
    CommonL2Config l2Config = mock(CommonL2Config.class);
    BindPort bindPort = mock(BindPort.class);

    when(l2Config.managementPort()).thenReturn(bindPort);
    when(bindPort.getIntValue()).thenReturn(0);

    int managementPortResult = TCStop.computeManagementPort(l2Config);
    assertThat(managementPortResult, is(TCStop.DEFAULT_PORT));
  }

  @Test
  public void computeManagementPortUndefined() {
    int tsaPort = 9515;

    CommonL2Config l2Config = mock(CommonL2Config.class);
    BindPort bindPort = mock(BindPort.class);

    when(l2Config.tsaPort()).thenReturn(bindPort);
    when(bindPort.getIntValue()).thenReturn(tsaPort);

    int managementPortResult = TCStop.computeManagementPort(l2Config);
    assertThat(managementPortResult, is(tsaPort + L2DSOConfigObject.DEFAULT_MANAGEMENTPORT_OFFSET_FROM_TSAPORT));
  }

}