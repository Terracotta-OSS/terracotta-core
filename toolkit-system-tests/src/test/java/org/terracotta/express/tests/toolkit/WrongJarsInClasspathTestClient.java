/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.toolkit;

import org.terracotta.tests.base.AbstractClientBase;
import org.terracotta.toolkit.InvalidToolkitConfigException;
import org.terracotta.toolkit.ToolkitFactory;

import junit.framework.Assert;

public class WrongJarsInClasspathTestClient extends AbstractClientBase {
  private static final String TOOLKIT_IMPL_CLASS_NAME = "com.terracotta.toolkit.TerracottaToolkit";

  public WrongJarsInClasspathTestClient(String[] args) {
    super(args);
  }

  @Override
  protected void doTest() throws Throwable {
    final String toolkitURI = "toolkit:terracotta://" + getTerracottaUrl();
    debug("Creating toolkit with uri: " + toolkitURI);
    try {
      ToolkitFactory.createToolkit(toolkitURI);
      Assert.fail("Should have failed with exception");
    } catch (InvalidToolkitConfigException e) {
      if (e.getMessage().contains("Wrong jars") && e.getMessage().contains("toolkit-impl")
          && e.getMessage().contains(TOOLKIT_IMPL_CLASS_NAME)) {
        // expected exception
        System.out.println("Caught expected exception: " + e);
      } else {
        throw e;
      }
    }
  }

}