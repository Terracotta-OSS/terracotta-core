/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.performance.http.load;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

public class TestProperties {
  static final String    PROPERTIES_FILE   = "http-load-client.properties";
  static final String    PROP_HOSTS        = "hosts";                      // comma delimited
  static final String    PROP_THREADS      = "threads";
  static final String    PROP_STICKY_RATIO = "sticky-ratio";
  static final String    PROP_SESSIONS     = "sessions";

  private final int      stickyRatio;
  private final int      sessionsCount;
  private final String[] hosts;
  private final int      threadCount;

  public TestProperties(File workingDir) {
    this(workingDir.getAbsolutePath());
  }

  public TestProperties(String workingDir) {
    Properties props = new Properties();
    try {
      props.load(new FileInputStream(workingDir + File.separator + PROPERTIES_FILE));
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
    stickyRatio = toInt(props.getProperty(PROP_STICKY_RATIO, "100"), 100);
    sessionsCount = toInt(props.getProperty(PROP_SESSIONS, "100"), 100);
    hosts = props.getProperty(PROP_HOSTS, "http://127.0.0.1").split(",");
    threadCount = toInt(props.getProperty(PROP_THREADS, "50"), 50);
  }

  public String[] getHosts() {
    return hosts;
  }

  public int getSessionsCount() {
    return sessionsCount;
  }

  public int getStickyRatio() {
    return stickyRatio;
  }

  public int getThreadCount() {
    return threadCount;
  }
  
  private static int toInt(String v, int def) {
    try {
      return Integer.parseInt(v);
    } catch (Exception e) {
      return def;
    }
  }
}
