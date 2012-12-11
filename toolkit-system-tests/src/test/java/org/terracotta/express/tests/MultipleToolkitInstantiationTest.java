/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests;

import org.terracotta.express.tests.base.AbstractToolkitTestBase;
import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.toolkit.Toolkit;

import com.tc.test.config.model.TestConfig;

public class MultipleToolkitInstantiationTest extends AbstractToolkitTestBase {

  public MultipleToolkitInstantiationTest(TestConfig testConfig) {
    super(testConfig, App.class);
  }

  public static class App extends ClientBase {

    public App(String[] args) {
      super(args);
    }

    @Override
    protected void test(Toolkit toolkit) throws Throwable {
      createToolkit();
      createToolkit();
      createToolkit();
      createToolkit();
    }

    @Override
    protected String getTerracottaTypeSubType() {
      return "toolkit:nonstop-terracotta://";
    }

  }

}
