/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.map;

import org.terracotta.express.tests.base.AbstractExpressActivePassiveTest;
import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.collections.ToolkitList;
import org.terracotta.toolkit.internal.cache.ToolkitCacheInternal;
import org.terracotta.toolkit.store.ToolkitStoreConfigBuilder;

import com.tc.test.config.model.TestConfig;

import java.io.Serializable;
import java.util.concurrent.BrokenBarrierException;

import junit.framework.Assert;

public class ClusteredMapCompressionTest extends AbstractExpressActivePassiveTest {

  public ClusteredMapCompressionTest(TestConfig testConfig) {
    super(testConfig, ClusteredMapCompressionTestClient.class, ClusteredMapCompressionTestClient.class);
  }

  public static class ClusteredMapCompressionTestClient extends ClientBase {

    public ClusteredMapCompressionTestClient(String[] args) {
      super(args);
    }

    @Override
    protected void test(Toolkit toolkit) throws Throwable {
      basicTest(toolkit);
    }

    private void basicTest(Toolkit toolkit) throws InterruptedException, BrokenBarrierException {
      ToolkitStoreConfigBuilder builder = new ToolkitStoreConfigBuilder();
      builder.compressionEnabled(true);
      ToolkitCacheInternal<String, MyObject> map = (ToolkitCacheInternal<String, MyObject>) toolkit
          .getCache("compressionEnabledMap", builder.build(), MyObject.class);
      ToolkitCacheInternal<String, MyObject> unCompressedMap = (ToolkitCacheInternal<String, MyObject>) toolkit
          .getCache("compressionDisabledMap", builder.build(), MyObject.class);
      ToolkitList<MyObject> list = toolkit.getList("UnCompressedList", null);
      int index = getBarrierForAllClients().await();
      if (index == 0) {
        for (int i = 0; i < 1000; i++) {
          MyObject value = getValue(i);
          unCompressedMap.put(String.valueOf(i), value);
          map.put(String.valueOf(i), value);
          list.add(value);
        }
        map.size();
      }
      getBarrierForAllClients().await();
      for (int i = 0; i < 1000; i++) {
        MyObject valueInList = list.get(i);
        MyObject valueInMap = map.get(String.valueOf(i));
        Assert.assertEquals(valueInMap, getValue(i));
        Assert.assertEquals(valueInList, getValue(i));
        Assert.assertEquals(valueInList, valueInMap);
        System.out.println("Compressed Map Size: " + (map.localOffHeapSizeInBytes() + map.localOnHeapSizeInBytes()));
        System.out.println("UnCompressed Map Size: "
                           + (unCompressedMap.localOffHeapSizeInBytes() + unCompressedMap.localOnHeapSizeInBytes()));

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
