/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import static com.tc.admin.model.IClusterNode.POLLED_ATTR_LIVE_OBJECT_COUNT;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;

import com.tc.admin.AbstractClusterListener;
import com.tc.admin.IAdminClientContext;
import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.BasicWorker;
import com.tc.admin.common.XAbstractAction;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XScrollPane;
import com.tc.admin.common.XSplitPane;
import com.tc.admin.common.XTabbedPane;
import com.tc.admin.model.IBasicObject;
import com.tc.admin.model.IClusterModel;
import com.tc.admin.model.IServer;
import com.tc.admin.model.IServerGroup;
import com.tc.admin.model.PolledAttributeListener;
import com.tc.admin.model.PolledAttributesResult;
import com.tc.admin.model.IClusterModel.PollScope;
import com.tc.stats.DSOClassInfo;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.concurrent.Callable;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

public class ClusteredHeapPanel extends XContainer implements PolledAttributeListener {
  protected IAdminClientContext     adminClientContext;
  protected IClusterModel           clusterModel;
  protected RootsPanel              rootsPanel;
  private XSplitPane                splitPane;
  private ClassesTable              classesTable;
  private ClassesTreeMap            classesTreeMap;
  private LiveObjectCountGraphPanel graphPanel;
  private TimeSeries                timeSeries;
  private boolean                   inited;

  private static final String       REFRESH = "Refresh";

  public ClusteredHeapPanel(IAdminClientContext adminClientContext, IClusterModel clusterModel, IBasicObject[] roots) {
    super(new BorderLayout());

    this.adminClientContext = adminClientContext;
    this.clusterModel = clusterModel;

    XTabbedPane tabbedPane = new XTabbedPane();
    tabbedPane.add("Live Object Browser", rootsPanel = new RootsPanel(adminClientContext, clusterModel, clusterModel,
                                                                      roots));
    tabbedPane.add("Instance Count", createInstanceCountPanel());
    tabbedPane.add("DGC", new GCStatsPanel(adminClientContext, clusterModel));

    graphPanel = createGraphPanel();
    splitPane = new XSplitPane(JSplitPane.VERTICAL_SPLIT, tabbedPane, graphPanel);
    add(splitPane, BorderLayout.CENTER);
    splitPane.setPreferences(adminClientContext.getPrefs().node("ClusteredHeapPanel"));

    add(splitPane);

    clusterModel.addPropertyChangeListener(new ClusterListener(clusterModel));
    if (clusterModel.isReady()) {
      init();
    }
  }

  private JComponent createInstanceCountPanel() {
    classesTable = new ClassesTable();
    classesTable.setModel(new ClassTableModel(adminClientContext));

    classesTreeMap = new ClassesTreeMap();
    classesTreeMap.setModel(new ClassTreeModel(adminClientContext, new DSOClassInfo[] {}));

    XSplitPane splitter = new XSplitPane(JSplitPane.HORIZONTAL_SPLIT, new XScrollPane(classesTable), classesTreeMap);

    KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0, true);
    RefreshAction refreshAction = new RefreshAction();

    classesTreeMap.getActionMap().put(REFRESH, refreshAction);
    classesTreeMap.getInputMap().put(ks, REFRESH);

    classesTable.getActionMap().put(REFRESH, refreshAction);
    classesTable.getInputMap().put(ks, REFRESH);

    splitter.setPreferences(adminClientContext.getPrefs().node("ClusteredHeapPanel").node("InstanceCountPanel"));

