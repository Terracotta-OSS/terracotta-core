/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.config.schema;

import com.tc.config.schema.Config;

/**
 * Represents the instrumentation-logging options for DSO.
 */
public interface DSOInstrumentationLoggingOptions extends Config {

  boolean logClass();

  boolean logLocks();

  boolean logTransientRoot();

  boolean logRoots();

  boolean logDistributedMethods();
  
  void setLogDistributedMethods(boolean val);

}
