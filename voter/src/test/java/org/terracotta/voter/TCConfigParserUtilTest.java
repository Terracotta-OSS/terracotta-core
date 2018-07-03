package org.terracotta.voter;

import org.junit.jupiter.api.Test;

import com.tc.config.schema.L2ConfigForL1;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;

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