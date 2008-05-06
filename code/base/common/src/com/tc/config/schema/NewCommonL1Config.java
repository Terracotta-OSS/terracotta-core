/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema;

import com.tc.config.schema.dynamic.FileConfigItem;
import com.terracottatech.config.Modules;

/**
 * Contains methods for L1 DSO.
 */
public interface NewCommonL1Config extends NewConfig, NewStatisticsConfig {

  FileConfigItem logsPath();

  Modules modules();

}
