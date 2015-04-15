/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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
