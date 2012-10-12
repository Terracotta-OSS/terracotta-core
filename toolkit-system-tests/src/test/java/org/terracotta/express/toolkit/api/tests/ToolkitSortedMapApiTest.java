/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.toolkit.api.tests;

import org.terracotta.express.tests.base.AbstractToolkitTestBase;

import com.tc.test.config.model.TestConfig;

public class ToolkitSortedMapApiTest extends AbstractToolkitTestBase {
  public ToolkitSortedMapApiTest(TestConfig testConfig) {
    super(testConfig, ToolkitSortedMapApiClient.class, ToolkitSortedMapApiClient.class);
  }
}
