/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.map;

import org.terracotta.express.tests.base.AbstractToolkitTestBase;
import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.collections.ToolkitMap;

import com.tc.test.config.model.TestConfig;

import java.io.Serializable;

import junit.framework.Assert;

public class SimpleMapTest extends AbstractToolkitTestBase {

  public SimpleMapTest(TestConfig testConfig) {
    super(testConfig, SimpleMapTestClient.class, SimpleMapTestClient2.class);
    testConfig.getClientConfig().setParallelClients(false);
    // testConfig.setNumOfGroups(2);
  }

  public static class SimpleMapTestClient extends ClientBase {

    public SimpleMapTestClient(String[] args) {
      super(args);
    }

    public static void main(String[] args) {
      new SimpleMapTestClient(args).run();
    }

    @Override
    protected void test(Toolkit toolkit) throws Throwable {
      System.out.println("Got toolkit: " + toolkit);

      System.out.println("Getting map...");
      ToolkitMap<Serializable, String> map = toolkit.getMap("someMap", null, null);
      System.out.println("Got map: " + map.getClass().getName());

      map.put("someKey", "value");
      System.out.println("Inserted key-value");

      String value = map.get("someKey");
      System.out.println("Got value: " + value);
      Assert.assertEquals("value", value);

      // TODO: fix this... before exiting wait for all txns to complete
      int count = 0;
      while (true) {
        System.out.println("Sleeping for one sec... elapsed: " + count + " secs");
        Thread.sleep(1000);
        count++;
        if (count >= 10) {
          break;
        }
      }
    }
  }

  public static class SimpleMapTestClient2 extends ClientBase {

    public SimpleMapTestClient2(String[] args) {
      super(args);
    }

    public static void main(String[] args) {
      new SimpleMapTestClient2(args).run();
    }

    @Override
    protected void test(Toolkit toolkit) throws Throwable {
      System.out.println("Got toolkit: " + toolkit);

      System.out.println("Getting map...");
      ToolkitMap<Serializable, String> map = toolkit.getMap("someMap", null, null);
      System.out.println("Got map: " + map.getClass().getName());

      System.out.println("Doing get on map");

      String value = map.get("someKey");
      System.out.println("Got value: " + value);
      Assert.assertEquals("value", value);
    }
  }

}
