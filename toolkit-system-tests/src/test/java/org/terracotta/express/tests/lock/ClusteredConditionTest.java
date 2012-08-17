/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.lock;

import org.terracotta.express.tests.base.AbstractToolkitTestBase;

import com.tc.test.config.model.TestConfig;



public class ClusteredConditionTest extends AbstractToolkitTestBase {

  public ClusteredConditionTest(TestConfig testConfig) {
    super(testConfig, ClusteredConditionTestClient.class, ClusteredConditionTestClient.class);
  }

}
