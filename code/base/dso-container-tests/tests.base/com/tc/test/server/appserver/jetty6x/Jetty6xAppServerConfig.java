/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.test.server.appserver.jetty6x;

import com.tc.test.server.tcconfig.StandardTerracottaAppServerConfig;

import java.io.File;

/**
 * Used to set appserver specific terracotta config elements such as excludes.
 */
public class Jetty6xAppServerConfig extends StandardTerracottaAppServerConfig {

  public Jetty6xAppServerConfig(File baseDir) {
    super(baseDir);
  }
}
