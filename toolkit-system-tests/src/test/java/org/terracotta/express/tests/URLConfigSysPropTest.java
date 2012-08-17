/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests;


import org.terracotta.express.tests.base.AbstractToolkitTestBase;

import com.tc.test.config.model.TestConfig;

public class URLConfigSysPropTest extends AbstractToolkitTestBase {

  public URLConfigSysPropTest(TestConfig testConfig) {
    super(testConfig, URLConfigClient.class);
  }

}
