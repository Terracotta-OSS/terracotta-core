/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import com.tc.management.beans.L2MBeanNames;

public class ServerHelper extends BaseHelper {
  private static ServerHelper m_helper = new ServerHelper();

  public static ServerHelper getHelper() {
    return m_helper;
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
}
