/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.platform;

import com.tc.object.handshakemanager.ClientHandshakeManager;
import com.tc.platform.rejoin.RejoinLifecycleListener;
import com.tc.platform.rejoin.RejoinManager;

import java.util.concurrent.CopyOnWriteArrayList;

public class RejoinLifecycleEventController {

  private final CopyOnWriteArrayList<RejoinLifecycleListener> upperLayerListeners = new CopyOnWriteArrayList<RejoinLifecycleListener>();
  private final ClientHandshakeManager                        clientHandshakeManager;

  public RejoinLifecycleEventController(RejoinManager rejoinManager, ClientHandshakeManager clientHandshakeManager) {
    this.clientHandshakeManager = clientHandshakeManager;
    rejoinManager.addListener(new RejoinLifecycleListenerImpl(this));
  }

  public void addUpperLayerListener(RejoinLifecycleListener listener) {
    upperLayerListeners.addIfAbsent(listener);
  }

  public void removeUpperLayerListener(RejoinLifecycleListener listener) {
    upperLayerListeners.remove(listener);
  }

  private void onRejoinStart() {
    // reset all subsystems
    clientHandshakeManager.reset();
    // notify upper listeners
    for (RejoinLifecycleListener listener : upperLayerListeners) {
      listener.onRejoinStart();
    }
  }

  private void onRejoinComplete() {
    // all subsystems must be already un-paused
    // notify upper listeners
    for (RejoinLifecycleListener listener : upperLayerListeners) {
      listener.onRejoinComplete();
    }
  }

  private static class RejoinLifecycleListenerImpl implements RejoinLifecycleListener {
    private final RejoinLifecycleEventController controller;

    public RejoinLifecycleListenerImpl(RejoinLifecycleEventController controller) {
      this.controller = controller;
    }

    @Override
    public void onRejoinStart() {
      controller.onRejoinStart();
    }

    @Override
    public void onRejoinComplete() {
      controller.onRejoinComplete();
    }

  }

}
