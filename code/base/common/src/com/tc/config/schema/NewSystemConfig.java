/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.config.schema;

import com.tc.config.schema.dynamic.ConfigItem;

/**
 * Contains methods that expose whole-system config.
 */
public interface NewSystemConfig extends NewConfig {

  ConfigItem configurationModel();

}
