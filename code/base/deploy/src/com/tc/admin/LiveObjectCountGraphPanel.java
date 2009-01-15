package com.tc.admin;

import static com.tc.admin.model.IClusterModel.PollScope.ACTIVE_SERVERS;
import static com.tc.admin.model.IClusterNode.POLLED_ATTR_LIVE_OBJECT_COUNT;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;

import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.XContainer;
import com.tc.admin.dso.BaseRuntimeStatsPanel;
import com.tc.admin.model.IClusterModel;
import com.tc.admin.model.IServer;
import com.tc.admin.model.IServerGroup;
import com.tc.admin.model.PolledAttributeListener;
import com.tc.admin.model.PolledAttributesResult;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.SwingUtilities;

class LiveObjectCountGraphPanel extends BaseRuntimeStatsPanel implements PolledAttributeListener {
  private IClusterModel   clusterModel;
  private TimeSeries      timeSeries;
  private ClusterListener clusterListener;

  public LiveObjectCountGraphPanel(ApplicationContext appContext, IClusterModel clusterModel) {
    super(appContext);
    this.clusterModel = clusterModel;
    setBorder(BorderFactory.createTitledBorder(appContext.getString("dso.client.liveObjectCount")));
    setName(clusterModel.toString());
    setup(chartsPanel);
    clusterModel.addPropertyChangeListener(clusterListener = new ClusterListener(clusterModel));
  }

  private class ClusterListener extends AbstractClusterListener {
    private ClusterListener(IClusterModel clusterModel) {
      super(clusterModel);
    }

    public void handleReady() {
      if (clusterModel.isReady()) {
        startMonitoringRuntimeStats();
      } else {
        stopMonitoringRuntimeStats();
      }
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

  private void addPolledAttributeListener() {
    IClusterModel theClusterModel = getClusterModel();
    if (theClusterModel != null) {
      theClusterModel.addPolledAttributeListener(ACTIVE_SERVERS, POLLED_ATTR_LIVE_OBJECT_COUNT, this);
    }
  }

  private void removePolledAttributeListener() {
    IClusterModel theClusterModel = getClusterModel();
    if (theClusterModel != null) {
      theClusterModel.removePolledAttributeListener(ACTIVE_SERVERS, POLLED_ATTR_LIVE_OBJECT_COUNT, this);
    }
  }

  public void startMonitoringRuntimeStats() {
    if (clusterModel.isReady()) {
      addPolledAttributeListener();
      super.startMonitoringRuntimeStats();
    }
  }

  public void stopMonitoringRuntimeStats() {
    removePolledAttributeListener();
    super.stopMonitoringRuntimeStats();
  }

  protected void setup(XContainer runtimeStatsPanel) {
    runtimeStatsPanel.setLayout(new BorderLayout());
    JFreeChart chart = createChart(timeSeries = createTimeSeries("LiveObjectCount"), false);
    ChartPanel chartPanel = createChartPanel(chart);
    runtimeStatsPanel.add(chartPanel);
  }

  private synchronized IClusterModel getClusterModel() {
    return clusterModel;
  }

  public void tearDown() {
    clusterModel.removePropertyChangeListener(clusterListener);
    clusterModel.removePolledAttributeListener(ACTIVE_SERVERS, POLLED_ATTR_LIVE_OBJECT_COUNT, this);

    super.tearDown();

    synchronized (this) {
      appContext = null;
      clusterModel = null;
      timeSeries.clear();
      timeSeries = null;
      clusterListener = null;
    }
  }
}
