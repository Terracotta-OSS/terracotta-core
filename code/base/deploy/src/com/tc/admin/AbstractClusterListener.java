/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import com.tc.admin.model.IClusterModel;
import com.tc.admin.model.IClusterModelElement;
import com.tc.admin.model.IServer;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.SwingUtilities;

public class AbstractClusterListener implements PropertyChangeListener {
  protected IClusterModel clusterModel;
  protected boolean       eventDispatchThreadOnly;

  public AbstractClusterListener(IClusterModel clusterModel) {
    this(clusterModel, true);
  }

  public AbstractClusterListener(IClusterModel clusterModel, boolean eventDispatchThreadOnly) {
    this.clusterModel = clusterModel;
    this.eventDispatchThreadOnly = eventDispatchThreadOnly;
  }

  public synchronized IClusterModel getClusterModel() {
    return clusterModel;
  }

  protected PropertyChangeRunnable createPropertyChangeRunnable(PropertyChangeEvent evt) {
    return new PropertyChangeRunnable(evt);
  }

  protected class PropertyChangeRunnable implements Runnable {
    protected final PropertyChangeEvent pce;

    protected PropertyChangeRunnable(PropertyChangeEvent pce) {
      this.pce = pce;
    }

    public void run() {
      IClusterModel theClusterModel = getClusterModel();
      if (theClusterModel == null) { return; }

      try {
        String prop = pce.getPropertyName();
        if (IClusterModel.PROP_CONNECT_ERROR.equals(prop)) {
          Exception connectError = (Exception) pce.getNewValue();
          if (connectError != null) {
            handleConnectError(connectError);
          }
        } else if (IClusterModel.PROP_CONNECTED.equals(prop)) {
          handleConnected();
        } else if (IClusterModelElement.PROP_READY.equals(prop)) {
          handleReady();
        } else if (IClusterModel.PROP_ACTIVE_COORDINATOR.equals(prop)) {
          IServer oldActive = (IServer) pce.getOldValue();
          IServer newActive = (IServer) pce.getNewValue();
          handleActiveCoordinator(oldActive, newActive);
        }
      } catch (Exception e) {
        handleUncaughtError(e);
      }
    }
  }

  protected void handleUncaughtError(Exception e) {
    e.printStackTrace();
  }

  public void propertyChange(PropertyChangeEvent evt) {
    IClusterModel theClusterModel = getClusterModel();
    if (theClusterModel == null) { return; }

    PropertyChangeEvent clonedEvent = new PropertyChangeEvent(evt.getSource(), evt.getPropertyName(),
                                                              evt.getOldValue(), evt.getNewValue());
    Runnable r = createPropertyChangeRunnable(clonedEvent);
    if (!eventDispatchThreadOnly || SwingUtilities.isEventDispatchThread()) {
      r.run();
    } else {
      SwingUtilities.invokeLater(r);
    }
  }

  protected void handleConnectError(Exception e) {
    /* override */
  }

  protected void handleConnected() {
    /* override */
  }

  protected void handleReady() {
    /* override */
  }

  protected void handleActiveCoordinator(IServer oldActive, IServer newActive) {
    /* override */
  }

  public synchronized void tearDown() {
    clusterModel = null;
  }
}