    return splitter;
  }

  private synchronized IClusterModel getClusterModel() {
    return clusterModel;
  }

  protected LiveObjectCountGraphPanel createGraphPanel() {
    return new LiveObjectCountGraphPanel(adminClientContext);
  }

  private void addPolledAttributeListener() {
    IClusterModel theClusterModel = getClusterModel();
    if (theClusterModel != null) {
      theClusterModel.addPolledAttributeListener(PollScope.ACTIVE_SERVERS, POLLED_ATTR_LIVE_OBJECT_COUNT, this);
    }
  }

  private void removePolledAttributeListener() {
    IClusterModel theClusterModel = getClusterModel();
    if (theClusterModel != null) {
      theClusterModel.removePolledAttributeListener(PollScope.ACTIVE_SERVERS, POLLED_ATTR_LIVE_OBJECT_COUNT, this);
    }
  }

  /**
   * Implementation of PolledAttributeListener.
   */
  public void attributesPolled(PolledAttributesResult result) {
    int liveObjectCount = 0;
    for (IServerGroup group : clusterModel.getServerGroups()) {
      IServer activeServer = group.getActiveServer();
      if (activeServer != null) {
        Number nodeValue = (Number) result.getPolledAttribute(activeServer, POLLED_ATTR_LIVE_OBJECT_COUNT);
        if (nodeValue != null) {
          liveObjectCount += nodeValue.intValue();
        } else {
          return;
        }
      } else {
        return;
      }
    }
    final int value = liveObjectCount;
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        timeSeries.addOrUpdate(new Second(), value);
      }
    });
  }

  class LiveObjectCountGraphPanel extends BaseRuntimeStatsPanel {
    LiveObjectCountGraphPanel(ApplicationContext appContext) {
      super(appContext);
      setBorder(BorderFactory.createTitledBorder(appContext.getString("dso.client.liveObjectCount")));
    }

    public void startMonitoringRuntimeStats() {
      addPolledAttributeListener();
      super.startMonitoringRuntimeStats();
    }

    public void stopMonitoringRuntimeStats() {
      removePolledAttributeListener();
      super.stopMonitoringRuntimeStats();
    }

    void setup() {
      setup(chartsPanel);
    }

    protected void setup(XContainer runtimeStatsPanel) {
      runtimeStatsPanel.setLayout(new BorderLayout());
      JFreeChart chart = createChart(timeSeries = createTimeSeries("LiveObjectCount"), false);
      ChartPanel chartPanel = createChartPanel(chart);
      runtimeStatsPanel.add(chartPanel);
    }
  }

  private class ClusterListener extends AbstractClusterListener {
    private ClusterListener(IClusterModel clusterModel) {
      super(clusterModel);
    }

    public void handleReady() {
      init();
    }
  }

  private void init() {
    IClusterModel theClusterModel = getClusterModel();
    if (theClusterModel != null) {
      if(!inited && theClusterModel.isReady()) {
        graphPanel.setup();
        inited = true;
      }
      adminClientContext.execute(new InitWorker());
    }
  }

  private class InitWorker extends BasicWorker<DSOClassInfo[]> {
    private InitWorker() {
      super(new Callable<DSOClassInfo[]>() {
        public DSOClassInfo[] call() throws Exception {
          return getClassInfos();
        }
      });
    }

    protected void finished() {
      Exception e = getException();
      if (e == null) {
        DSOClassInfo[] classInfo = getResult();
        classesTable.setClassInfo(classInfo);
        classesTreeMap.setModel(new ClassTreeModel(adminClientContext, classInfo));
      }
    }
  }

  private DSOClassInfo[] getClassInfos() {
    ArrayList<DSOClassInfo> list = new ArrayList<DSOClassInfo>();
    IClusterModel theClusterModel = getClusterModel();

    if (theClusterModel != null) {
      for (IServerGroup group : theClusterModel.getServerGroups()) {
        IServer activeServer = group.getActiveServer();

        if (activeServer != null) {
          DSOClassInfo[] classInfo = activeServer.getClassInfo();
          if (classInfo != null) {
            for (DSOClassInfo info : classInfo) {
              String className = info.getClassName();
              if (className.startsWith("com.tcclient")) continue;
              if (className.startsWith("[")) {
                int i = 0;
                while (className.charAt(i) == '[')
                  i++;
                if (className.charAt(i) == 'L') {
                  className = className.substring(i + 1, className.length() - 1);
                } else {
                  switch (className.charAt(i)) {
                    case 'Z':
                      className = "boolean";
                      break;
                    case 'I':
                      className = "int";
                      break;
                    case 'F':
                      className = "float";
                      break;
                    case 'C':
                      className = "char";
                      break;
                    case 'D':
                      className = "double";
                      break;
                    case 'B':
                      className = "byte";
                      break;
                  }
                }
                StringBuffer sb = new StringBuffer(className);
                for (int j = 0; j < i; j++) {
                  sb.append("[]");
                }
                className = sb.toString();
              }
              list.add(new DSOClassInfo(className, info.getInstanceCount()));
            }
          }
        }
      }
    }

    return list.toArray(new DSOClassInfo[0]);
  }

  public class RefreshAction extends XAbstractAction {
    public void actionPerformed(ActionEvent ae) {
      refresh();
    }
  }

  public void refresh() {
    init();
  }

  void clearModel() {
    rootsPanel.clearModel();
  }

  public void setObjects(IBasicObject[] roots) {
    rootsPanel.setObjects(roots);
  }

  public void addRoot(IBasicObject root) {
    rootsPanel.add(root);
  }

  public void tearDown() {
    super.tearDown();

    synchronized (this) {
      clusterModel = null;
      adminClientContext = null;
    }
  }
}
