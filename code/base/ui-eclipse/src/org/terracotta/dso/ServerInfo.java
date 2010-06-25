/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso;

import org.eclipse.jdt.core.IJavaProject;

import com.tc.util.Assert;
import com.terracottatech.config.Server;

/**
 * When an L2 is started, one of these is associated with the resulting IProcess via the m_servers map.
 */

public class ServerInfo {
  private IJavaProject    m_javaProject;
  private String          m_name;
  private Server          m_server;
  private int             m_status;

  public static final int STARTING   = 0;
  public static final int STARTED    = 1;
  public static final int TERMINATED = 2;

  public ServerInfo(IJavaProject javaProject, String name, Server server) {
    m_javaProject = javaProject;
    m_name = name;
    m_server = server;
    m_status = STARTING;
  }

  public IJavaProject getJavaProject() {
    return m_javaProject;
  }

  public String getName() {
    return m_name;
  }

  public Server getServer() {
    return m_server;
  }
  
  public String getHost() {
    return m_server.getHost();
  }

  public int getJmxPort() {
    return m_server.getJmxPort().getIntValue();
  }

  public int getStatus() {
    return m_status;
  }

  public void setStatus(int status) {
    Assert.assertTrue("Invalid status", status == STARTING || status == STARTED || status == TERMINATED);
    m_status = status;
  }

  public boolean isStarting() {
    return getStatus() == STARTING;
  }

  public boolean isStarted() {
    return getStatus() == STARTED;
  }

  public boolean isTerminated() {
    return getStatus() == TERMINATED;
  }

  public String toString() {
    return getName() + ":" + getJmxPort();
  }
}
