/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
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
 *      public void handleUpdate(Object data) {
 *        System.out.println(data);
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
      @Override
      public void run() {
        QueueEvent event = null;
        try {
          while (true) {
            event = queue.take();
            event.listener.handleUpdate(event.data);
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

  public synchronized void fireUpdateEvent(UpdateEvent data) {
    if (dispatchInterrupted) throw new IllegalStateException();
    if (eventListener != null && (data == null || eventListener != data.source)) {
      if (queue == null) eventListener.handleUpdate(data);
      else {
        QueueEvent event = new QueueEvent();
        event.listener = eventListener;
        event.data = data;
        queue.offer(event);
      }
    }
  }

  public synchronized void addListener(UpdateEventListener listener) {
    if (eventListener == null) {
      eventListener = listener;
    } else if (eventListener instanceof BroadcastListener) {
      ((BroadcastListener) eventListener).add(listener);
    } else {
      BroadcastListener broadcast = new BroadcastListener();
      broadcast.add(eventListener);
      broadcast.add(listener);
      eventListener = broadcast;
    }
  }

  public synchronized boolean removeListener(UpdateEventListener listener) {
    if (eventListener instanceof EventMulticaster.BroadcastListener) {
      BroadcastListener broadcast = (BroadcastListener) eventListener;
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

    private final List listeners = Collections.synchronizedList(new ArrayList());

    public void handleUpdate(UpdateEvent data) {
      for (Iterator iter = listeners.iterator(); iter.hasNext();) {
        UpdateEventListener listener = (UpdateEventListener) iter.next();
        if (listener == data.source) continue;
        if (queue == null) listener.handleUpdate(data);
        else {
          QueueEvent event = new QueueEvent();
          event.listener = listener;
          event.data = data;
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

  private static class QueueEvent implements Serializable {
    private transient UpdateEventListener listener;
    private transient UpdateEvent         data;
  }

  // --------------------------------------------------------------------------------

  private static class EventQueue implements Serializable {

    private final List list = new LinkedList();

    private void offer(QueueEvent val) {
      if (val == null) throw new NullPointerException();
      synchronized (list) {
        list.add(val);
        list.notify();
      }
    }

    private QueueEvent take() throws InterruptedException {
      synchronized (list) {
        while (list.size() == 0) {
          list.wait();
        }
        return (QueueEvent) list.remove(0);
      }
    }
  }
}
