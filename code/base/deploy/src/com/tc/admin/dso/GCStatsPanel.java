/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.dso;

import org.dijon.ContainerResource;

import com.tc.admin.AdminClient;
import com.tc.admin.AdminClientContext;
import com.tc.admin.ConnectionContext;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XObjectTable;
import com.tc.objectserver.api.GCStats;
import com.tc.stats.DSOMBean;

import javax.management.Notification;
import javax.management.NotificationListener;

public class GCStatsPanel extends XContainer
  implements NotificationListener
{
  private ConnectionContext m_cc;
  private XObjectTable      m_table;

  public GCStatsPanel(ConnectionContext cc) {
    super();

    m_cc = cc;

    AdminClientContext acc = AdminClient.getContext();
    load((ContainerResource)acc.topRes.getComponent("GCStatsPanel"));

    m_table = (XObjectTable)findComponent("GCStatsTable");

    GCStatsTableModel model = new GCStatsTableModel();
    m_table.setModel(model);

    DSOHelper helper  = DSOHelper.getHelper();
    GCStats[] gcStats = null;

    try {
      gcStats = helper.getGCStats(cc);
      cc.addNotificationListener(helper.getDSOMBean(cc), this);
    }
    catch(Exception e) {
      AdminClient.getContext().log(e);
      gcStats = new GCStats[]{};
    }

    model.setGCStats(gcStats);
  }

  public void handleNotification(Notification notice, Object notUsed) {
    String type = notice.getType();

    if(DSOMBean.GC_COMPLETED.equals(type)) {
      GCStatsTableModel model = (GCStatsTableModel)m_table.getModel();

      model.addGCStats((GCStats)notice.getSource());
    }
  }

  public void tearDown() {
    try {
      if(m_cc != null && m_cc.isConnected()) {
        DSOHelper helper = DSOHelper.getHelper();
        m_cc.removeNotificationListener(helper.getDSOMBean(m_cc), this);
      }
    }
    catch(Exception e) {/**/}

    super.tearDown();

    m_cc    = null;
    m_table = null;
  }
}
