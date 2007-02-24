/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema;

import com.tc.config.schema.dynamic.FileConfigItem;
import com.terracottatech.config.Plugins;

/**
 * Contains methods for L1 DSO.
 */
public interface NewCommonL1Config extends NewConfig {

  FileConfigItem logsPath();

  Plugins plugins();

}
