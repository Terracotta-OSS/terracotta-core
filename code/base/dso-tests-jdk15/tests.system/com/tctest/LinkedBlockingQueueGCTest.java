/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tctest;


public class LinkedBlockingQueueGCTest extends GCTestBase implements TestConfigurator {

  protected Class getApplicationClass() {
    return LinkedBlockingQueueTestApp.class;
  }

}
