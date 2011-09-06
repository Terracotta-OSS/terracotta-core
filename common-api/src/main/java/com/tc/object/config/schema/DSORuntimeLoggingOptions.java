/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.config.schema;

import com.tc.config.schema.Config;

/**
 * Represents the runtime-logging options for DSO.
 */
public interface DSORuntimeLoggingOptions extends Config {

  boolean logLockDebug();

  boolean logFieldChangeDebug();

  boolean logWaitNotifyDebug();

  boolean logDistributedMethodDebug();

  boolean logNewObjectDebug();

  boolean logNonPortableDump();

  boolean logNamedLoaderDebug();

}
