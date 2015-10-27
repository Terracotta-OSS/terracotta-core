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

package com.tc.object.config.schema;

import com.tc.object.config.schema.L2ConfigObject;
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