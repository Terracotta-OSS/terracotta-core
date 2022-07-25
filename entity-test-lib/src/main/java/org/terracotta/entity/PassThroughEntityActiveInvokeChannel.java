package org.terracotta.entity;

import org.terracotta.exception.EntityException;

public class PassThroughEntityActiveInvokeChannel<R extends EntityResponse> implements ActiveInvokeChannel<R> {
  
  private volatile boolean closed = false;

  public PassThroughEntityActiveInvokeChannel() {
  }

  @Override
  public synchronized void sendResponse(R r) {
    checkClosed();
    throw new UnsupportedOperationException();
  }

  @Override
  public synchronized void sendException(Exception ee) {
    checkClosed();
    if (ee instanceof EntityException) {
      throw new UnsupportedOperationException();
    } else {
      throw new UnsupportedOperationException();
    }
  }

  @Override
  public synchronized void close() {
    if (attemptClose()) {
      throw new UnsupportedOperationException();
    }
  }
  // serialized gating should be enough due to thread interaction at upper layers
  private synchronized boolean attemptClose() {
    try {
      return !closed;
    } finally {
      closed = true;
    }
  }
  // serialized gating should be enough due to thread interaction at upper layers
  private synchronized void checkClosed() {
    if (closed) {
      throw new IllegalStateException("channel closed");
    }
  }
}
