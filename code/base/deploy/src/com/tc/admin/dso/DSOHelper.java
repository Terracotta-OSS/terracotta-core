/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.BaseHelper;
import com.tc.admin.ConnectionContext;
import com.tc.admin.common.MBeanServerInvocationProxy;
import com.tc.management.beans.L2MBeanNames;
import com.tc.management.beans.object.ObjectManagementMonitorMBean;
import com.tc.object.ObjectID;
import com.tc.objectserver.mgmt.ManagedObjectFacade;
import com.tc.stats.DSOMBean;

import java.io.IOException;
import java.net.URL;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.swing.Icon;
import javax.swing.ImageIcon;

public class DSOHelper extends BaseHelper {
  private static DSOHelper m_helper = new DSOHelper();
  private Icon             m_dsoIcon;
  private Icon             m_gcIcon;

  public static DSOHelper getHelper() {
    return m_helper;
  }

  public Icon getDSOIcon() {
    if (m_dsoIcon == null) {
      URL url = getClass().getResource(ICONS_PATH + "search_menu.gif");

      if (url != null) {
        m_dsoIcon = new ImageIcon(url);
      }
    }

    return m_dsoIcon;
  }

  public Icon getGCIcon() {
    if (m_gcIcon == null) {
      URL url = getClass().getResource(ICONS_PATH + "trash.gif");

      if (url != null) {
        m_gcIcon = new ImageIcon(url);
      }
    }

    return m_gcIcon;
  }

  public DSOMBean getDSOBean(ConnectionContext cc) {
    return (DSOMBean) MBeanServerInvocationProxy.newProxyInstance(cc.mbsc, L2MBeanNames.DSO, DSOMBean.class, false);
  }

  public ObjectName getDSOMBean(ConnectionContext cc) throws IOException, MalformedObjectNameException {
    return cc.queryName(L2MBeanNames.DSO.getCanonicalName());
  }

  public ObjectManagementMonitorMBean getObjectManagementMonitorBean(ConnectionContext cc) {
    return (ObjectManagementMonitorMBean) MBeanServerInvocationProxy
        .newProxyInstance(cc.mbsc, L2MBeanNames.OBJECT_MANAGEMENT, ObjectManagementMonitorMBean.class, false);
  }

  public ManagedObjectFacade lookupFacade(ConnectionContext cc, ObjectID objectID, int batchSize) throws Exception {
    ObjectName bean = getDSOMBean(cc);
    String op = "lookupFacade";
    Object[] args = new Object[] { objectID, Integer.valueOf(batchSize) };
    String[] types = new String[] { "com.tc.object.ObjectID", "int" };

    return (ManagedObjectFacade) cc.invoke(bean, op, args, types);
  }
}
