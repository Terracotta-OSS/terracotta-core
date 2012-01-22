/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.config.schema;

import com.tc.config.schema.Config;
import com.terracottatech.config.AdditionalBootJarClasses;
import com.terracottatech.config.TransientFields;
import com.terracottatech.config.WebApplications;

/**
 * Represents the per-application config for DSO.
 */
public interface DSOApplicationConfig extends Config {

  InstrumentedClass[] instrumentedClasses();

  TransientFields transientFields();

  Lock[] locks();

  Root[] roots();

  AdditionalBootJarClasses additionalBootJarClasses();

  boolean supportSharingThroughReflection();

  WebApplications webApplications();

}
