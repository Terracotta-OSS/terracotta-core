/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.toolkit.api.tests;

import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.collections.ToolkitMap;

import java.util.concurrent.BrokenBarrierException;

import junit.framework.Assert;

public class ToolkitMapApiKeyValGrClient extends AbstractToolkitApiTestClientUtil {
  private Toolkit    toolkit;
  private ToolkitMap toolkitMap;

  @Override
  protected void test(Toolkit toolKit) throws Throwable {
    this.toolkit = toolKit;
    setDs(toolkit);
    keyValueGenerator = new LiteralKeyLiteralValueGenerator();
    super.test(toolkit);
    this.test();
    keyValueGenerator = new NonLiteralKeyLiteralValueGenerator();
    super.test(toolkit);
    this.test();
    keyValueGenerator = new NonLiteralKeyNonLiteralValueGenerator();
    super.test(toolkit);
    this.test();
  }

  public void test() throws Throwable {
    checkDestroy();
    checkGetName();
    checkIsDestroyed();
    someMethod();
  }

  public void someMethod() throws InterruptedException, BrokenBarrierException {
    setUp();
    try {
      index = barrier.await();
      System.err.println("index = " + index);
      if (index == 1) {
        System.err.println("doing some puts");
        doSomePuts(START, END);
        System.err.println("puts done");
      }
      barrier.await();
      System.err.println("index = " + index);

    } finally {
      tearDown();
    }
  }

  public ToolkitMapApiKeyValGrClient(String[] args) {
    super(args);
  }

  @Override
  public void setDs(Toolkit toolkit) {
    barrier = toolkit.getBarrier("mybarr", 2);
    map = toolkitMap = toolkit.getMap("myMap", null, null);
    keyValueGenerator = new LiteralKeyLiteralValueGenerator();
  }

  @Override
  protected void checkGetName() throws InterruptedException, BrokenBarrierException {
    setUp();
    try {
      Assert.assertEquals("myMap", toolkitMap.getName());
    } finally {
      tearDown();
    }
  }

  @Override
  protected void checkIsDestroyed() throws InterruptedException, BrokenBarrierException {
    setUp();
    try {
      index = barrier.await();
      if (index == 0) {
        ToolkitMap tmpMap = toolkit.getMap("tempMap", null, null);
        Assert.assertFalse(tmpMap.isDestroyed());
        tmpMap.destroy();
        Assert.assertTrue(tmpMap.isDestroyed());
      }
      barrier.await();
    } finally {
      tearDown();
    }
  }

  @Override
  protected void checkDestroy() throws InterruptedException, BrokenBarrierException {
    setUp();
    try {
      index = barrier.await();
      if (index == 0) {
        ToolkitMap tmpMap = toolkit.getMap("tempMap", null, null);
        Assert.assertFalse(tmpMap.isDestroyed());
        tmpMap.destroy();
        Assert.assertTrue(tmpMap.isDestroyed());
      }
      barrier.await();
    } finally {
      tearDown();
    }
  }

}
