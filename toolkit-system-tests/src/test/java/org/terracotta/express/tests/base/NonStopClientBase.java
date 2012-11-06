/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.base;

public abstract class NonStopClientBase extends ClientBase {
  public NonStopClientBase(String args[]) {
    super(args);
  }

  @Override
  protected String getTerracottaTypeSubType() {
    return "toolkit:nonstop-terracotta://";
  }

}
