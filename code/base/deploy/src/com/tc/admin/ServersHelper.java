/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin;

import java.net.URL;

import javax.swing.Icon;
import javax.swing.ImageIcon;

public class ServersHelper extends BaseHelper {
  private static ServersHelper m_helper = new ServersHelper();
  private Icon                 m_serverIcon;
  private Icon                 m_connectIcon;
  private Icon                 m_disconnectIcon;
  private Icon                 m_deleteIcon;
  private Icon                 m_shutdownIcon;

  public static final String SERVERS      = "Servers";
  public static final String HOST         = "Host";
  public static final String PORT         = "Port";
  public static final String AUTO_CONNECT = "AutoConnect";
  public static final String SPLIT        = "Split";

  public static ServersHelper getHelper() {
    return m_helper;
  }

  public Icon getServerIcon() {
    if(m_serverIcon == null) {
      URL url = getClass().getResource(ICONS_PATH+"methpro_obj.gif");
      m_serverIcon = new ImageIcon(url);
    }

    return m_serverIcon;
  }

  public Icon getConnectIcon() {
    if(m_connectIcon == null) {
      URL url = getClass().getResource(ICONS_PATH+"newex_wiz.gif");
      m_connectIcon = new ImageIcon(url);
    }

    return m_connectIcon;
  }

  public Icon getDisconnectIcon() {
    if(m_disconnectIcon == null) {
      URL url = getClass().getResource(ICONS_PATH+"disconnect_co.gif");
      m_disconnectIcon = new ImageIcon(url);
    }

    return m_disconnectIcon;
  }

  public Icon getDeleteIcon() {
    if(m_deleteIcon == null) {
      URL url = getClass().getResource(ICONS_PATH+"rem_co.gif");
      m_deleteIcon = new ImageIcon(url);
    }

    return m_deleteIcon;
  }

  public Icon getShutdownIcon() {
    if(m_shutdownIcon == null) {
      URL url = getClass().getResource(ICONS_PATH+"terminate_co.gif");
      m_shutdownIcon = new ImageIcon(url);
    }

    return m_shutdownIcon;
  }
}
