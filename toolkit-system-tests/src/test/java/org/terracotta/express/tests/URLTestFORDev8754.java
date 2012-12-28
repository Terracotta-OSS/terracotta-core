/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests;

import org.terracotta.express.tests.base.AbstractToolkitTestBase;
import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.ToolkitFactory;
import org.terracotta.toolkit.ToolkitInstantiationException;
import org.terracotta.toolkit.internal.ToolkitInternal;

import com.tc.test.config.model.TestConfig;

public class URLTestFORDev8754 extends AbstractToolkitTestBase {

  public URLTestFORDev8754(TestConfig testConfig) {
    super(testConfig, URLTestFORDev8754Client.class);
  }

  public static class URLTestFORDev8754Client extends ClientBase {

    private static final String COMMA = ",";

    public URLTestFORDev8754Client(String[] args) {
      super(args);
    }

    protected ToolkitInternal createToolkitWithCommaInEnd() {
      try {
        return (ToolkitInternal) ToolkitFactory.createToolkit(getTerracottaTypeSubType() + getTerracottaUrl() + COMMA);
      } catch (ToolkitInstantiationException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    protected void test(Toolkit toolkit) throws Throwable {
      debug("Trying to create Toolkit");
      ToolkitInternal tk = createToolkitWithCommaInEnd();
      tk = createToolkitWithTwoCommasInMiddle();
    }

    private ToolkitInternal createToolkitWithTwoCommasInMiddle() {
      try {
        return (ToolkitInternal) ToolkitFactory.createToolkit(getTerracottaTypeSubType() + getTerracottaUrl() + COMMA
                                                              + COMMA + COMMA + getTerracottaUrl() + COMMA + COMMA
                                                              + COMMA + COMMA + getTerracottaUrl() + COMMA + COMMA);
      } catch (ToolkitInstantiationException e) {
        throw new RuntimeException(e);
      }
    }

  }
}
