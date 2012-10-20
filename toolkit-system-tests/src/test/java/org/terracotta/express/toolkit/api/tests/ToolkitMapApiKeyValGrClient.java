/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.toolkit.api.tests;

import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.collections.ToolkitMap;

import com.tc.util.Assert;

import java.util.concurrent.BrokenBarrierException;

public class ToolkitMapApiKeyValGrClient extends AbstractToolkitApiTestClientUtil {
  private ToolkitMap toolkitMap;

  @Override
  protected void test(Toolkit toolKit) throws Throwable {
    this.toolkit = toolKit;
    // this consistency is not used while creating map so pass null here
    setDs(toolkit, NAME_OF_DS, null);
    log("Testing ToolkitMap with LiteralKeyLiteralValueGenerator");
    keyValueGenerator = new LiteralKeyLiteralValueGenerator();
    super.test(toolkit);
    this.test();

    log("Testing ToolkitMap with NonLiteralKeyLiteralValueGenerator");
    keyValueGenerator = new NonLiteralKeyLiteralValueGenerator();
    super.test(toolkit);
    this.test();

    log("Testing ToolkitMap with NonLiteralKeyNonLiteralValueGenerator");
    keyValueGenerator = new NonLiteralKeyNonLiteralValueGenerator();
    super.test(toolkit);
    this.test();

    log("Testing ToolkitMap with LiteralKeyNonLiteralValueGenerator");
    keyValueGenerator = new LiteralKeyNonLiteralValueGenerator();
    super.test();
    this.test();
  }

  @Override
  public void test() throws InterruptedException, BrokenBarrierException {
    checkGetName();
  }

  public ToolkitMapApiKeyValGrClient(String[] args) {
    super(args);
  }

  @Override
  public void setDs(Toolkit toolkit, String name, String strongOrEventualDs) {
    super.toolkit = toolkit;
    barrier = toolkit.getBarrier("myBarrier", 2);
    super.chm = toolkit.getMap(name, String.class, String.class);
    toolkitMap = (ToolkitMap) super.chm;
  }

  @Override
  protected void checkGetName() throws InterruptedException, BrokenBarrierException {
    waitForAllClientsToReachHere();
    try {
      Assert.assertEquals(NAME_OF_DS, toolkitMap.getName());
    } finally {
      clearDs();
    }
  }

}
