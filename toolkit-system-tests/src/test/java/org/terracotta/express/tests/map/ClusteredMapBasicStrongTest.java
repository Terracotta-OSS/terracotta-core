/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.map;

import org.terracotta.express.tests.base.AbstractToolkitTestBase;

import com.tc.test.config.model.TestConfig;

public class ClusteredMapBasicStrongTest extends AbstractToolkitTestBase {

  public ClusteredMapBasicStrongTest(TestConfig testConfig) {

    super(testConfig, ClusteredMapBasicStrongTestClient.class, ClusteredMapBasicStrongTestClient.class);
  }

}
