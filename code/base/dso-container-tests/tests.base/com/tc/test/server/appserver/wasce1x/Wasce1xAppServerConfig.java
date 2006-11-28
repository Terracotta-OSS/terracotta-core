/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.test.server.appserver.wasce1x;

import com.tc.test.server.tcconfig.StandardTerracottaAppServerConfig;

import java.io.File;

/**
 * Used to set appserver specific terracotta config elements such as excludes.
 */
public class Wasce1xAppServerConfig extends StandardTerracottaAppServerConfig {

  public Wasce1xAppServerConfig(File baseDir) {
    super(baseDir);
  }
}
