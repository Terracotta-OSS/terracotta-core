/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.map;

import org.terracotta.express.tests.base.AbstractExpressActivePassiveTest;
import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.cache.ToolkitCache;
import org.terracotta.toolkit.store.ToolkitStoreConfigBuilder;

import com.tc.test.config.model.TestConfig;

import java.io.Serializable;

import junit.framework.Assert;

public class ClusteredMapCopyOnReadTest extends AbstractExpressActivePassiveTest {

  public ClusteredMapCopyOnReadTest(TestConfig testConfig) {
    super(testConfig, ClusteredMapCopyOnReadTestClient.class);
    testConfig.getClientConfig().addExtraClientJvmArg("-Dtc.classloader.writeToDisk=true");
    testConfig.getL2Config().addExtraServerJvmArg("-Dtc.classloader.writeToDisk=true");
  }

  public static class ClusteredMapCopyOnReadTestClient extends ClientBase {

    public ClusteredMapCopyOnReadTestClient(String[] args) {
      super(args);
    }

    @Override
    protected void test(Toolkit toolkit) throws Throwable {
      basicTest(toolkit);
    }

    private void basicTest(Toolkit toolkit) {
      ToolkitStoreConfigBuilder builder = new ToolkitStoreConfigBuilder();
      builder.copyOnReadEnabled(true);
      ToolkitCache<String, MyObject> copyOnReadEnabledMap = toolkit.getCache("copyOnReadEnabledMap", builder.build(),
                                                                             null);
      builder.copyOnReadEnabled(false);
      ToolkitCache<String, MyObject> copyOnReadDisabledMap = toolkit.getCache("copyOnReadDisabled", builder.build(),
                                                                              null);
      for (int i = 0; i < 1000; i++) {
        MyObject value = getValue(i);
        copyOnReadDisabledMap.put(String.valueOf(i), value);
        copyOnReadEnabledMap.put(String.valueOf(i), value);
      }
      copyOnReadEnabledMap.size();
      for (int i = 0; i < 1000; i++) {
        Assert.assertEquals(copyOnReadEnabledMap.get(String.valueOf(i)), getValue(i));
        Assert.assertEquals(copyOnReadDisabledMap.get(String.valueOf(i)), getValue(i));
        Assert.assertTrue(copyOnReadDisabledMap.get(String.valueOf(i)) == copyOnReadDisabledMap.get(String.valueOf(i)));
        Assert.assertFalse(copyOnReadEnabledMap.get(String.valueOf(i)) == copyOnReadEnabledMap.get(String.valueOf(i)));
      }
    }

    private MyObject getValue(int i) {
      StringBuilder sb = new StringBuilder();
      for (int j = 0; j < 1024; j++) {
        sb.append(i);
      }
      return new MyObject(sb.toString());
    }

  }

  public static class MyObject implements Serializable {
    private final String value;

    public MyObject(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof MyObject ? ((MyObject) obj).value.equals(value) : false;
    }

    @Override
    public String toString() {
      return "value=[" + value + "] ";
    }
  }
}
