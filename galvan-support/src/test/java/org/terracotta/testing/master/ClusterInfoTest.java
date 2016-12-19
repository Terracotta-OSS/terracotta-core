package org.terracotta.testing.master;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;


/**
 * Tests that {@link ClusterInfo} can be correctly encoded and decoded.
 */
public class ClusterInfoTest {
  @Test
  public void testEncoding() throws Exception {
    String testName = "test";
    int testServerPort = 1234, testGroupPort = 1235;

    Map<String, ServerInfo> servers = new HashMap<>();
    servers.put(testName, new ServerInfo(testName, testServerPort, testGroupPort));

    ClusterInfo clusterInfo = new ClusterInfo(servers);
    String encoded = clusterInfo.encode();
    ClusterInfo decoded = ClusterInfo.decode(encoded);

    ServerInfo serverInfo = decoded.getServersInfo().iterator().next();
    Assert.assertEquals(testName, serverInfo.getName());
    Assert.assertEquals(testServerPort, serverInfo.getServerPort());
    Assert.assertEquals(testGroupPort, serverInfo.getGroupPort());
  }
}
