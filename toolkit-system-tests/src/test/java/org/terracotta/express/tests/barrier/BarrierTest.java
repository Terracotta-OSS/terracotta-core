/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.barrier;

import org.terracotta.express.tests.base.AbstractToolkitTestBase;
import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.express.tests.util.ClusteredStringBuilder;
import org.terracotta.express.tests.util.ClusteredStringBuilderFactory;
import org.terracotta.express.tests.util.ClusteredStringBuilderFactoryImpl;
import org.terracotta.toolkit.Toolkit;

import com.tc.test.config.model.TestConfig;

import junit.framework.Assert;

public class BarrierTest extends AbstractToolkitTestBase {

  private static final String clientBucket = "BarrierClientBucket";

  public BarrierTest(TestConfig testConfig) {
    super(testConfig, BarrierClient1.class, BarrierClient2.class);
    testConfig.getClientConfig().setParallelClients(true);
  }

  public static class BarrierClient1 extends ClientBase {

    private final static String EXPECTED_VALUE = "0123456789-9876543210";

    public BarrierClient1(String[] args) {
      super(args);
    }

    public static void main(String[] args) {
      new BarrierClient1(args).run();
    }

    @Override
    public void test(Toolkit toolkit) throws Throwable {

      testIllegalParties(toolkit, 0);
      testIllegalParties(toolkit, -1);
      testIllegalParties(toolkit, -100);

      ClusteredStringBuilderFactory csbFactory = new ClusteredStringBuilderFactoryImpl(toolkit);
      ClusteredStringBuilder bucket = csbFactory.getClusteredStringBuilder(clientBucket);
      Thread.sleep(1000L);
      for (int i = 0; i < 10; i++) {
        bucket.append(i + "");
        debug("added " + i);
      }
      debug("1. Waiting for other client...");
      getBarrierForAllClients().await();
      debug("DONE: 1. Waiting for other client...");
      debug("2. Waiting for other client...");
      getBarrierForAllClients().await();
      debug("DONE: 2. Waiting for other client...");
      System.out.println(bucket);
      Assert.assertEquals(EXPECTED_VALUE, bucket.toString());
    }

    private void testIllegalParties(Toolkit toolkit, int parties) {
      try {
        toolkit.getBarrier("abcdef", parties);
        Assert.fail("should have thrown exception for illegal parties - " + parties);
      } catch (IllegalArgumentException expected) {
        //
      }
    }

    public static void debug(String string) {
      System.out.println(string);
    }
  }

  public static class BarrierClient2 extends ClientBase {

    public BarrierClient2(String[] args) {
      super(args);
    }

    public static void main(String[] args) {
      new BarrierClient2(args).run();
    }

    @Override
    public void test(Toolkit toolkit) throws Throwable {
      ClusteredStringBuilderFactory csbFactory = new ClusteredStringBuilderFactoryImpl(toolkit);
      ClusteredStringBuilder bucket = csbFactory.getClusteredStringBuilder(clientBucket);
      debug("1. Waiting for other client...");
      getBarrierForAllClients().await();
      debug("DONE: 1. Waiting for other client...");
      bucket.append("-");
      for (int i = 9; i >= 0; i--) {
        bucket.append(i + "");
        debug("added " + i);
      }
      debug("2. Waiting for other client...");
      getBarrierForAllClients().await();
      debug("DONE: 2. Waiting for other client...");
      System.out.println(bucket);
      Assert.assertEquals(BarrierClient1.EXPECTED_VALUE, bucket.toString());
    }
  }

}
