/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.textbucket;

import org.terracotta.express.tests.base.AbstractToolkitTestBase;

import com.tc.test.config.model.TestConfig;

public class TextBucketTest extends AbstractToolkitTestBase {

  public TextBucketTest(TestConfig testConfig) {
    super(testConfig);
    testConfig.getClientConfig().setClientClasses(TextBucketClient.class, TextBucketClient.NODE_COUNT);
  }

}
