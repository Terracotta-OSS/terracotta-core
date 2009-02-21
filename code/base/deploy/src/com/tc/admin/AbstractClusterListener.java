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
  protected final IClusterModel clusterModel;

  public AbstractClusterListener(IClusterModel clusterModel) {
    this.clusterModel = clusterModel;
  }

  public IClusterModel getClusterModel() {
    return clusterModel;
  }

  protected PropertyChangeRunnable createPropertyChangeRunnable(PropertyChangeEvent evt) {
    return new PropertyChangeRunnable(evt);
  }

  protected class PropertyChangeRunnable implements Runnable {
    protected final PropertyChangeEvent pce;

    PropertyChangeRunnable(PropertyChangeEvent pce) {
      this.pce = pce;
    }

    public void run() {
      String prop = pce.getPropertyName();
      if (IServer.PROP_CONNECTED.equals(prop)) {
        handleConnected();
      } else if (IClusterModelElement.PROP_READY.equals(prop)) {
        handleReady();
      } else if (IClusterModel.PROP_ACTIVE_COORDINATOR.equals(prop)) {
        IServer oldActive = (IServer) pce.getOldValue();
        IServer newActive = (IServer) pce.getNewValue();
        handleActiveCoordinator(oldActive, newActive);
      }
    }
  }

  public void propertyChange(PropertyChangeEvent evt) {
    PropertyChangeEvent clonedEvent = new PropertyChangeEvent(evt.getSource(), evt.getPropertyName(),
                                                              evt.getOldValue(), evt.getNewValue());
    Runnable r = createPropertyChangeRunnable(clonedEvent);
    if (SwingUtilities.isEventDispatchThread()) {
      r.run();
    } else {
      SwingUtilities.invokeLater(r);
    }
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
}
