/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.system.tests;

import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.store.ToolkitStore;

import java.io.Serializable;

import junit.framework.Assert;

public class ClientTerminatingTestCoordinator extends ClientBase {
  private final ToolkitStore<String, Serializable> map;

  public ClientTerminatingTestCoordinator(String[] args) {
    super(args);
    this.map = getClusteringToolkit().getStore("testMap", null);
  }

  @Override
  protected void test(Toolkit toolkit) throws Throwable {
    Assert.assertEquals(ForceTerminatingTestClient.NUM_OF_ELEMENTS, this.map.size());
    for (int i = 0; i < ForceTerminatingTestClient.NUM_OF_ELEMENTS; i++) {
      Assert.assertTrue(this.map.keySet().contains(String.valueOf(i)));
    }
  }

}
