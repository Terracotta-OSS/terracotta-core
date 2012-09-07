/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.toolkit;

import com.tc.test.TCTestCase;

public class ToolkitRuntimePackagingTest extends TCTestCase {

  public void test() throws Exception {
    ToolkitRuntimePackagingTestHelper.doTest(getTempDirectory());
  }

}
