/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.event;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Generic Event/Listener implementation.
 * <p>
 * USAGE:
 * </p>
 * 
 * <pre>
 *  class Worker implements Runnable {
 *  
 *  private final EventMulticaster updateObserver;
 *  
 *  private void doWork() {
 *    if (stateChanged()) updateObserver.fireUpdateEvent();
 *  
 *  ...
 *  
 *  public void addWorkEventListener(UpdateEventListener listener) {
 *    updateObserver.addListener(listener);
 *  
 *  ...
 *  
 *  class Master {
 *  
 *  private void delegateTask() {
 *    Worker worker = new Worker();
 *    worker.addWorkEventListener(new UpdateEventListener() {
 *      public void handleUpdate(Object arg) {
 *        System.out.println(arg);       
 *      }
 *    });
 *  
 *  ...
 * </pre>
 */
public final class EventMulticaster implements Serializable {

  private transient UpdateEventListener eventListener;
  private transient EventQueue          queue;
  private transient Thread              dispatchThread;
  private volatile boolean              dispatchInterrupted;

  public synchronized void enableDispatchThread() {
    this.eventListener = null;
    this.queue = new EventQueue();
    this.dispatchThread = new Thread() {
      public void run() {
        UpdateEvent event = null;
        try {
          while (true) {
            event = queue.take();
            event.listener.handleUpdate(event.arg);
          }
        } catch (InterruptedException e) {
          dispatchInterrupted = true;
        }
      }
    };
    dispatchThread.setDaemon(true);
    dispatchThread.start();
  }

  public void fireUpdateEvent() {
    fireUpdateEvent(null);
  }

  public synchronized void fireUpdateEvent(Object arg) {
    if (dispatchInterrupted) throw new IllegalStateException();
    if (eventListener != null) {
      if (queue == null) eventListener.handleUpdate(arg);
      else {
        UpdateEvent event = new UpdateEvent();
        event.listener = eventListener;
        event.arg = arg;
        queue.offer(event);
      }
    }
  }

  public synchronized void addListener(UpdateEventListener listener) {
    if (eventListener == null) {
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
      eventListener = null;
      return true;
    }
  }

  // --------------------------------------------------------------------------------

  private class BroadcastListener implements UpdateEventListener, Serializable {

    private List listeners = Collections.synchronizedList(new ArrayList());

    public void handleUpdate(Object arg) {
      for (Iterator iter = listeners.iterator(); iter.hasNext();) {
        if (queue == null) ((UpdateEventListener) iter.next()).handleUpdate(arg);
        else {
          UpdateEvent event = new UpdateEvent();
          event.listener = (UpdateEventListener) iter.next();
          event.arg = arg;
          queue.offer(event);
        }
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

  private class UpdateEvent implements Serializable {
    private UpdateEventListener listener;
    private Object              arg;
  }

  // --------------------------------------------------------------------------------

  private class EventQueue implements Serializable {

    private final List list = new LinkedList();

    private void offer(UpdateEvent val) {
      if (val == null) throw new NullPointerException();
      synchronized (list) {
        list.add(val);
        list.notify();
      }
    }

    private UpdateEvent take() throws InterruptedException {
      synchronized (list) {
        while (list.size() == 0) {
          list.wait();
        }
        return (UpdateEvent) list.remove(0);
      }
    }
  }
}
