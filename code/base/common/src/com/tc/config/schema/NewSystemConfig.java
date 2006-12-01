/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema;

import com.tc.config.schema.dynamic.ConfigItem;

/**
 * Contains methods that expose whole-system config.
 */
public interface NewSystemConfig extends NewConfig {

  ConfigItem configurationModel();

}
