/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.config.schema;

import com.tc.config.schema.NewConfig;

/**
 * Represents the runtime-logging options for DSO.
 */
public interface DSORuntimeLoggingOptions extends NewConfig {

  boolean logLockDebug();

  boolean logFieldChangeDebug();

  boolean logWaitNotifyDebug();

  boolean logDistributedMethodDebug();

  boolean logNewObjectDebug();

  boolean logNonPortableDump();

  boolean logNamedLoaderDebug();

}
