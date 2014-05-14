package com.tc.object.config.schema;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertThat;

public class L2DSOConfigObjectTest {

  @Test
  public void computeJMXPortFromTSAPortDefault() {
    int tsaPort = 9510;
    assertThat(L2DSOConfigObject.computeJMXPortFromTSAPort(tsaPort), is(tsaPort + L2DSOConfigObject.DEFAULT_JMXPORT_OFFSET_FROM_TSAPORT));
  }

  @Test
  public void computeJMXPortFromTSAPortAboveMaximumPort() {
    int tsaPort = L2DSOConfigObject.MAX_PORTNUMBER - 1;
    int jmxPort = L2DSOConfigObject.computeJMXPortFromTSAPort(tsaPort);
    assertThat(jmxPort, greaterThanOrEqualTo(L2DSOConfigObject.MIN_PORTNUMBER));
    assertThat(jmxPort, lessThanOrEqualTo(L2DSOConfigObject.MAX_PORTNUMBER));
  }

}