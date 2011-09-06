/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

public interface TestConfigurator {
  public void doSetUp(TransparentTestIface t) throws Exception;
}
