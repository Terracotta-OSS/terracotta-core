/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.map;

import org.terracotta.express.tests.base.AbstractToolkitTestBase;
import org.terracotta.toolkit.store.ToolkitStoreConfigFields.Consistency;

import com.tc.test.config.model.TestConfig;


public class ToolkitStoreIteratorEventualTest extends AbstractToolkitTestBase {

  public ToolkitStoreIteratorEventualTest(TestConfig testConfig) {
    super(testConfig, Client.class, Client.class);
    testConfig.getClientConfig().setMaxHeap(256);
  }

  public static class Client extends AbstractToolkitStoreIteratorClient {

    public Client(String[] args) {
      super(args);
    }

    @Override
    public Consistency getConsistency() {
      return Consistency.EVENTUAL;
    }

  }

}
