/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.toolkit;

import org.terracotta.express.tests.base.AbstractToolkitTestBase;
import org.terracotta.test.util.TestBaseUtil;

import com.tc.test.config.model.TestConfig;
import com.terracotta.toolkit.TerracottaToolkit;

import java.util.Collections;
import java.util.List;

public class WrongJarsInClasspathOSTest extends AbstractToolkitTestBase {

  public WrongJarsInClasspathOSTest(TestConfig testConfig) {
    super(testConfig, WrongJarsInClasspathTestClient.class);
  }

  @Override
  protected List<String> getExtraJars() {
    return Collections.singletonList(TestBaseUtil.jarFor(TerracottaToolkit.class));
  }

}
