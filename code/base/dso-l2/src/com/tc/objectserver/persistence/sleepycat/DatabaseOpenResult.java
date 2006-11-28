/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

public class DatabaseOpenResult {
  private final boolean clean;
  
  DatabaseOpenResult(boolean clean) {
    this.clean = clean;
  }
  
  public boolean isClean() {
    return this.clean;
  }
  
}
