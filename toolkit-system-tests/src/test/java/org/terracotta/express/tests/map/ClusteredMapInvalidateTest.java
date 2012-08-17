/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.map;

import org.terracotta.express.tests.base.AbstractToolkitTestBase;

import com.tc.test.config.model.TestConfig;

public class ClusteredMapInvalidateTest extends AbstractToolkitTestBase {

  public ClusteredMapInvalidateTest(TestConfig testConfig) {
    super(testConfig, ClusteredMapInvalidateTestClient.class,
          ClusteredMapInvalidateTestClient.class);
  }

}
