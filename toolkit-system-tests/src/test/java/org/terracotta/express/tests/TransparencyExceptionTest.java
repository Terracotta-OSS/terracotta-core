/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests;


import org.terracotta.express.tests.base.AbstractToolkitTestBase;

import com.tc.test.config.model.TestConfig;

/**
 * Test what happens when an exception is thrown in a locked method.
 */
public class TransparencyExceptionTest extends AbstractToolkitTestBase {

  public TransparencyExceptionTest(TestConfig testConfig) {
    super(testConfig, TransparencyExceptionTestApp.class, TransparencyExceptionTestApp.class);
  }

}
