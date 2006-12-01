/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.dynamic;

import java.io.File;

/**
 * A {@link ConfigItem} that also exposes its value as a {@link File}.
 */
public interface FileConfigItem extends ConfigItem {

  File getFile();
  
}
