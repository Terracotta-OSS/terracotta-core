/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.abortable;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class AbortableOperationManagerImpl implements AbortableOperationManager {
  enum OperationState {
    INIT, ABORTED;
  }

  private final ConcurrentMap<ThreadWrapper, OperationState> threadStates = new ConcurrentHashMap<ThreadWrapper, OperationState>();

  @Override
  public void begin() {
    if (threadStates.putIfAbsent(new ThreadWrapper(Thread.currentThread()), OperationState.INIT) != null) { throw new AssertionError(); }
  }

  @Override
  public void finish() {
    OperationState state = threadStates.remove(new ThreadWrapper(Thread.currentThread()));
    if (state != null && state == OperationState.ABORTED) {
      // TODO: Clearing the interrupted status
      // This is wrong ... But what to do ?
      // We could clear the actual interrupt to the App thread
      Thread.interrupted();
    }
  }

  @Override
  public void abort(Thread thread) {
    if (threadStates.replace(new ThreadWrapper(thread), OperationState.INIT, OperationState.ABORTED)) {
      thread.interrupt();
    }
  }

  @Override
  public boolean isAborted() {
    return threadStates.get(new ThreadWrapper(Thread.currentThread())) == OperationState.ABORTED;
  }

  private static class ThreadWrapper {
    private final Thread thread;

    public ThreadWrapper(Thread thread) {
      this.thread = thread;
    }

    @Override
    public int hashCode() {
      return System.identityHashCode(thread);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      ThreadWrapper other = (ThreadWrapper) obj;
      if (thread == null) {
        if (other.thread != null) return false;
      } else if (thread != other.thread) return false;
      return true;
    }

  }
}
