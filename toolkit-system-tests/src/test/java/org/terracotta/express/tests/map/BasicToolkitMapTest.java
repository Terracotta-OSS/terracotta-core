/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.map;

import org.terracotta.express.tests.base.AbstractToolkitTestBase;
import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.collections.ToolkitMap;
import org.terracotta.toolkit.concurrent.ToolkitBarrier;

import com.tc.test.config.model.TestConfig;

import java.io.Serializable;

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
      Assert.assertEquals(10, o.i);
      o = (MyInt) map.get(20);
      System.err.println("Got for 20 " + o);
      Assert.assertEquals(20, o.i);

      Integer i = (Integer) map.get(30);
      System.err.println("Got for 30 " + i);

      Assert.assertEquals(30, i.intValue());
    }
  }

  private static class MyInt implements Serializable {
    private final int i;

    public MyInt(int i) {
      this.i = i;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + i;
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      MyInt other = (MyInt) obj;
      if (i != other.i) return false;
      return true;
    }

    @Override
    public String toString() {
      return "MyInt [i=" + i + "]";
    }

  }

}
