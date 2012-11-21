/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.rejoin;

import com.tc.test.config.model.TestConfig;

public class ToolkitSetRejoinTest extends AbstractToolkitRejoinTest {

  public ToolkitSetRejoinTest(TestConfig testConfig) {
    super(testConfig, ToolkitSetRejoinTestClient.class);
  }

}
