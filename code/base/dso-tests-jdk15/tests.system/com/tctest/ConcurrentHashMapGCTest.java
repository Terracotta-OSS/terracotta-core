/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tctest;

public class ConcurrentHashMapGCTest extends GCTestBase implements TestConfigurator {
  protected Class getApplicationClass() {
    return ConcurrentHashMapSwapingTestApp.class;
  }

}
