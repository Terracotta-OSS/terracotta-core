/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import com.tc.admin.model.IServer;
import com.tc.management.beans.L2MBeanNames;

import java.awt.Color;

public class ServerHelper extends BaseHelper {
  private static final ServerHelper helper = new ServerHelper();

  private ServerHelper() {/**/}
  
  public static ServerHelper getHelper() {
    return helper;
  }

  public boolean canShutdown(ConnectionContext cc) throws Exception {
    return cc.getBooleanAttribute(L2MBeanNames.TC_SERVER_INFO, "Shutdownable");
  }

  public boolean isActive(ConnectionContext cc) throws Exception {
    return cc.getBooleanAttribute(L2MBeanNames.TC_SERVER_INFO, "Active");
  }

  public boolean isStarted(ConnectionContext cc) throws Exception {
    return cc.getBooleanAttribute(L2MBeanNames.TC_SERVER_INFO, "Started");
  }

  public boolean isPassiveUninitialized(ConnectionContext cc) throws Exception {
    return cc.getBooleanAttribute(L2MBeanNames.TC_SERVER_INFO, "PassiveUninitialized");
  }

  public boolean isPassiveStandby(ConnectionContext cc) throws Exception {
    return cc.getBooleanAttribute(L2MBeanNames.TC_SERVER_INFO, "PassiveStandby");
  }

  public Color getServerStatusColor(IServer server) {
    if (server != null) {
      if (server.isActive()) {
        return Color.GREEN;
      } else if (server.isPassiveStandby()) {
        return Color.CYAN;
      } else if (server.isPassiveUninitialized()) {
        return Color.ORANGE;
      } else if (server.isStarted()) {
        return Color.YELLOW;
      } else if (server.hasConnectError()) { return Color.RED; }
    }
    return Color.LIGHT_GRAY;
  }

}
