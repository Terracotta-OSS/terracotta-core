package com.tc.object.config.schema;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertThat;

public class L2ConfigObjectTest {

  @Test
  public void computeJMXPortFromTSAPortDefault() {
    int tsaPort = 9510;
    assertThat(L2ConfigObject.computeJMXPortFromTSAPort(tsaPort), is(tsaPort + L2ConfigObject.DEFAULT_JMXPORT_OFFSET_FROM_TSAPORT));
  }

  @Test
  public void computeJMXPortFromTSAPortAboveMaximumPort() {
    int tsaPort = L2ConfigObject.MAX_PORTNUMBER - 1;
    int jmxPort = L2ConfigObject.computeJMXPortFromTSAPort(tsaPort);
    assertThat(jmxPort, greaterThanOrEqualTo(L2ConfigObject.MIN_PORTNUMBER));
    assertThat(jmxPort, lessThanOrEqualTo(L2ConfigObject.MAX_PORTNUMBER));
  }

}