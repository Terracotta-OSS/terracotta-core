/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.config.schema.dynamic;

import java.io.File;

/**
 * A {@link ConfigItem} that also exposes its value as a {@link File}.
 */
public interface FileConfigItem extends ConfigItem {

  File getFile();
  
}
