/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.map;

import org.terracotta.express.tests.base.AbstractToolkitTestBase;
import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.toolkit.Toolkit;

import com.tc.test.config.model.TestConfig;

public class ClusteredMapCreationTest extends AbstractToolkitTestBase {

  public ClusteredMapCreationTest(TestConfig testConfig) {
    super(testConfig, ClusteredMapCreationTestClient.class, ClusteredMapCreationTestClient.class);
    testConfig.getClientConfig().setMaxHeap(200);
  }

  public static class ClusteredMapCreationTestClient extends ClientBase {
    private static int NUM_OF_MAPS = 250;

    public ClusteredMapCreationTestClient(String[] args) {
      super(args);
    }

    @Override
    protected void test(Toolkit toolkit) throws Throwable {
      for (int i = 0; i < NUM_OF_MAPS; i++) {
        toolkit.getMap("name" + i, null, null).put(i, i);
      }

    }
  }

}
