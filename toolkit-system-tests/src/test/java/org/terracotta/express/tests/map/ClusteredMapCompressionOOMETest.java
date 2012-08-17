/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.map;

import org.terracotta.express.tests.base.AbstractToolkitTestBase;
import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.store.ToolkitStore;
import org.terracotta.toolkit.store.ToolkitStoreConfigBuilder;

import com.tc.test.config.model.TestConfig;

public class ClusteredMapCompressionOOMETest extends AbstractToolkitTestBase {

  public ClusteredMapCompressionOOMETest(TestConfig testConfig) {
    super(testConfig, ClusteredMapCompressionOOMETestClient.class);
  }

  public static class ClusteredMapCompressionOOMETestClient extends ClientBase {
    public ClusteredMapCompressionOOMETestClient(String[] args) {
      super(args);
    }

    @Override
    protected void test(Toolkit toolkit) throws Throwable {
      ToolkitStore map = createMap(toolkit);
      doPutsAndGets(map);
    }

    private ToolkitStore createMap(Toolkit toolkit) {
      ToolkitStoreConfigBuilder builder = new ToolkitStoreConfigBuilder();
      builder.compressionEnabled(true);
      builder.maxBytesLocalHeap(32 * 1024 * 1024);
      ToolkitStore map = toolkit.getStore("compressionEnabledMap", builder.build(), null);
      return map;
    }

    private void doPutsAndGets(ToolkitStore map) {
      for (int i = 0; i < 300; i++) {
        map.putNoReturn(i, new byte[1024 * 1024]);
        map.get(i);
        System.err.println("Put " + i + " success");
      }
    }
  }

}
