/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.event;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Generic Listener/Observer implementation.
 * 
 * <p>USAGE:</p>
 * <pre>
 * class Worker implements Runnable {
 * 
 *   private final EventMulticaster updateObserver;
 * 
 *   private void doWork() {
 *     if (stateChanged()) updateObserver.fireUpdateEvent();
 *     
 * ...
 * 
 *   public void addWorkEventListener(UpdateEventListener listener) {
 *     updateObserver.addListener(listener);
 *    
 * ...
 * 
 * class Master {
 * 
 *   private void delegateTask() {
 *     Worker worker = new Worker();
 *     worker.addWorkEventListener(new UpdateEventListener() {
 *       public void handleUpdate() {
 *         notifyUser();         
 *       }
 *     });
 *     
 * ...
 * 
 *   private void notifyUser() {
 *   
 * ...
 * </pre>
 */
public class EventMulticaster {

  private UpdateEventListener eventListener = new NullEventListener();

  public synchronized void fireUpdateEvent() {
    eventListener.handleUpdate();
  }

  public synchronized void addListener(UpdateEventListener listener) {
    if (eventListener instanceof EventMulticaster.NullEventListener) {
      eventListener = listener;
    } else if (eventListener instanceof EventMulticaster.BroadcastListener) {
      EventMulticaster.BroadcastListener broadcast = (EventMulticaster.BroadcastListener) eventListener;
      broadcast.add(listener);
    } else {
      EventMulticaster.BroadcastListener broadcast = new BroadcastListener();
      broadcast.add(eventListener);
      broadcast.add(listener);
      eventListener = broadcast;
    }
  }

  public synchronized boolean removeListener(UpdateEventListener listener) {
    if (eventListener instanceof EventMulticaster.BroadcastListener) {
      EventMulticaster.BroadcastListener broadcast = (EventMulticaster.BroadcastListener) eventListener;
      if (!broadcast.remove(listener)) return false;
      if (broadcast.size() == 1) eventListener = broadcast.pop();
      return true;
    } else {
      if (eventListener != listener) return false;
      eventListener = new NullEventListener();
      return true;
    }
  }

  // --------------------------------------------------------------------------------

  private class BroadcastListener implements UpdateEventListener {

    private List listeners = new ArrayList();

    public void handleUpdate() {
      for (Iterator iter = listeners.iterator(); iter.hasNext();) {
        ((UpdateEventListener) iter.next()).handleUpdate();
      }
    }

    private void add(UpdateEventListener listener) {
      listeners.add(listener);
    }

    private boolean remove(UpdateEventListener listener) {
      return listeners.remove(listener);
    }

    private int size() {
      return listeners.size();
    }

    private UpdateEventListener pop() {
      return (UpdateEventListener) listeners.iterator().next();
    }
  }

  // --------------------------------------------------------------------------------

  private class NullEventListener implements UpdateEventListener {

    public void handleUpdate() {
    // do nothing
    }
  }
}
