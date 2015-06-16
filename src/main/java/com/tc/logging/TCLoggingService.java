/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.logging;

import java.io.File;

public interface TCLoggingService {
  int PROCESS_TYPE_GENERIC = 0;
  int PROCESS_TYPE_L1      = 1;
  int PROCESS_TYPE_L2      = 2;

  TCLogger getLogger(String name);

  TCLogger getLogger(Class<?> c);

  void setLogDirectory(File theDirectory, int processType);
}
