/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.test.server.dsoserver;

import com.tc.test.server.Server;

import java.util.List;

/**
 * DSO specific server interface.
 */
public interface DsoServer extends Server {
  // available for future feature enhancements
  
  void addJvmArgs(List jvmArgs);
  
  boolean isRunning();
}
