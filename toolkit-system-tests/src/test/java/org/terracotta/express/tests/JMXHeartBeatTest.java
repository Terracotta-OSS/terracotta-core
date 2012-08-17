/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests;


import org.terracotta.express.tests.base.AbstractToolkitTestBase;

import com.tc.test.config.model.TestConfig;

public class JMXHeartBeatTest extends AbstractToolkitTestBase {

  public JMXHeartBeatTest(TestConfig testConfig) {
    super(testConfig, JMXHeartBeatTestClient.class);
  }

}
