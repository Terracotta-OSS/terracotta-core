/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.toolkit.api.tests;

import org.terracotta.express.tests.base.AbstractToolkitTestBase;

import com.tc.test.config.model.TestConfig;

public class ToolkitMapApiTestWithKeyValueGenerator extends AbstractToolkitTestBase {
  public ToolkitMapApiTestWithKeyValueGenerator(TestConfig testConfig) {
    super(testConfig, ToolkitMapApiKeyValGrClient.class, ToolkitMapApiKeyValGrClient.class);
  }
}
