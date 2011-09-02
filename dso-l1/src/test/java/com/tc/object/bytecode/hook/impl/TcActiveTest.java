/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode.hook.impl;

import junit.framework.TestCase;

public class TcActiveTest extends TestCase {
  public void tests() throws Exception {
    assertTrue(Boolean.getBoolean("tc.active"));
  }
}