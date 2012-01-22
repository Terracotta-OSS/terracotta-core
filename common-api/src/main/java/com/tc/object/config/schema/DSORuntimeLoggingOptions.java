/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
