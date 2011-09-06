/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import java.net.URL;

import javax.swing.Icon;
import javax.swing.ImageIcon;

public class ServersHelper extends BaseHelper {
  private static final ServersHelper helper       = new ServersHelper();
  private Icon                       serverIcon;
  private Icon                       serversIcon;
  private Icon                       connectIcon;
  private Icon                       disconnectIcon;
  private Icon                       deleteIcon;
  private Icon                       backupIcon;
  private Icon                       shutdownIcon;
  private Icon                       activityIcon;

  public static final String         SERVERS      = "Servers";
  public static final String         NAME         = "Name";
  public static final String         HOST         = "Host";
  public static final String         PORT         = "Port";
  public static final String         AUTO_CONNECT = "AutoConnect";
  public static final String         SPLIT        = "Split";

  private ServersHelper() {/**/
  }

  public static ServersHelper getHelper() {
    return helper;
  }

  public Icon getServersIcon() {
    if (serversIcon == null) {
      URL url = getClass().getResource(ICONS_PATH + "methpro_obj.gif");
      serversIcon = new ImageIcon(url);
    }
    return serverIcon;
  }

  public Icon getServerIcon() {
    if (serverIcon == null) {
      URL url = getClass().getResource(ICONS_PATH + "methpro_obj.gif");
      serverIcon = new ImageIcon(url);
    }
    return serverIcon;
  }

  public Icon getConnectIcon() {
    if (connectIcon == null) {
      URL url = getClass().getResource(ICONS_PATH + "newex_wiz.gif");
      connectIcon = new ImageIcon(url);
    }
    return connectIcon;
  }

  public Icon getDisconnectIcon() {
    if (disconnectIcon == null) {
      URL url = getClass().getResource(ICONS_PATH + "disconnect_co.gif");
      disconnectIcon = new ImageIcon(url);
    }
    return disconnectIcon;
  }

  public Icon getDeleteIcon() {
    if (deleteIcon == null) {
      URL url = getClass().getResource(ICONS_PATH + "rem_co.gif");
      deleteIcon = new ImageIcon(url);
    }
    return deleteIcon;
  }

  public Icon getBackupIcon() {
    if (backupIcon == null) {
      URL url = getClass().getResource(ICONS_PATH + "database_save.png");
      backupIcon = new ImageIcon(url);
    }
    return backupIcon;
  }

  public Icon getShutdownIcon() {
    if (shutdownIcon == null) {
      URL url = getClass().getResource(ICONS_PATH + "terminate_co.gif");
      shutdownIcon = new ImageIcon(url);
    }
    return shutdownIcon;
  }

  public Icon getActivityIcon() {
    if (activityIcon == null) {
      URL url = getClass().getResource(ICONS_PATH + "methpri_obj.gif");
      activityIcon = new ImageIcon(url);
    }
    return activityIcon;
  }

}
