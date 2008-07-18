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
import com.tc.admin.common.BasicWorker;
import com.tc.admin.common.ExceptionHelper;
import com.tc.admin.common.XAbstractAction;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XObjectTable;
import com.tc.admin.model.DGCListener;
import com.tc.objectserver.api.GCStats;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.Callable;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class GCStatsPanel extends XContainer implements DGCListener {
  private AdminClientContext m_acc;
  private GCStatsNode        m_gcStatsNode;
  private XObjectTable       m_table;
  private PopupMenu          m_popupMenu;
  private RunGCAction        m_gcAction;

  public GCStatsPanel(GCStatsNode gcStatsNode) {
    super();

    m_acc = AdminClient.getContext();
    load((ContainerResource) m_acc.getComponent("GCStatsPanel"));

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

    m_acc.execute(new InitWorker());
    gcStatsNode.getClusterModel().addDGCListener(this);
  }

  private class InitWorker extends BasicWorker<GCStats[]> {
    private InitWorker() {
      super(new Callable<GCStats[]>() {
        public GCStats[] call() throws Exception {
          return m_gcStatsNode.getClusterModel().getGCStats();
        }
      });
    }

    protected void finished() {
      Exception e = getException();
      if (e != null) {
        m_acc.log(e);
      } else {
        GCStatsTableModel model = (GCStatsTableModel) m_table.getModel();
        model.setGCStats(getResult());
      }
      m_gcAction.setEnabled(true);
    }
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

  public void statusUpdate(GCStats gcStats) {
    SwingUtilities.invokeLater(new ModelUpdater(gcStats));
  }

  private class ModelUpdater implements Runnable {
    private GCStats m_gcStats;

    private ModelUpdater(GCStats gcStats) {
      m_gcStats = gcStats;
    }

    public void run() {
      ((GCStatsTableModel) m_table.getModel()).addGCStats(m_gcStats);
    }
  }

  private class RunGCWorker extends BasicWorker<Void> {
    private RunGCWorker() {
      super(new Callable<Void>() {
        public Void call() {
          m_gcStatsNode.getClusterModel().runGC();
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
    m_acc.execute(new RunGCWorker());
  }

  public void tearDown() {
    m_gcStatsNode.getClusterModel().removeDGCListener(this);

    super.tearDown();

    m_acc = null;
    m_gcStatsNode = null;
    m_table = null;
    m_popupMenu = null;
    m_gcAction = null;
  }
}
