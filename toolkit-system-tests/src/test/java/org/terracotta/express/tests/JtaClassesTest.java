/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests;

import org.terracotta.express.tests.base.AbstractToolkitTestBase;
import org.terracotta.test.util.TestBaseUtil;

import com.tc.test.config.model.TestConfig;

import java.util.Collections;
import java.util.List;

import javax.transaction.Transaction;

public class JtaClassesTest extends AbstractToolkitTestBase {

  public JtaClassesTest(TestConfig testConfig) {
    super(testConfig, JtaClassesClient.class);
  }

  @Override
  protected List<String> getExtraJars() {
    return Collections.singletonList(TestBaseUtil.jarFor(Transaction.class));
  }
}
