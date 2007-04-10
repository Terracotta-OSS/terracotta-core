/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.dso;

import org.dijon.Button;
import org.dijon.ContainerResource;
import org.dijon.PopupMenu;

import com.tc.admin.AdminClient;
import com.tc.admin.AdminClientContext;
import com.tc.admin.ConnectionContext;
import com.tc.admin.common.XAbstractAction;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XObjectTable;
import com.tc.management.beans.L2MBeanNames;
import com.tc.management.beans.object.ObjectManagementMonitorMBean;
import com.tc.objectserver.api.GCStats;
import com.tc.stats.DSOMBean;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.management.MBeanServerInvocationHandler;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.swing.JOptionPane;

public class GCStatsPanel extends XContainer
  implements NotificationListener
{
  private ConnectionContext            m_cc;
  private XObjectTable                 m_table;
  private PopupMenu                    m_popupMenu;
  private ObjectManagementMonitorMBean m_objectManagementMonitor;
  
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
    
    m_objectManagementMonitor = (ObjectManagementMonitorMBean) MBeanServerInvocationHandler
      .newProxyInstance(m_cc.mbsc, L2MBeanNames.OBJECT_MANAGEMENT, ObjectManagementMonitorMBean.class, false);

    RunGCAction runDGCAction = new RunGCAction();
    Button runDGCButton = (Button)findComponent("RunGCButton");
    runDGCButton.setAction(runDGCAction);
    
    m_popupMenu = new PopupMenu("GC");
    m_popupMenu.add(runDGCAction);
    m_table.add(m_popupMenu);
    m_table.addMouseListener(new MouseAdapter() {
      public void mousePressed(MouseEvent e) {testPopup(e);}
      public void mouseReleased(MouseEvent e) {testPopup(e);}
      
      public void testPopup(MouseEvent e) {
        if(e.isPopupTrigger()) {
          m_popupMenu.show(m_table, e.getX(), e.getY());
        }
      }
    });
  }

  class RunGCAction extends XAbstractAction {
    RunGCAction() {
      super("Run GC");
    }
    
    public void actionPerformed(ActionEvent ae) {
      runGC();
    }
  }
  
  public void handleNotification(Notification notice, Object notUsed) {
    String type = notice.getType();

    if(DSOMBean.GC_COMPLETED.equals(type)) {
      GCStatsTableModel model = (GCStatsTableModel)m_table.getModel();
      model.addGCStats((GCStats)notice.getSource());
    }
  }

  private void runGC() {
    try {
      m_objectManagementMonitor.runGC();
    } catch (RuntimeException re) {
      Frame  frame = (Frame)getAncestorOfClass(Frame.class);
      String msg   = re.getMessage();
      String title = frame.getTitle();
      
      JOptionPane.showMessageDialog(frame, msg, title, JOptionPane.INFORMATION_MESSAGE);
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
