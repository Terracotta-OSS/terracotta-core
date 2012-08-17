/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests;

import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.toolkit.Toolkit;

public class CustomLoggingClient extends ClientBase {

  public CustomLoggingClient(String[] args) {
    super(args);
  }

  public static void main(String[] args) {
    new CustomLoggingClient(args).run();
  }

  @Override
  protected void test(Toolkit toolkit) throws Throwable {
    toolkit.getMap("foo", null, null).put("key", "value");
  }

}
