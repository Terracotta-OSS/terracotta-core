/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util.concurrent;

/**
 * This class isn't very exciting, nor is it best (or only) way to implement a (safe) stoppable thread. It is useful to
 * subclass StoppableThread if need to support stop functionality and you don't want to assume that it's okay to just
 * interrupt() the thread at any given moment. In my opinion, if the code you're running in the thread isn't entirely in
 * your control, you probably don't want to randomly interrupt it. For threads that spend most of their time blocking on
 * something, simply use a timeout and periodically check stopRequested() to see if it is time to stop. <br>
 * <br>
 * README: You have to actually check stopRequested() someplace in your run() method for this thread to be "stoppable".
 */
public class StoppableThread extends Thread implements LifeCycleState {

  private volatile boolean stopRequested = false;

  public StoppableThread() {
    super();
  }

  public StoppableThread(Runnable target) {
    super(target);
  }

  public StoppableThread(String name) {
    super(name);
  }

  public StoppableThread(ThreadGroup group, Runnable target) {
    super(group, target);
  }

  public StoppableThread(Runnable target, String name) {
    super(target, name);
  }

  public StoppableThread(ThreadGroup group, String name) {
    super(group, name);
  }

  public StoppableThread(ThreadGroup group, Runnable target, String name) {
    super(group, target, name);
  }

  public StoppableThread(ThreadGroup group, Runnable target, String name, long stackSize) {
    super(group, target, name, stackSize);
  }

  public boolean isStopRequested() {
    return stopRequested;
  }

  public void requestStop() {
    this.stopRequested = true;
  }

  public boolean stopAndWait(long timeout) {
    requestStop();
    try {
      join(timeout);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    return !isAlive();
  }

}
