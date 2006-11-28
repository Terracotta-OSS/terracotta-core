/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.config.schema;

import com.tc.config.schema.dynamic.FileConfigItem;

/**
 * Contains methods for L1 DSO.
 */
public interface NewCommonL1Config extends NewConfig {

  FileConfigItem logsPath();
  
}
