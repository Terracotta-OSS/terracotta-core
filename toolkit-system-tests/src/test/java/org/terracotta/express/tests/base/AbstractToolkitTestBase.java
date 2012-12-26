/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.base;

import org.mockito.Mockito;
import org.terracotta.test.util.TestBaseUtil;
import org.terracotta.tests.base.AbstractClientBase;
import org.terracotta.tests.base.AbstractTestBase;
import org.terracotta.toolkit.ToolkitFactory;

import com.tc.management.beans.L2MBeanNames;
import com.tc.test.config.model.TestConfig;

public abstract class AbstractToolkitTestBase extends AbstractTestBase {

  public AbstractToolkitTestBase(TestConfig testConfig, Class<? extends AbstractClientBase>... c) {
    super(testConfig);
    testConfig.getClientConfig().setClientClasses(c);
  }

  @Override
  protected String createClassPath(Class client) {
    String expressRuntime = TestBaseUtil.jarFor(ToolkitFactory.class);
    String clientBase = TestBaseUtil.jarFor(ClientBase.class);
    String l2Mbean = TestBaseUtil.jarFor(L2MBeanNames.class);
    String mockito = TestBaseUtil.jarFor(Mockito.class);
    return makeClasspath(expressRuntime, clientBase, l2Mbean, mockito);
  }
}
