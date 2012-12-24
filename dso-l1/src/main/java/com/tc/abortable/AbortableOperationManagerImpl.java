/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.abortable;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class AbortableOperationManagerImpl implements AbortableOperationManager {
  private static final TCLogger LOGGER = TCLogging.getLogger(AbortableOperationManagerImpl.class);

  enum OperationState {
    INIT, ABORTED;
  }

  private final ConcurrentMap<ThreadWrapper, OperationState> threadStates = new ConcurrentHashMap<ThreadWrapper, OperationState>();

  @Override
  public void begin() {
    if (threadStates.putIfAbsent(new ThreadWrapper(Thread.currentThread()), OperationState.INIT) != null) { throw new IllegalStateException(); }
  }

  @Override
  public void finish() {
    OperationState state = threadStates.remove(new ThreadWrapper(Thread.currentThread()));
    if (state == null) { throw new IllegalStateException(); }
    if (state == OperationState.ABORTED) {
      // TODO: Clearing the interrupted status
      // This is wrong ... But what to do ?
      // We could clear the actual interrupt to the App thread
      Thread.interrupted();
    }
  }

  @Override
  public void abort(Thread thread) {
    if (threadStates.replace(new ThreadWrapper(thread), OperationState.INIT, OperationState.ABORTED)) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Interrupting thread :" + thread);
      }
      thread.interrupt();
    } else {
      throw new IllegalStateException();
    }
  }

  @Override
  public boolean isAborted() {
    OperationState operationState = threadStates.get(new ThreadWrapper(Thread.currentThread()));
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("checking isAborted for thread :" + Thread.currentThread() + " State : " + operationState);
    }
    return operationState == null ? false : operationState == OperationState.ABORTED;
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
