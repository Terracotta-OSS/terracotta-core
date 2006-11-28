/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.config.schema;

import com.tc.config.schema.dynamic.ConfigItem;

/**
 * An interface implemented by all config objects.
 */
public interface NewConfig {

  void changesInItemIgnored(ConfigItem item);
  
  void changesInItemForbidden(ConfigItem item);
  
}
