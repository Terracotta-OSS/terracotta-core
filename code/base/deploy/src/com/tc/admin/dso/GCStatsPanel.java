/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
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
import java.io.IOException;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServerInvocationHandler;
import javax.management.MalformedObjectNameException;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ReflectionException;
import javax.swing.JOptionPane;

public class GCStatsPanel extends XContainer implements NotificationListener {
  private GCStatsNode                  m_gcStatsNode;
  private XObjectTable                 m_table;
  private PopupMenu                    m_popupMenu;
  private ObjectManagementMonitorMBean m_objectManagementMonitor;

  public GCStatsPanel(GCStatsNode gcStatsNode) throws IOException, MalformedObjectNameException,
      InstanceNotFoundException, MBeanException, ReflectionException, AttributeNotFoundException {
    super();

    AdminClientContext acc = AdminClient.getContext();
    load((ContainerResource) acc.topRes.getComponent("GCStatsPanel"));

    m_gcStatsNode = gcStatsNode;
    m_table = (XObjectTable) findComponent("GCStatsTable");

    GCStatsTableModel model = new GCStatsTableModel();
    m_table.setModel(model);

    DSOHelper helper = DSOHelper.getHelper();
    ConnectionContext cc = gcStatsNode.getConnectionContext();
    GCStats[] gcStats = helper.getGCStats(cc);
    cc.addNotificationListener(helper.getDSOMBean(cc), this);

    model.setGCStats(gcStats);

    m_objectManagementMonitor = (ObjectManagementMonitorMBean) MBeanServerInvocationHandler
        .newProxyInstance(cc.mbsc, L2MBeanNames.OBJECT_MANAGEMENT, ObjectManagementMonitorMBean.class, false);

    RunGCAction runDGCAction = new RunGCAction();
    Button runDGCButton = (Button) findComponent("RunGCButton");
    runDGCButton.setAction(runDGCAction);

    m_popupMenu = new PopupMenu("GC");
    m_popupMenu.add(runDGCAction);
    m_table.add(m_popupMenu);
    m_table.addMouseListener(new MouseAdapter() {
      public void mousePressed(MouseEvent e) {
        testPopup(e);
      }

      public void mouseReleased(MouseEvent e) {
        testPopup(e);
      }

      public void testPopup(MouseEvent e) {
        if (e.isPopupTrigger()) {
          m_popupMenu.show(m_table, e.getX(), e.getY());
        }
      }
    });
  }

  void newConnectionContext() {
    try {
      DSOHelper helper = DSOHelper.getHelper();
      ConnectionContext cc = m_gcStatsNode.getConnectionContext();
      cc.addNotificationListener(helper.getDSOMBean(cc), this);

      m_objectManagementMonitor = (ObjectManagementMonitorMBean) MBeanServerInvocationHandler
          .newProxyInstance(cc.mbsc, L2MBeanNames.OBJECT_MANAGEMENT, ObjectManagementMonitorMBean.class, false);
    } catch (Exception e) {/**/
    }
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

    if (DSOMBean.GC_COMPLETED.equals(type)) {
      GCStatsTableModel model = (GCStatsTableModel) m_table.getModel();
      model.addGCStats((GCStats) notice.getSource());
    }
  }

  private void runGC() {
    try {
      m_objectManagementMonitor.runGC();
    } catch (RuntimeException re) {
      Frame frame = (Frame) getAncestorOfClass(Frame.class);
      Throwable cause = re.getCause();
      String msg = cause != null ? cause.getMessage() : re.getMessage();
      String title = frame.getTitle();

      JOptionPane.showMessageDialog(frame, msg, title, JOptionPane.INFORMATION_MESSAGE);
    }
  }

  public void tearDown() {
    try {
      ConnectionContext cc = m_gcStatsNode.getConnectionContext();
      if (cc != null && cc.isConnected()) {
        DSOHelper helper = DSOHelper.getHelper();
        cc.removeNotificationListener(helper.getDSOMBean(cc), this);
      }
    } catch (Exception e) {/**/
    }

    super.tearDown();

    m_table = null;
  }
}
