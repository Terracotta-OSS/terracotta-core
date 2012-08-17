package org.terracotta.system.tests;

import org.terracotta.express.tests.base.AbstractToolkitTestBase;

import com.tc.test.config.model.TestConfig;

public class ToolkitSameNameMultipleTypesTest extends AbstractToolkitTestBase {
  public ToolkitSameNameMultipleTypesTest(TestConfig testConfig) {
    super(testConfig, ToolkitSameNameMultipleTypesTestClient.class);

  }
}
