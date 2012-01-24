/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.config.schema;

import com.tc.config.schema.Config;

/**
 * Represents the per-application config for DSO.
 */
public interface DSOApplicationConfig extends Config {

  InstrumentedClass[] instrumentedClasses();

  Lock[] locks();

  Root[] roots();

  boolean supportSharingThroughReflection();

}
