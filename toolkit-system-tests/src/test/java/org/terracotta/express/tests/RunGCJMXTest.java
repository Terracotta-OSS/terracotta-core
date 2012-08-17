/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests;


import org.terracotta.express.tests.base.AbstractToolkitTestBase;

import com.tc.test.config.model.TestConfig;

public class RunGCJMXTest extends AbstractToolkitTestBase {

  public RunGCJMXTest(TestConfig testConfig) {
    super(testConfig, RunGcJMXTestApp.class, RunGcJMXTestApp.class, RunGcJMXTestApp.class);

  }

}
