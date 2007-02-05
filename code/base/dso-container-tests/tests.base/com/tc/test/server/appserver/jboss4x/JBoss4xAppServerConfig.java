/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.test.server.appserver.jboss4x;

import com.tc.test.server.tcconfig.StandardTerracottaAppServerConfig;

import java.io.File;

/**
 * Used to set appserver specific terracotta config elements such as excludes.
 */
public class JBoss4xAppServerConfig extends StandardTerracottaAppServerConfig {

  public JBoss4xAppServerConfig(File baseDir) {
    super(baseDir);
  }
}
