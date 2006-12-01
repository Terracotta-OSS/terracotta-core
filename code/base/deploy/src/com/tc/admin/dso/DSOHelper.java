/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.BaseHelper;
import com.tc.admin.ConnectionContext;
import com.tc.management.beans.L2MBeanNames;
import com.tc.object.ObjectID;
import com.tc.objectserver.api.GCStats;
import com.tc.objectserver.mgmt.ManagedObjectFacade;

import java.net.URL;

import javax.management.ObjectName;
import javax.swing.Icon;
import javax.swing.ImageIcon;

public class DSOHelper extends BaseHelper {
  private static DSOHelper m_helper = new DSOHelper();
  private Icon             m_dsoIcon;

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

  public ObjectName getDSOMBean(ConnectionContext cc) {
    try {
      return cc.queryName(L2MBeanNames.DSO.getCanonicalName());
    } catch (Exception e) {/**/
    }

    return null;
  }

  public ManagedObjectFacade lookupFacade(ConnectionContext cc, ObjectID objectID, int batchSize) throws Exception {
    ObjectName bean = getDSOMBean(cc);
    String op = "lookupFacade";
    Object[] args = new Object[] { objectID, new Integer(batchSize) };
    String[] types = new String[] { "com.tc.object.ObjectID", "int" };

    return (ManagedObjectFacade) cc.invoke(bean, op, args, types);
  }

  public GCStats[] getGCStats(ConnectionContext cc) throws Exception {
    ObjectName bean = getDSOMBean(cc);
    String attr = "GarbageCollectorStats";

    return (GCStats[]) cc.getAttribute(bean, attr);
  }
}
