/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.server.appserver.deployment;

import junit.extensions.TestSetup;
import junit.framework.Test;

public class ErrorTestSetup extends TestSetup {

  public ErrorTestSetup(Test test) {
    super(test);
  }

  public void setUp() {
    throw new RuntimeException("Container test needs to have TestSetup for proper cleanup!");
  }
}
