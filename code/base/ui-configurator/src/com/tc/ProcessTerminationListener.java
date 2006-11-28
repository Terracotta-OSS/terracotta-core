/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc;

public interface ProcessTerminationListener {
  void processTerminated(int exitCode);
}
