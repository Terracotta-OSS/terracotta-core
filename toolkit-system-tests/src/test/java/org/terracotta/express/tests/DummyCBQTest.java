/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests;

import org.terracotta.express.tests.base.AbstractToolkitTestBase;
import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.collections.ToolkitBlockingQueue;

import com.tc.test.config.model.TestConfig;

import java.io.Serializable;

import junit.framework.Assert;

public class DummyCBQTest extends AbstractToolkitTestBase {

  public DummyCBQTest(TestConfig testConfig) {
    super(testConfig, DummyCBQTestClient1.class, DummyCBQTestClient2.class);
    testConfig.getClientConfig().setParallelClients(false);
  }

  public static class DummyCBQTestClient1 extends ClientBase {

    public DummyCBQTestClient1(String[] args) {
      super(args);
    }

    @Override
    protected void test(Toolkit toolkit) throws Throwable {
      ToolkitBlockingQueue<DataPojo> cbq = toolkit.getBlockingQueue("dummyCBQ", 5, null);
      DataPojo pojo = new DataPojo("abc", 123, new DataPojo2("def", 456));
      cbq.add(pojo);
      pojo = new DataPojo("abc1", 123, new DataPojo2("def1", 456));
      cbq.add(pojo);
      System.out.println("abhim size " + cbq.size() + " name " + cbq.getName() + " capacity " + cbq.getCapacity());
    }
  }

  public static class DummyCBQTestClient2 extends ClientBase {

    public DummyCBQTestClient2(String[] args) {
      super(args);
    }

    @Override
    protected void test(Toolkit toolkit) throws Throwable {
      ToolkitBlockingQueue<DataPojo> cbq = toolkit.getBlockingQueue("dummyCBQ", 5, null);
      DataPojo actual = cbq.take();
      DataPojo expected = new DataPojo("abc", 123, new DataPojo2("def", 456));
      Assert.assertEquals(expected.toString(), actual.toString());
      System.out.println("abhim size " + cbq.size() + " name " + cbq.getName() + " capacity " + cbq.getCapacity());
    }
  }

  public static class DataPojo implements Serializable {
    private final String    stringValue;
    private final int       intValue;
    private final DataPojo2 reference;

    public DataPojo(String stringValue, int intValue, DataPojo2 reference) {
      super();
      this.stringValue = stringValue;
      this.intValue = intValue;
      this.reference = reference;
    }

    @Override
    public String toString() {
      return "DataPojo [stringValue=" + stringValue + ", intValue=" + intValue + ", reference=" + reference + "]";
    }

  }

  public static class DataPojo2 implements Serializable {
    private final String stringValue;
    private final int    intValue;

    public DataPojo2(String stringValue, int intValue) {
      super();
      this.stringValue = stringValue;
      this.intValue = intValue;
    }

    @Override
    public String toString() {
      return "DataPojo2 [stringValue=" + stringValue + ", intValue=" + intValue + "]";
    }

  }

}
