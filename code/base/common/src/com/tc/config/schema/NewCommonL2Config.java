/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema;

import com.tc.config.schema.dynamic.FileConfigItem;
import com.tc.config.schema.dynamic.IntConfigItem;
import com.tc.config.schema.dynamic.StringConfigItem;

/**
 * Contains methods exposing DSO L2 config.
 */
public interface NewCommonL2Config extends NewConfig {

  FileConfigItem dataPath();
  
  FileConfigItem logsPath();
  
  IntConfigItem jmxPort();
  
  StringConfigItem host();

  boolean authentication();
  
  String authenticationPasswordFile();
  
  String authenticationAccessFile();
}
