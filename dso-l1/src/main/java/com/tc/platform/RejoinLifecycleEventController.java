/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.platform;

import com.tc.object.handshakemanager.ClientHandshakeManager;
import com.tc.platform.rejoin.RejoinLifecycleListener;

import java.util.concurrent.CopyOnWriteArraySet;

public class RejoinLifecycleEventController implements RejoinLifecycleListener {

  // private final Status status = new Status();
  private final CopyOnWriteArraySet<RejoinLifecycleListener> upperLayerListeners = new CopyOnWriteArraySet<RejoinLifecycleListener>();
  private final ClientHandshakeManager                       clientHandshakeManager;

  public RejoinLifecycleEventController(ClientHandshakeManager clientHandshakeManager) {
    this.clientHandshakeManager = clientHandshakeManager;
  }

  public void addUpperLayerListener(RejoinLifecycleListener listener) {
    upperLayerListeners.add(listener);
  }

  public void removeUpperLayerListener(RejoinLifecycleListener listener) {
    upperLayerListeners.remove(listener);
  }

  @Override
  public void onRejoinStart() {
    // closePlatformServiceGate();
    // reset all subsystems
    clientHandshakeManager.reset();
    notifyUpperLayerListeners(RejoinEventType.START);
  }

  private void notifyUpperLayerListeners(RejoinEventType type) {
    switch (type) {
      case START:
        for (RejoinLifecycleListener listener : upperLayerListeners) {
          listener.onRejoinStart();
        }
        break;
      case COMPLETE:
        for (RejoinLifecycleListener listener : upperLayerListeners) {
          listener.onRejoinComplete();
        }
        break;
    }
  }

  @Override
  public void onRejoinComplete() {
    // openPlatformServiceGate();
    notifyUpperLayerListeners(RejoinEventType.COMPLETE);
  }

  // private void closePlatformServiceGate() {
  // // don't let any more threads enter the platform service
  // status.markRejoinInProgress();
  // }

  // private void openPlatformServiceGate() {
  // status.markRejoinComplete();
  // }

  // @Override
  // public Object intercept(PlatformService actualDelegate, Method method, Object[] args) throws Exception {
  // // don't go ahead if rejoin already in progress
  // status.waitIfRejoinInProgress();
  //
  // // todo: handle threads that went in but will come out due to 'reset'?
  // return method.invoke(actualDelegate, args);
  // }

  // private static class Status {
  //
  // private final Object monitor = new Object();
  // private final AtomicBoolean rejoinInProgress = new AtomicBoolean(false);
  //
  // public void markRejoinInProgress() {
  // synchronized (monitor) {
  // rejoinInProgress.set(true);
  // monitor.notifyAll();
  // }
  // }
  //
  // public void markRejoinComplete() {
  // synchronized (monitor) {
  // rejoinInProgress.set(false);
  // monitor.notifyAll();
  // }
  // }
  //
  // public void waitIfRejoinInProgress() throws AbortedOperationException, InterruptedException {
  // while (rejoinInProgress.get()) {
  // synchronized (monitor) {
  // try {
  // monitor.wait();
  // } catch (InterruptedException e) {
  // // return with aborted exception if interrrupted during wait
  // // todo: consult abortableManager here? or throw for every interrupt?
  // boolean aborted = true;
  // if (aborted) {
  // throw new AbortedOperationException("Interrupted while waiting for rejoin to complete", e);
  // } else {
  // throw new InterruptedException("Interrupted while waiting for rejoin to complete");
  // }
  // }
  // }
  // }
  // }
  // }
  private static enum RejoinEventType {
    START, COMPLETE;
  }

}
