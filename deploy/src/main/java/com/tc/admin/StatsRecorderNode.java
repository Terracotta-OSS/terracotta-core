/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.ComponentNode;
import com.tc.admin.model.IClusterModel;
import com.tc.admin.model.IClusterStatsListener;
import com.tc.admin.model.IServer;

import java.awt.Component;
import java.util.Map;

import javax.swing.SwingUtilities;

public class StatsRecorderNode extends ComponentNode implements IClusterStatsListener {
  protected final ApplicationContext appContext;
  private final IClusterModel        clusterModel;

  private StatsRecorderPanel         statsRecorderPanel;
  private String                     baseLabel;
  private final String               recordingSuffix;

  public StatsRecorderNode(ApplicationContext appContext, IClusterModel clusterModel) {
    super();

    this.appContext = appContext;
    this.clusterModel = clusterModel;

    setLabel(baseLabel = appContext.getMessage("stats.recorder.node.label"));
    recordingSuffix = appContext.getMessage("stats.recording.suffix");
    setIcon(ServerHelper.getHelper().getStatsRecorderIcon());

    clusterModel.addPropertyChangeListener(new ClusterListener(clusterModel));
    if (clusterModel.isReady()) {
      IServer activeCoord = clusterModel.getActiveCoordinator();
      if (activeCoord != null && activeCoord.isClusterStatsSupported()) {
        activeCoord.addClusterStatsListener(this);
      }
    }
  }

  @Override
  public Component getComponent() {
    if (statsRecorderPanel == null) {
      statsRecorderPanel = createStatsRecorderPanel(appContext, clusterModel);
      statsRecorderPanel.setNode(this);
    }
    return statsRecorderPanel;
  }

  protected StatsRecorderPanel createStatsRecorderPanel(ApplicationContext theAppContext, IClusterModel theClusterModel) {
    return new StatsRecorderPanel(theAppContext, theClusterModel);
  }

  private class ClusterListener extends AbstractClusterListener {
    private ClusterListener(IClusterModel clusterModel) {
      super(clusterModel);
    }

    @Override
    protected void handleReady() {
      if (clusterModel.isReady()) {
        IServer activeCoord = clusterModel.getActiveCoordinator();
        if (activeCoord != null && activeCoord.isClusterStatsSupported()) {
          activeCoord.addClusterStatsListener(StatsRecorderNode.this);
        }
      }
    }

    @Override
    public void handleActiveCoordinator(IServer oldActive, IServer newActive) {
      if (oldActive != null) {
        oldActive.removeClusterStatsListener(StatsRecorderNode.this);
      }
      if (newActive != null && newActive.isClusterStatsSupported()) {
        newActive.addClusterStatsListener(StatsRecorderNode.this);
      }
    }

    @Override
    protected void handleUncaughtError(Exception e) {
      appContext.log(e);
    }
  }

  String[] getConnectionCredentials() {
    return clusterModel.getConnectionCredentials();
  }

  Map<String, Object> getConnectionEnvironment() {
    return clusterModel.getConnectionEnvironment();
  }

  void showRecording(boolean recording) {
    setLabel(baseLabel + (recording ? recordingSuffix : ""));
    nodeChanged();
  }

  /*
   * IClusterStatsListener implementation
   */

  public void allSessionsCleared() {
    /**/
  }

  public void connected() {
    /**/
  }

  public void disconnected() {
    /**/
  }

  public void reinitialized() {
    /**/
  }

  public void sessionCleared(String sessionId) {
    /**/
  }

  public void sessionCreated(String sessionId) {
    /**/
  }

  public void sessionStarted(String sessionId) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        showRecording(true);
      }
    });
  }

  public void sessionStopped(String sessionId) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        showRecording(false);
      }
    });
  }

  @Override
  public void tearDown() {
    if (statsRecorderPanel != null) {
      statsRecorderPanel.tearDown();
    }
    super.tearDown();
  }
}
