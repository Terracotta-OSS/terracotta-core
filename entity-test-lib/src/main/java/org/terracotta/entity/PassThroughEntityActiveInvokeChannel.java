package org.terracotta.entity;

import org.terracotta.exception.EntityException;
import org.terracotta.exception.EntityServerException;

public class PassThroughEntityActiveInvokeChannel<R extends EntityResponse> implements ActiveInvokeChannel<R> {
  
  private final InvokeMonitor<R> monitor;
  private volatile boolean closed = false;

  public PassThroughEntityActiveInvokeChannel(InvokeMonitor<R> monitor) {
    this.monitor = monitor;
  }

  @Override
  public synchronized void sendResponse(R r) {
    checkClosed();
    monitor.accept(r);
  }

  @Override
  public synchronized void sendException(Exception ee) {
    checkClosed();
    if (ee instanceof EntityException) {
      monitor.exception((EntityException)ee);
    } else {
      monitor.exception(new EntityServerException(null, null, null, ee));
    }
  }

  @Override
  public synchronized void close() {
    if (attemptClose()) {
      monitor.close();
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
