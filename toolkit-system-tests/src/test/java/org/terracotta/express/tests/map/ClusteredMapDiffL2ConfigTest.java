/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.map;

import org.terracotta.express.tests.base.AbstractToolkitTestBase;

import com.tc.test.config.model.TestConfig;

public class ClusteredMapDiffL2ConfigTest extends AbstractToolkitTestBase {
  public ClusteredMapDiffL2ConfigTest(TestConfig testConfig) {
    super(testConfig, ClusteredMapDiffL2ConfigTestClient.class, ClusteredMapDiffL2ConfigTestClient.class);
  }
}
