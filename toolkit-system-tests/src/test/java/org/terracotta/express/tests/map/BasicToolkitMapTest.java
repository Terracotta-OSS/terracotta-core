/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.map;

import org.terracotta.express.tests.base.AbstractToolkitTestBase;
import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.express.toolkit.api.tests.MyInt;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.collections.ToolkitMap;
import org.terracotta.toolkit.concurrent.ToolkitBarrier;

import com.tc.test.config.model.TestConfig;

import junit.framework.Assert;

public class BasicToolkitMapTest extends AbstractToolkitTestBase {
  public BasicToolkitMapTest(TestConfig testConfig) {
    super(testConfig, BasicToolkitMapTestClient.class, BasicToolkitMapTestClient.class);
  }

  public static class BasicToolkitMapTestClient extends ClientBase {
    public BasicToolkitMapTestClient(String[] args) {
      super(args);
    }

    @Override
    protected void test(Toolkit toolkit) throws Throwable {
      ToolkitMap map = toolkit.getMap("myMap", null, null);
      ToolkitBarrier barrier = toolkit.getBarrier("myBarrier", 2);
      int index = barrier.await();

      if (index == 0) {
        map.put(new MyInt(10), new MyInt(10));
        map.put(20, new MyInt(20));
        map.put(30, 30);
      }

      barrier.await();

      MyInt o = (MyInt) map.get(new MyInt(10));
      System.err.println("Got for 10 " + o);
      Assert.assertEquals(10, o.getI());
      o = (MyInt) map.get(20);
      System.err.println("Got for 20 " + o);
      Assert.assertEquals(20, o.getI());

      Integer i = (Integer) map.get(30);
      System.err.println("Got for 30 " + i);

      Assert.assertEquals(30, i.intValue());
    }
  }


}
