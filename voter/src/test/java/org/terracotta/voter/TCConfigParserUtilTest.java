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
package org.terracotta.voter;

import com.tc.config.schema.L2ConfigForL1;
import org.junit.Test;

import static org.hamcrest.Matchers.arrayContaining;
import static org.junit.Assert.assertThat;

public class TCConfigParserUtilTest {

  TCConfigParserUtil parser = new TCConfigParserUtil();

  @Test
  public void testParseHostPorts() throws Exception {
    String hostPort1 = "foo:1234";
    String hostPort2 = "bar:2345";

    String[] hostPorts = parser.parseHostPorts(getClass().getClassLoader().getResourceAsStream("tc-config.xml"));
    assertThat(hostPorts, arrayContaining(hostPort1, hostPort2));

  }

  @Test
  public void testParseHostPortsOnlyParsesServerTags() throws Exception {
    String hostPort1 = "foo:1234";
    String hostPort2 = "bar:2345";

    String[] hostPorts = parser.parseHostPorts(getClass().getClassLoader().getResourceAsStream("tc-config-services.xml"));
    assertThat(hostPorts, arrayContaining(hostPort1, hostPort2));

  }
  

  @Test
  public void testParseDefaultPorts() throws Exception {
    String hostPort1 = "foo:" + L2ConfigForL1.DEFAULT_PORT;
    String hostPort2 = "bar:" + L2ConfigForL1.DEFAULT_PORT;

    String[] hostPorts = parser.parseHostPorts(getClass().getClassLoader().getResourceAsStream("tc-config-services-noport.xml"));
    assertThat(hostPorts, arrayContaining(hostPort1, hostPort2));

  }  

}