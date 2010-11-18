/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.config.schema;

import com.tc.config.schema.NewConfig;
import com.terracottatech.config.AdditionalBootJarClasses;
import com.terracottatech.config.TransientFields;
import com.terracottatech.config.WebApplications;

/**
 * Represents the per-application config for DSO.
 */
public interface NewDSOApplicationConfig extends NewConfig {

  InstrumentedClass[] instrumentedClasses();

  TransientFields transientFields();

  Lock[] locks();

  Root[] roots();

  AdditionalBootJarClasses additionalBootJarClasses();

  boolean supportSharingThroughReflection();

  WebApplications webApplications();

}
