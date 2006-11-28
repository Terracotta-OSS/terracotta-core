/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tctest;

public interface TestConfigurator {
  public void doSetUp(TransparentTestIface t) throws Exception;
}
