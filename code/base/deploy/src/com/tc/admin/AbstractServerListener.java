/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import com.tc.admin.model.IClusterModelElement;
import com.tc.admin.model.IServer;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.SwingUtilities;

public class AbstractServerListener implements PropertyChangeListener {
  protected final IServer server;

  public AbstractServerListener(IServer server) {
    this.server = server;
  }

  public void startListening() {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        handleConnected();
        server.addPropertyChangeListener(AbstractServerListener.this);
      }
    });
  }

  public IServer getServer() {
    return server;
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

  protected PropertyChangeRunnable createPropertyChangeRunnable(PropertyChangeEvent evt) {
    return new PropertyChangeRunnable(evt);
  }

  private class PropertyChangeRunnable implements Runnable {
    protected final PropertyChangeEvent pce;

    PropertyChangeRunnable(PropertyChangeEvent pce) {
      this.pce = pce;
    }

    public void run() {
      String prop = pce.getPropertyName();
      if (IServer.PROP_CONNECTED.equals(prop)) {
        if (server.isConnected()) {
          handleConnected();
        }
      } else if (IClusterModelElement.PROP_READY.equals(prop)) {
        handleReady();
      } else if (IServer.PROP_CONNECT_ERROR.equals(prop)) {
        if (pce.getNewValue() != null) {
          handleConnectError();
        }
      }
    }
  }

  protected void handleConnected() {
    if (server.isActive()) {
      handleActivation();
    } else if (server.isPassiveStandby()) {
      handlePassiveStandby();
    } else if (server.isPassiveUninitialized()) {
      handlePassiveUninitialized();
    } else if (server.isStarted()) {
      handleStarting();
    }
  }

  protected void handleConnectError() {
    /* override */
  }

  protected void handleStarting() {
    /* override */
  }

  protected void handleReady() {
    /* override */
  }
  
  protected void handleActivation() {
    /* override */
  }

  protected void handlePassiveStandby() {
    /* override */
  }

  protected void handlePassiveUninitialized() {
    /* override */
  }

  protected void handleDisconnected() {
    /* override */
  }

}
