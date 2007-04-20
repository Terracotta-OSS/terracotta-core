/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin;

import com.tc.management.beans.L2MBeanNames;

import javax.management.ObjectName;

public class ServerHelper extends BaseHelper {
  private static ServerHelper m_helper = new ServerHelper();

  public static ServerHelper getHelper() {
    return m_helper;
  }

  public ObjectName getServerInfoMBean(ConnectionContext cc) throws Exception {
    return cc.queryName(L2MBeanNames.TC_SERVER_INFO.getCanonicalName());
  }

  public boolean isActive(ConnectionContext cc) throws Exception {
    ObjectName infoMBean = getServerInfoMBean(cc);
    return infoMBean != null && cc.getBooleanAttribute(infoMBean, "Active");
  }

  public boolean isStarted(ConnectionContext cc) throws Exception {
    ObjectName infoMBean = getServerInfoMBean(cc);
    return infoMBean != null && cc.getBooleanAttribute(infoMBean, "Started");
  }
  
  public boolean isPassiveUninitialized(ConnectionContext cc) throws Exception {
    ObjectName infoMBean = getServerInfoMBean(cc);
    return infoMBean != null && cc.getBooleanAttribute(infoMBean, "PassiveUninitialized");
  }

  public boolean isPassiveStandby(ConnectionContext cc) throws Exception {
    ObjectName infoMBean = getServerInfoMBean(cc);
    return infoMBean != null && cc.getBooleanAttribute(infoMBean, "PassiveStandby");
  }
}
