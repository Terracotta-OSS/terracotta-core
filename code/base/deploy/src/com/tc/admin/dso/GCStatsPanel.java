/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import org.dijon.Button;
import org.dijon.ContainerResource;
import org.dijon.PopupMenu;

import com.tc.admin.AdminClient;
import com.tc.admin.AdminClientContext;
import com.tc.admin.ConnectionContext;
import com.tc.admin.common.BasicWorker;
import com.tc.admin.common.ExceptionHelper;
import com.tc.admin.common.MBeanServerInvocationProxy;
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
import java.util.concurrent.Callable;

import javax.management.Notification;
import javax.management.NotificationListener;
import javax.swing.JOptionPane;

public class GCStatsPanel extends XContainer implements NotificationListener {
  private AdminClientContext           m_acc;
  private GCStatsNode                  m_gcStatsNode;
  private XObjectTable                 m_table;
  private PopupMenu                    m_popupMenu;
  private RunGCAction                  m_gcAction;
  private ObjectManagementMonitorMBean m_objectManagementMonitor;

  public GCStatsPanel(GCStatsNode gcStatsNode) {
    super();

    m_acc = AdminClient.getContext();
    load((ContainerResource) m_acc.topRes.getComponent("GCStatsPanel"));

    m_gcStatsNode = gcStatsNode;
    m_table = (XObjectTable) findComponent("GCStatsTable");
    m_table.setModel(new GCStatsTableModel());

    m_gcAction = new RunGCAction();
    Button runDGCButton = (Button) findComponent("RunGCButton");
    runDGCButton.setAction(m_gcAction);

    m_popupMenu = new PopupMenu("GC");
    m_popupMenu.add(m_gcAction);
    m_table.add(m_popupMenu);
    m_table.addMouseListener(new TableMouseHandler());

    m_acc.executorService.execute(new InitWorker(true));
  }

  private class InitWorker extends BasicWorker<GCStats[]> {
    private boolean getStats;

    private InitWorker(final boolean getStats) {
      super(new Callable<GCStats[]>() {
        public GCStats[] call() throws Exception {
          DSOHelper helper = DSOHelper.getHelper();
          ConnectionContext cc = m_gcStatsNode.getConnectionContext();
          cc.addNotificationListener(helper.getDSOMBean(cc), GCStatsPanel.this);
          m_objectManagementMonitor = (ObjectManagementMonitorMBean) MBeanServerInvocationProxy
              .newProxyInstance(cc.mbsc, L2MBeanNames.OBJECT_MANAGEMENT, ObjectManagementMonitorMBean.class, false);
          return getStats ? DSOHelper.getHelper().getGCStats(cc) : null;
        }
      });
      this.getStats = getStats;
    }

    protected void finished() {
      Exception e = getException();
      if (e != null) {
        m_acc.log(e);
      } else if (getStats) {
        GCStatsTableModel model = (GCStatsTableModel) m_table.getModel();
        model.setGCStats(getResult());
      }
      m_gcAction.setEnabled(true);
    }
  }

  void newConnectionContext() {
    m_gcAction.setEnabled(false);
    m_acc.executorService.execute(new InitWorker(false));
  }

  class TableMouseHandler extends MouseAdapter {
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

  private class RunGCWorker extends BasicWorker<Void> {
    private RunGCWorker() {
      super(new Callable<Void>() {
        public Void call() {
          m_objectManagementMonitor.runGC();
          return null;
        }
      });
    }

    protected void finished() {
      Exception e = getException();
      if (e != null) {
        Frame frame = (Frame) getAncestorOfClass(Frame.class);
        Throwable cause = ExceptionHelper.getRootCause(e);
        String msg = cause.getMessage();
        String title = frame.getTitle();

        JOptionPane.showMessageDialog(frame, msg, title, JOptionPane.INFORMATION_MESSAGE);
      }
    }
  }

  private void runGC() {
    m_acc.executorService.execute(new RunGCWorker());
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

    m_acc = null;
    m_gcStatsNode = null;
    m_table = null;
    m_popupMenu = null;
    m_gcAction = null;
    m_objectManagementMonitor = null;
  }
}
