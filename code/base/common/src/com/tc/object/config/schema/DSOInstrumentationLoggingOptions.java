/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.config.schema;

import com.tc.config.schema.NewConfig;
import com.tc.config.schema.dynamic.BooleanConfigItem;

/**
 * Represents the instrumentation-logging options for DSO.
 */
public interface DSOInstrumentationLoggingOptions extends NewConfig {

  BooleanConfigItem logClass();

  BooleanConfigItem logHierarchy();

  BooleanConfigItem logLocks();

  BooleanConfigItem logTransientRoot();

  BooleanConfigItem logRoots();

  BooleanConfigItem logDistributedMethods();

}
