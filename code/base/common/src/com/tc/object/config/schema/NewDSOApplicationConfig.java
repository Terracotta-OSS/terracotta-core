/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.config.schema;

import com.tc.config.schema.NewConfig;
import com.tc.config.schema.dynamic.BooleanConfigItem;
import com.tc.config.schema.dynamic.ConfigItem;
import com.tc.config.schema.dynamic.StringArrayConfigItem;

/**
 * Represents the per-application config for DSO.
 */
public interface NewDSOApplicationConfig extends NewConfig {

  ConfigItem instrumentedClasses();

  StringArrayConfigItem transientFields();

  ConfigItem locks();

  ConfigItem roots();

  StringArrayConfigItem additionalBootJarClasses();

  BooleanConfigItem supportSharingThroughReflection();

  StringArrayConfigItem webApplications();

}
