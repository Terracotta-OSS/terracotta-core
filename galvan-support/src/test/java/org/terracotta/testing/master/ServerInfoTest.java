package org.terracotta.testing.master;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author vmad
 */
public class ServerInfoTest {
  @Test
  public void testEncoding() throws Exception {
    String testName = "test";
    int testServerPort = 1234;
    int testGroupPort = 1235;

    ServerInfo serverInfo = new ServerInfo(testName, testServerPort, testGroupPort);
    String encoded = serverInfo.encode();
    ServerInfo decoded = ServerInfo.decode(encoded);

    Assert.assertEquals(testName, decoded.getName());
    Assert.assertEquals(testServerPort, decoded.getServerPort());
    Assert.assertEquals(testGroupPort, decoded.getGroupPort());
  }

}